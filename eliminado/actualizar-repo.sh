#!/bin/bash

# Script de actualización del repositorio mc-paper-docker
# Fecha: 20 de diciembre de 2025

set -e  # Salir si hay algún error

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  Actualización mc-paper-docker${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# Función para verificar si un comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Función para verificar Maven
check_maven() {
    if ! command_exists mvn; then
        echo -e "${RED}❌ Maven no está instalado${NC}"
        echo -e "${YELLOW}Instálalo con:${NC}"
        echo "  Ubuntu/Debian: sudo apt install maven"
        echo "  CentOS/RHEL:   sudo yum install maven"
        exit 1
    fi
}

# Función para verificar Docker
check_docker() {
    if ! command_exists docker; then
        echo -e "${RED}❌ Docker no está instalado${NC}"
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        echo -e "${RED}❌ docker-compose no está instalado${NC}"
        exit 1
    fi
}

# Función para detener panel web
stop_web_panel() {
    echo -e "${YELLOW}→ Deteniendo panel web...${NC}"
    pkill -f "python3.*app.py" 2>/dev/null || true
    sleep 2
}

# Función para iniciar panel web
start_web_panel() {
    echo -e "${YELLOW}→ Iniciando panel web...${NC}"
    cd web
    nohup python3 app.py > /tmp/web-panel.log 2>&1 &
    cd ..
    sleep 3
    
    if curl -s http://localhost:5000 > /dev/null; then
        echo -e "${GREEN}✓ Panel web iniciado correctamente${NC}"
    else
        echo -e "${RED}⚠ Panel web puede no estar respondiendo${NC}"
    fi
}

# Función para actualizar código
update_code() {
    echo -e "${YELLOW}→ Actualizando código desde GitHub...${NC}"
    
    # Guardar cambios locales si los hay
    if [[ -n $(git status -s) ]]; then
        echo -e "${YELLOW}⚠ Hay cambios locales, guardándolos...${NC}"
        git stash
        STASHED=true
    else
        STASHED=false
    fi
    
    # Actualizar
    git pull origin master
    
    # Restaurar cambios locales
    if [ "$STASHED" = true ]; then
        echo -e "${YELLOW}→ Restaurando cambios locales...${NC}"
        git stash pop || echo -e "${YELLOW}⚠ No se pudieron restaurar cambios locales automáticamente${NC}"
    fi
    
    echo -e "${GREEN}✓ Código actualizado${NC}"
}

# Función para compilar plugin
compile_plugin() {
    echo -e "${YELLOW}→ Compilando plugin MMORPG...${NC}"
    cd mmorpg-plugin
    mvn clean package -q
    cp target/mmorpg-plugin-1.0.0.jar ../plugins/
    cd ..
    echo -e "${GREEN}✓ Plugin compilado${NC}"
}

# Función para copiar plugin al contenedor
copy_plugin_to_container() {
    echo -e "${YELLOW}→ Copiando plugin al contenedor...${NC}"
    docker cp plugins/mmorpg-plugin-1.0.0.jar minecraft-paper:/server/plugins/
    echo -e "${GREEN}✓ Plugin copiado${NC}"
}

# Función para reiniciar servidor
restart_server() {
    echo -e "${YELLOW}→ Reiniciando servidor Minecraft...${NC}"
    docker-compose restart minecraft-paper
    echo -e "${GREEN}✓ Servidor reiniciado${NC}"
}

# Función para verificar logs
check_logs() {
    echo ""
    echo -e "${BLUE}=========================================${NC}"
    echo -e "${BLUE}  Verificando logs del plugin...${NC}"
    echo -e "${BLUE}=========================================${NC}"
    sleep 5
    docker logs minecraft-paper 2>&1 | grep -i "MMORPG" | tail -15
}

# ============================================
# OPCIÓN 1: Actualización Completa
# ============================================
option_1_full_update() {
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OPCIÓN 1: Actualización Completa${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    
    # Verificar requisitos
    check_maven
    check_docker
    
    # Detener servicios
    echo -e "${YELLOW}→ Deteniendo servicios...${NC}"
    docker-compose down
    stop_web_panel
    echo -e "${GREEN}✓ Servicios detenidos${NC}"
    
    # Actualizar código
    update_code
    
    # Compilar plugin
    compile_plugin
    
    # Reiniciar servicios
    echo -e "${YELLOW}→ Iniciando servidor...${NC}"
    docker-compose up -d
    echo -e "${GREEN}✓ Servidor iniciado${NC}"
    
    # Iniciar panel web
    start_web_panel
    
    # Verificar
    echo ""
    echo -e "${GREEN}✓✓✓ Actualización completa finalizada ✓✓✓${NC}"
    check_logs
}

# ============================================
# OPCIÓN 2: Actualización Rápida
# ============================================
option_2_quick_update() {
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OPCIÓN 2: Actualización Rápida${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    
    # Verificar requisitos
    check_maven
    check_docker
    
    # Actualizar código
    update_code
    
    # Compilar plugin
    compile_plugin
    
    # Copiar al contenedor
    copy_plugin_to_container
    
    # Reiniciar servidor
    restart_server
    
    # Reiniciar panel web
    stop_web_panel
    start_web_panel
    
    # Verificar
    echo ""
    echo -e "${GREEN}✓✓✓ Actualización rápida finalizada ✓✓✓${NC}"
    check_logs
}

# ============================================
# OPCIÓN 3: Solo Actualizar Código
# ============================================
option_3_code_only() {
    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OPCIÓN 3: Solo Actualizar Código${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo ""
    
    # Verificar requisitos
    check_maven
    check_docker
    
    # Actualizar código
    update_code
    
    # Compilar plugin
    compile_plugin
    
    # Copiar al contenedor
    copy_plugin_to_container
    
    # Reiniciar solo el servidor
    restart_server
    
    # Verificar
    echo ""
    echo -e "${GREEN}✓✓✓ Actualización de código finalizada ✓✓✓${NC}"
    check_logs
}

# ============================================
# OPCIÓN 4: Verificar Estado
# ============================================
option_4_check_status() {
    echo ""
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Estado Actual del Sistema${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo ""
    
    # Estado de contenedores
    echo -e "${YELLOW}→ Estado de contenedores Docker:${NC}"
    docker-compose ps
    echo ""
    
    # Estado del panel web
    echo -e "${YELLOW}→ Estado del panel web:${NC}"
    if curl -s http://localhost:5000 > /dev/null; then
        echo -e "${GREEN}✓ Panel web activo en http://localhost:5000${NC}"
    else
        echo -e "${RED}✗ Panel web no responde${NC}"
    fi
    echo ""
    
    # Últimos logs del plugin
    echo -e "${YELLOW}→ Últimos logs del plugin MMORPG:${NC}"
    docker logs minecraft-paper 2>&1 | grep -i "MMORPG" | tail -10
    echo ""
    
    # Estado de Git
    echo -e "${YELLOW}→ Estado de Git:${NC}"
    git status -s
    echo ""
    
    # Versión del plugin
    echo -e "${YELLOW}→ Versión del plugin compilado:${NC}"
    if [ -f "mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar" ]; then
        ls -lh mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar
    else
        echo -e "${RED}Plugin no compilado${NC}"
    fi
}

# ============================================
# MENÚ PRINCIPAL
# ============================================
show_menu() {
    echo ""
    echo -e "${BLUE}Selecciona una opción:${NC}"
    echo ""
    echo -e "${GREEN}1)${NC} Actualización Completa"
    echo "   └─ Detiene todo → Actualiza → Recompila → Reinicia todo"
    echo "   └─ ${YELLOW}Tiempo estimado: 2-3 minutos${NC}"
    echo ""
    echo -e "${GREEN}2)${NC} Actualización Rápida (SIN detener servidor)"
    echo "   └─ Actualiza → Recompila → Reinicia solo servidor"
    echo "   └─ ${YELLOW}Tiempo estimado: 1-2 minutos${NC}"
    echo ""
    echo -e "${GREEN}3)${NC} Solo Actualizar Código (Más seguro)"
    echo "   └─ Actualiza código → Recompila → Copia plugin → Reinicia"
    echo "   └─ ${YELLOW}Tiempo estimado: 1 minuto${NC}"
    echo ""
    echo -e "${GREEN}4)${NC} Verificar Estado del Sistema"
    echo "   └─ Muestra estado sin hacer cambios"
    echo ""
    echo -e "${RED}5)${NC} Salir"
    echo ""
}

# ============================================
# MAIN
# ============================================
main() {
    # Verificar que estamos en el directorio correcto
    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${RED}❌ Error: Este script debe ejecutarse desde el directorio mc-paper-docker${NC}"
        exit 1
    fi
    
    # Si se pasa argumento, ejecutar sin menú
    if [ "$1" = "1" ]; then
        option_1_full_update
        exit 0
    elif [ "$1" = "2" ]; then
        option_2_quick_update
        exit 0
    elif [ "$1" = "3" ]; then
        option_3_code_only
        exit 0
    elif [ "$1" = "4" ]; then
        option_4_check_status
        exit 0
    fi
    
    # Menú interactivo
    while true; do
        show_menu
        read -p "Opción: " choice
        
        case $choice in
            1)
                option_1_full_update
                break
                ;;
            2)
                option_2_quick_update
                break
                ;;
            3)
                option_3_code_only
                break
                ;;
            4)
                option_4_check_status
                ;;
            5)
                echo ""
                echo -e "${BLUE}Saliendo...${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Opción inválida${NC}"
                ;;
        esac
    done
}

# Ejecutar main
main "$@"
