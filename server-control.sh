#!/bin/bash

###############################################################################
# MMORPG Server - Control Nativo
###############################################################################

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MC_DIR="$SCRIPT_DIR/minecraft-server"
WEB_DIR="$SCRIPT_DIR/mmorpg-web"
PID_FILE_MC="$MC_DIR/server.pid"
PID_FILE_WEB="$WEB_DIR/web.pid"
LOG_FILE_MC="$MC_DIR/logs/latest.log"
LOG_FILE_WEB="$WEB_DIR/web.log"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[✓]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[!]${NC} $1"; }
print_error() { echo -e "${RED}[✗]${NC} $1"; }

# Funciones de Minecraft Server
start_minecraft() {
    if [ -f "$PID_FILE_MC" ] && ps -p $(cat "$PID_FILE_MC") > /dev/null 2>&1; then
        print_warning "Servidor Minecraft ya está ejecutándose (PID: $(cat $PID_FILE_MC))"
        return 1
    fi
    
    print_info "Iniciando servidor Minecraft..."
    cd "$MC_DIR"
    
    # Aceptar EULA automáticamente
    echo "eula=true" > eula.txt
    
    # Iniciar servidor en background
    nohup java -Xms1G -Xmx1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled \
        -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions \
        -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \
        -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 \
        -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 \
        -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem \
        -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs \
        -Daikars.new.flags=true -jar paper.jar --nogui \
        > "$LOG_FILE_MC" 2>&1 &
    
    echo $! > "$PID_FILE_MC"
    print_success "Servidor Minecraft iniciado (PID: $(cat $PID_FILE_MC))"
    print_info "Logs: tail -f $LOG_FILE_MC"
}

stop_minecraft() {
    if [ ! -f "$PID_FILE_MC" ]; then
        print_warning "No se encontró PID del servidor Minecraft"
        return 1
    fi
    
    PID=$(cat "$PID_FILE_MC")
    if ! ps -p $PID > /dev/null 2>&1; then
        print_warning "Servidor Minecraft no está ejecutándose"
        rm -f "$PID_FILE_MC"
        return 1
    fi
    
    print_info "Deteniendo servidor Minecraft (PID: $PID)..."
    
    # Enviar comando stop via RCON si está disponible
    if command -v mcrcon &> /dev/null; then
        mcrcon -H localhost -P 25575 -p minecraft stop 2>/dev/null || true
        sleep 5
    fi
    
    # Si sigue corriendo, matar el proceso
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID
        sleep 2
    fi
    
    # Forzar si es necesario
    if ps -p $PID > /dev/null 2>&1; then
        kill -9 $PID
    fi
    
    rm -f "$PID_FILE_MC"
    print_success "Servidor Minecraft detenido"
}

restart_minecraft() {
    stop_minecraft
    sleep 2
    start_minecraft
}

status_minecraft() {
    if [ -f "$PID_FILE_MC" ] && ps -p $(cat "$PID_FILE_MC") > /dev/null 2>&1; then
        PID=$(cat "$PID_FILE_MC")
        print_success "Servidor Minecraft está ejecutándose (PID: $PID)"
        
        # Mostrar uso de recursos
        if command -v ps &> /dev/null; then
            CPU=$(ps -p $PID -o %cpu | tail -1)
            MEM=$(ps -p $PID -o %mem | tail -1)
            RSS=$(ps -p $PID -o rss | tail -1)
            RSS_MB=$((RSS / 1024))
            print_info "CPU: ${CPU}% | RAM: ${MEM}% (${RSS_MB} MB)"
        fi
        
        return 0
    else
        print_error "Servidor Minecraft no está ejecutándose"
        rm -f "$PID_FILE_MC"
        return 1
    fi
}

logs_minecraft() {
    if [ ! -f "$LOG_FILE_MC" ]; then
        print_error "No se encontraron logs"
        return 1
    fi
    
    tail -f "$LOG_FILE_MC"
}

# Funciones de Panel Web
start_web() {
    if [ -f "$PID_FILE_WEB" ] && ps -p $(cat "$PID_FILE_WEB") > /dev/null 2>&1; then
        print_warning "Panel web ya está ejecutándose (PID: $(cat $PID_FILE_WEB))"
        return 1
    fi
    
    print_info "Iniciando panel web..."
    cd "$WEB_DIR"
    
    # Activar entorno virtual y ejecutar
    source venv/bin/activate
    nohup python app.py > "$LOG_FILE_WEB" 2>&1 &
    
    echo $! > "$PID_FILE_WEB"
    print_success "Panel web iniciado (PID: $(cat $PID_FILE_WEB))"
    print_info "URL: http://localhost:5000"
    print_info "Logs: tail -f $LOG_FILE_WEB"
}

stop_web() {
    if [ ! -f "$PID_FILE_WEB" ]; then
        print_warning "No se encontró PID del panel web"
        return 1
    fi
    
    PID=$(cat "$PID_FILE_WEB")
    if ! ps -p $PID > /dev/null 2>&1; then
        print_warning "Panel web no está ejecutándose"
        rm -f "$PID_FILE_WEB"
        return 1
    fi
    
    print_info "Deteniendo panel web (PID: $PID)..."
    kill $PID 2>/dev/null || kill -9 $PID
    rm -f "$PID_FILE_WEB"
    print_success "Panel web detenido"
}

restart_web() {
    stop_web
    sleep 1
    start_web
}

status_web() {
    if [ -f "$PID_FILE_WEB" ] && ps -p $(cat "$PID_FILE_WEB") > /dev/null 2>&1; then
        PID=$(cat "$PID_FILE_WEB")
        print_success "Panel web está ejecutándose (PID: $PID)"
        print_info "URL: http://localhost:5000"
        return 0
    else
        print_error "Panel web no está ejecutándose"
        rm -f "$PID_FILE_WEB"
        return 1
    fi
}

logs_web() {
    if [ ! -f "$LOG_FILE_WEB" ]; then
        print_error "No se encontraron logs del panel web"
        return 1
    fi
    
    tail -f "$LOG_FILE_WEB"
}

# Función principal
case "$1" in
    start)
        case "$2" in
            server) start_minecraft ;;
            web) start_web ;;
            all)
                start_minecraft
                sleep 5
                start_web
                ;;
            *) 
                echo "Uso: $0 start {server|web|all}"
                exit 1
                ;;
        esac
        ;;
    stop)
        case "$2" in
            server) stop_minecraft ;;
            web) stop_web ;;
            all)
                stop_web
                stop_minecraft
                ;;
            *) 
                echo "Uso: $0 stop {server|web|all}"
                exit 1
                ;;
        esac
        ;;
    restart)
        case "$2" in
            server) restart_minecraft ;;
            web) restart_web ;;
            all)
                stop_web
                stop_minecraft
                sleep 2
                start_minecraft
                sleep 5
                start_web
                ;;
            *) 
                echo "Uso: $0 restart {server|web|all}"
                exit 1
                ;;
        esac
        ;;
    status)
        case "$2" in
            server) status_minecraft ;;
            web) status_web ;;
            all)
                status_minecraft
                echo ""
                status_web
                ;;
            *) 
                echo "Uso: $0 status {server|web|all}"
                exit 1
                ;;
        esac
        ;;
    logs)
        case "$2" in
            server) logs_minecraft ;;
            web) logs_web ;;
            *) 
                echo "Uso: $0 logs {server|web}"
                exit 1
                ;;
        esac
        ;;
    *)
        echo "Uso: $0 {start|stop|restart|status|logs} {server|web|all}"
        echo ""
        echo "Comandos:"
        echo "  start server   - Iniciar servidor Minecraft"
        echo "  start web      - Iniciar panel web"
        echo "  start all      - Iniciar todo"
        echo "  stop server    - Detener servidor Minecraft"
        echo "  stop web       - Detener panel web"
        echo "  stop all       - Detener todo"
        echo "  restart server - Reiniciar servidor Minecraft"
        echo "  restart web    - Reiniciar panel web"
        echo "  restart all    - Reiniciar todo"
        echo "  status server  - Estado del servidor Minecraft"
        echo "  status web     - Estado del panel web"
        echo "  status all     - Estado de todo"
        echo "  logs server    - Ver logs del servidor Minecraft"
        echo "  logs web       - Ver logs del panel web"
        exit 1
        ;;
esac
