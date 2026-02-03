#!/bin/bash

###############################################################################
# MMORPG Server - Control Unificado
###############################################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MC_DIR="$SCRIPT_DIR/minecraft-server"
WEB_DIR="$SCRIPT_DIR/mmorpg-web"
MC_PID_FILE="$MC_DIR/server.pid"
WEB_PID_FILE="$WEB_DIR/web.pid"

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Funciones para el servidor MC
is_mc_running() {
    if [ -f "$MC_PID_FILE" ]; then
        PID=$(cat "$MC_PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

start_mc() {
    if is_mc_running; then
        print_warning "El servidor Minecraft ya está corriendo"
        return 1
    fi
    
    print_info "Iniciando servidor Minecraft..."
    cd "$MC_DIR"
    nohup ./start.sh > logs/console.log 2>&1 &
    echo $! > "$MC_PID_FILE"
    sleep 3
    
    if is_mc_running; then
        print_success "Servidor Minecraft iniciado (PID: $(cat $MC_PID_FILE))"
    else
        print_error "Error al iniciar el servidor Minecraft"
        return 1
    fi
}

stop_mc() {
    if ! is_mc_running; then
        print_warning "El servidor Minecraft no está corriendo"
        return 1
    fi
    
    print_info "Deteniendo servidor Minecraft..."
    PID=$(cat "$MC_PID_FILE")
    
    # Intentar detener gracefully
    kill -TERM "$PID" 2>/dev/null
    
    # Esperar hasta 30 segundos
    for i in {1..30}; do
        if ! ps -p "$PID" > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    
    # Forzar si aún está corriendo
    if ps -p "$PID" > /dev/null 2>&1; then
        print_warning "Forzando detención..."
        kill -9 "$PID" 2>/dev/null
    fi
    
    rm -f "$MC_PID_FILE"
    print_success "Servidor Minecraft detenido"
}

# Funciones para el panel web
is_web_running() {
    if [ -f "$WEB_PID_FILE" ]; then
        PID=$(cat "$WEB_PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

start_web() {
    if is_web_running; then
        print_warning "El panel web ya está corriendo"
        return 1
    fi
    
    print_info "Iniciando panel web..."
    cd "$WEB_DIR"
    nohup ./start-web.sh > web.log 2>&1 &
    echo $! > "$WEB_PID_FILE"
    sleep 2
    
    if is_web_running; then
        print_success "Panel web iniciado (PID: $(cat $WEB_PID_FILE))"
        print_info "Accede en: http://localhost:5000"
    else
        print_error "Error al iniciar el panel web"
        return 1
    fi
}

stop_web() {
    if ! is_web_running; then
        print_warning "El panel web no está corriendo"
        return 1
    fi
    
    print_info "Deteniendo panel web..."
    PID=$(cat "$WEB_PID_FILE")
    kill -TERM "$PID" 2>/dev/null
    sleep 2
    
    if ps -p "$PID" > /dev/null 2>&1; then
        kill -9 "$PID" 2>/dev/null
    fi
    
    rm -f "$WEB_PID_FILE"
    print_success "Panel web detenido"
}

# Funciones principales
start_all() {
    start_mc
    start_web
}

stop_all() {
    stop_mc
    stop_web
}

restart_all() {
    stop_all
    sleep 2
    start_all
}

show_status() {
    echo ""
    echo "Estado del sistema:"
    echo "==================="
    
    if is_mc_running; then
        echo -e "Servidor MC: ${GREEN}●${NC} Corriendo (PID: $(cat $MC_PID_FILE))"
    else
        echo -e "Servidor MC: ${RED}●${NC} Detenido"
    fi
    
    if is_web_running; then
        echo -e "Panel Web:   ${GREEN}●${NC} Corriendo (PID: $(cat $WEB_PID_FILE))"
        echo "             http://localhost:5000"
    else
        echo -e "Panel Web:   ${RED}●${NC} Detenido"
    fi
    echo ""
}

show_logs() {
    echo "Selecciona qué logs ver:"
    echo "1) Servidor Minecraft"
    echo "2) Panel Web"
    echo "3) Ambos"
    read -p "Opción [1]: " option
    option=${option:-1}
    
    case $option in
        1) tail -f "$MC_DIR/logs/latest.log" ;;
        2) tail -f "$WEB_DIR/web.log" ;;
        3) tail -f "$MC_DIR/logs/latest.log" "$WEB_DIR/web.log" ;;
        *) print_error "Opción inválida" ;;
    esac
}

show_usage() {
    cat << EOF
Uso: $0 {start|stop|restart|status|logs|mc-start|mc-stop|web-start|web-stop}

Comandos:
  start       - Iniciar servidor MC y panel web
  stop        - Detener servidor MC y panel web
  restart     - Reiniciar servidor MC y panel web
  status      - Mostrar estado de los servicios
  logs        - Ver logs en tiempo real
  
  mc-start    - Iniciar solo servidor MC
  mc-stop     - Detener solo servidor MC
  mc-restart  - Reiniciar solo servidor MC
  
  web-start   - Iniciar solo panel web
  web-stop    - Detener solo panel web
  web-restart - Reiniciar solo panel web

EOF
}

# Main
case "$1" in
    start)
        start_all
        show_status
        ;;
    stop)
        stop_all
        ;;
    restart)
        restart_all
        show_status
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    mc-start)
        start_mc
        ;;
    mc-stop)
        stop_mc
        ;;
    mc-restart)
        stop_mc
        sleep 2
        start_mc
        ;;
    web-start)
        start_web
        ;;
    web-stop)
        stop_web
        ;;
    web-restart)
        stop_web
        sleep 2
        start_web
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
