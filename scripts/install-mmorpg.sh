#!/bin/bash

# Script de instalación automática del Sistema MMORPG
# Este script prepara el entorno completo para el plugin MMORPG
# - Verifica e instala el plugin JAR
# - Prepara archivos de configuración
# - Inicia el servidor y el panel web

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "🚀 Instalador MMORPG - Sistema Completo"
echo "========================================="
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

success() { echo -e "${GREEN}✅${NC} $1"; }
error() { echo -e "${RED}❌${NC} $1"; }
warn() { echo -e "${YELLOW}⚠️ ${NC} $1"; }
info() { echo -e "${BLUE}ℹ️ ${NC} $1"; }

# Función para compilar plugin
compile_plugin() {
    echo ""
    echo "========================================="
    echo "📦 Compilando Plugin MMORPG"
    echo "========================================="
    echo ""
    
    if [ ! -f "$SCRIPT_DIR/mmorpg-plugin/pom.xml" ]; then
        error "No se encontró pom.xml del plugin"
        return 1
    fi
    
    cd "$SCRIPT_DIR/mmorpg-plugin"
    
    if mvn clean package -q 2>/dev/null; then
        success "Plugin compilado exitosamente"
        
        # Copiar JAR si el contenedor está corriendo
        JAR_FILE="$SCRIPT_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar"
        if [ -f "$JAR_FILE" ]; then
            if docker ps --format '{{.Names}}' | grep -q "^minecraft-paper$"; then
                if docker cp "$JAR_FILE" minecraft-paper:/server/plugins/mmorpg-plugin-1.0.0.jar 2>/dev/null; then
                    success "JAR sincronizado con el contenedor"
                else
                    warn "No se pudo sincronizar JAR con el contenedor"
                fi
            else
                info "Contenedor no está corriendo (se sincronizará al iniciar)"
            fi
        fi
        return 0
    else
        error "Error compilando el plugin"
        mvn clean package 2>&1 | tail -10
        return 1
    fi
}

# Función para iniciar el servidor
start_server() {
    echo ""
    echo "========================================="
    echo "🎮 Iniciando Servidor Minecraft"
    echo "========================================="
    echo ""
    
    if ! docker ps --format '{{.Names}}' | grep -q "^minecraft-paper$"; then
        info "Iniciando contenedor Docker..."
        cd "$SCRIPT_DIR"
        if docker-compose up -d 2>&1 | grep -q "done\|Creating"; then
            success "Servidor iniciado"
            echo ""
            info "Esperando a que el servidor cargue completamente..."
            sleep 10
            return 0
        else
            error "Error al iniciar el servidor"
            return 1
        fi
    else
        success "Servidor ya está corriendo"
        return 0
    fi
}

# Función para iniciar el panel web
start_web_panel() {
    echo ""
    echo "========================================="
    echo "🌐 Iniciando Panel Web"
    echo "========================================="
    echo ""
    
    cd "$SCRIPT_DIR"
    
    # Ejecutar start-web-panel.sh en background
    if ./start-web-panel.sh > /dev/null 2>&1 &
    then
        success "Panel web iniciado en background"
        echo ""
        info "Panel disponible en: http://localhost:5000"
        return 0
    else
        error "Error al iniciar el panel web"
        return 1
    fi
}

# Función para verificar estado
check_status() {
    echo ""
    echo "========================================="
    echo "📊 Estado del Sistema"
    echo "========================================="
    echo ""
    
    # Verificar servidor
    if docker ps --format '{{.Names}}' | grep -q "^minecraft-paper$"; then
        success "Servidor Minecraft corriendo"
        
        # Verificar plugin
        if docker logs minecraft-paper 2>/dev/null | grep -q "MMORPGPlugin habilitado"; then
            success "Plugin MMORPG cargado"
        else
            warn "Esperando carga del plugin..."
        fi
    else
        error "Servidor no está corriendo"
    fi
    
    # Verificar panel web
    if ps aux | grep -v grep | grep -q "python3.*app.py"; then
        success "Panel web ejecutándose"
    else
        warn "Panel web no está corriendo"
    fi
    
    echo ""
}

# Menú principal
show_menu() {
    echo ""
    echo "========================================="
    echo "📋 Opciones"
    echo "========================================="
    echo ""
    echo "1) Compilar plugin"
    echo "2) Iniciar servidor"
    echo "3) Iniciar panel web"
    echo "4) Compilar + Iniciar servidor + Panel"
    echo "5) Ver estado del sistema"
    echo "6) Ver logs del servidor"
    echo "7) Detener todo"
    echo "0) Salir"
    echo ""
}

# Detener todo
stop_all() {
    echo ""
    info "Deteniendo servicios..."
    
    if pgrep -f "python3.*app.py" > /dev/null; then
        pkill -f "python3.*app.py"
        success "Panel web detenido"
    fi
    
    if docker ps --format '{{.Names}}' | grep -q "^minecraft-paper$"; then
        docker-compose down 2>/dev/null
        success "Servidor detenido"
    fi
    
    echo ""
}

# Ver logs
show_logs() {
    echo ""
    info "Mostrando logs del servidor (Ctrl+C para salir)..."
    echo ""
    docker logs -f minecraft-paper 2>/dev/null || error "Servidor no está corriendo"
}

# Menú interactivo
if [ "$1" == "--auto" ] || [ "$1" == "-a" ]; then
    # Modo automático
    compile_plugin && start_server && start_web_panel && check_status
else
    # Menú interactivo
    while true; do
        show_menu
        read -p "Selecciona opción: " -n 1 -r
        echo ""
        
        case $REPLY in
            1) compile_plugin ;;
            2) start_server ;;
            3) start_web_panel ;;
            4) compile_plugin && start_server && start_web_panel && check_status ;;
            5) check_status ;;
            6) show_logs ;;
            7) stop_all ;;
            0) echo "Saliendo..." && exit 0 ;;
            *) error "Opción no válida" ;;
        esac
    done
fi

echo ""
success "Sistema MMORPG listo para usar"
echo ""
echo "📝 Próximos pasos:"
echo "   1. Abre el navegador: http://localhost:5000"
echo "   2. Login con: admin / minecraft123"
echo "   3. Verifica los logs del servidor"
echo ""
