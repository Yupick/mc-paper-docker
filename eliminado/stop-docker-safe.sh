#!/bin/bash

#############################################
# Script: stop-docker-safe.sh
# Descripción: Detiene contenedores Docker de forma segura
#              manejando errores de permisos y endpoints activos
# Autor: Sistema MC-Paper
# Fecha: 1 de diciembre de 2025
#############################################

set -e  # Detener en errores (excepto donde se maneje explícitamente)

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Función para verificar si se ejecuta como root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_warning "Este script no se está ejecutando como root"
        print_info "Si encuentras errores de permisos, ejecuta: sudo $0"
        echo
        read -p "¿Continuar de todos modos? (s/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Ss]$ ]]; then
            exit 1
        fi
    fi
}

# Función para verificar si Docker está corriendo
check_docker() {
    if ! docker info &>/dev/null; then
        print_error "Docker no está corriendo o no tienes permisos"
        print_info "Intenta ejecutar: sudo systemctl start docker"
        print_info "O ejecuta este script con sudo: sudo $0"
        exit 1
    fi
}

# Función para listar contenedores corriendo
list_containers() {
    print_info "Obteniendo lista de contenedores corriendo..."
    echo
    
    # Obtener contenedores corriendo
    containers=$(docker ps --format "{{.ID}}|{{.Names}}|{{.Image}}|{{.Status}}" 2>/dev/null)
    
    if [ -z "$containers" ]; then
        print_warning "No hay contenedores corriendo"
        exit 0
    fi
    
    # Imprimir encabezado
    printf "${BLUE}%-5s %-20s %-30s %-30s${NC}\n" "NUM" "NOMBRE" "IMAGEN" "ESTADO"
    printf "%.0s-" {1..90}
    echo
    
    # Imprimir contenedores con numeración
    i=1
    while IFS='|' read -r id name image status; do
        printf "%-5s %-20s %-30s %-30s\n" "$i" "$name" "$image" "$status"
        container_ids[$i]=$id
        container_names[$i]=$name
        ((i++))
    done <<< "$containers"
    
    echo
    TOTAL_CONTAINERS=$((i-1))
}

# Función para forzar detención de contenedor
force_stop_container() {
    local container_id=$1
    local container_name=$2
    
    print_info "Intentando detener contenedor: $container_name ($container_id)"
    
    # Intento 1: Detención normal
    print_info "Intento 1: docker stop (timeout 30s)..."
    if docker stop -t 30 "$container_id" &>/dev/null; then
        print_success "Contenedor detenido exitosamente"
        return 0
    fi
    print_warning "Detención normal falló"
    
    # Intento 2: Kill directo
    print_info "Intento 2: docker kill..."
    if docker kill "$container_id" &>/dev/null; then
        print_success "Contenedor terminado con kill"
        return 0
    fi
    print_warning "Kill falló"
    
    # Intento 3: Con sudo
    print_info "Intento 3: Con privilegios elevados..."
    if sudo docker stop -t 10 "$container_id" &>/dev/null; then
        print_success "Contenedor detenido con sudo"
        return 0
    fi
    print_warning "Stop con sudo falló"
    
    # Intento 4: Kill con sudo
    print_info "Intento 4: Kill con privilegios elevados..."
    if sudo docker kill "$container_id" &>/dev/null; then
        print_success "Contenedor terminado con sudo kill"
        return 0
    fi
    print_warning "Sudo kill falló"
    
    # Intento 5: Reiniciar Docker daemon
    print_warning "Último recurso: ¿Reiniciar Docker daemon?"
    read -p "Esto afectará TODOS los contenedores. ¿Continuar? (s/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        print_info "Reiniciando Docker daemon..."
        if sudo systemctl restart docker; then
            print_success "Docker reiniciado"
            sleep 3
            # Verificar si el contenedor sigue corriendo
            if ! docker ps -q --filter "id=$container_id" | grep -q .; then
                print_success "Contenedor ya no está corriendo"
                return 0
            fi
        fi
    fi
    
    print_error "No se pudo detener el contenedor por ningún método"
    return 1
}

# Función para limpiar redes huérfanas
cleanup_networks() {
    print_info "Verificando redes huérfanas..."
    
    # Obtener redes con endpoints activos pero sin contenedores corriendo
    orphan_networks=$(docker network ls -q 2>/dev/null | while read net; do
        if docker network inspect "$net" 2>/dev/null | grep -q '"Containers": {}'; then
            echo "$net"
        fi
    done)
    
    if [ -n "$orphan_networks" ]; then
        print_warning "Se encontraron redes huérfanas"
        echo "$orphan_networks" | while read net; do
            net_name=$(docker network inspect "$net" --format '{{.Name}}' 2>/dev/null)
            print_info "Limpiando red: $net_name"
            
            # Intentar remover la red
            if docker network rm "$net" &>/dev/null; then
                print_success "Red $net_name removida"
            elif sudo docker network rm "$net" &>/dev/null; then
                print_success "Red $net_name removida con sudo"
            else
                print_warning "No se pudo remover la red $net_name (puede estar en uso)"
            fi
        done
    else
        print_success "No hay redes huérfanas"
    fi
}

# Función para desconectar endpoints forzadamente
force_disconnect_endpoints() {
    local container_id=$1
    local container_name=$2
    
    print_info "Buscando redes conectadas al contenedor..."
    
    # Obtener redes del contenedor
    networks=$(docker inspect "$container_id" --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}} {{end}}' 2>/dev/null)
    
    if [ -n "$networks" ]; then
        for network in $networks; do
            print_info "Desconectando de red: $network"
            if docker network disconnect -f "$network" "$container_id" &>/dev/null; then
                print_success "Desconectado de $network"
            elif sudo docker network disconnect -f "$network" "$container_id" &>/dev/null; then
                print_success "Desconectado de $network con sudo"
            else
                print_warning "No se pudo desconectar de $network"
            fi
        done
    fi
}

# Función para remover contenedor si está detenido
remove_container_if_stopped() {
    local container_id=$1
    local container_name=$2
    
    # Verificar si está detenido
    status=$(docker inspect "$container_id" --format '{{.State.Running}}' 2>/dev/null)
    
    if [ "$status" = "false" ]; then
        print_info "El contenedor está detenido"
        read -p "¿Deseas remover el contenedor también? (s/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            print_info "Removiendo contenedor..."
            if docker rm -f "$container_id" &>/dev/null; then
                print_success "Contenedor removido"
            elif sudo docker rm -f "$container_id" &>/dev/null; then
                print_success "Contenedor removido con sudo"
            else
                print_error "No se pudo remover el contenedor"
            fi
        fi
    fi
}

# Función principal
main() {
    clear
    echo "╔════════════════════════════════════════════════════╗"
    echo "║   Script de Detención Segura de Docker            ║"
    echo "║   Manejo robusto de errores y permisos            ║"
    echo "╚════════════════════════════════════════════════════╝"
    echo
    
    # Verificaciones iniciales
    check_docker
    check_root
    
    # Listar contenedores
    declare -A container_ids
    declare -A container_names
    list_containers
    
    if [ "${TOTAL_CONTAINERS:-0}" -eq 0 ]; then
        exit 0
    fi
    
    # Selección de contenedor
    while true; do
        read -p "Selecciona el número del contenedor a detener (1-$TOTAL_CONTAINERS, 0 para salir): " selection
        
        if [ "$selection" = "0" ]; then
            print_info "Operación cancelada"
            exit 0
        fi
        
        if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le "$TOTAL_CONTAINERS" ]; then
            break
        fi
        
        print_error "Selección inválida. Ingresa un número entre 1 y $total"
    done
    
    selected_id=${container_ids[$selection]}
    selected_name=${container_names[$selection]}
    
    echo
    print_info "Has seleccionado: $selected_name"
    print_warning "ID: $selected_id"
    echo
    
    read -p "¿Confirmas la detención de este contenedor? (s/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Ss]$ ]]; then
        print_info "Operación cancelada"
        exit 0
    fi
    
    echo
    print_info "═══ Iniciando proceso de detención ═══"
    echo
    
    # Paso 1: Desconectar endpoints
    force_disconnect_endpoints "$selected_id" "$selected_name"
    echo
    
    # Paso 2: Detener contenedor
    if force_stop_container "$selected_id" "$selected_name"; then
        echo
        print_success "═══ Contenedor detenido exitosamente ═══"
        echo
        
        # Paso 3: Limpiar redes
        cleanup_networks
        echo
        
        # Paso 4: Preguntar si remover
        remove_container_if_stopped "$selected_id" "$selected_name"
        echo
        
        print_success "═══ Proceso completado ═══"
    else
        echo
        print_error "═══ Proceso falló ═══"
        print_info "Recomendaciones:"
        echo "  1. Ejecuta el script con sudo: sudo $0"
        echo "  2. Verifica logs: docker logs $selected_id"
        echo "  3. Revisa procesos: sudo lsof -i -P | grep docker"
        echo "  4. Último recurso: sudo systemctl restart docker"
        exit 1
    fi
}

# Ejecutar script
main "$@"
