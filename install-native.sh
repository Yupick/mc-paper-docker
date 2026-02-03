#!/bin/bash

###############################################################################
# MMORPG Server - Instalador Nativo (Sin Docker)
# Para Debian/Ubuntu Linux
###############################################################################

set -e  # Salir si hay errores

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="$SCRIPT_DIR"
MC_DIR="$INSTALL_DIR/minecraft-server"
WEB_DIR="$INSTALL_DIR/mmorpg-web"
JAVA_MIN_VERSION=17
PYTHON_MIN_VERSION=3.8
MC_VERSION="1.20.6"
MC_BUILD=""

# Función para imprimir mensajes
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

print_cyan() {
    echo -e "${CYAN}$1${NC}"
}

# Función para verificar si es root (no lo necesitamos)
check_not_root() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "No ejecutes este script como root. Usa tu usuario normal."
        read -p "¿Continuar de todos modos? (no recomendado) [y/N]: " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Verificar Java
check_java() {
    print_info "Verificando Java..."
    
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge "$JAVA_MIN_VERSION" ]; then
            print_success "Java $JAVA_VERSION encontrado"
            return 0
        else
            print_warning "Java $JAVA_VERSION encontrado pero se necesita Java $JAVA_MIN_VERSION+"
        fi
    else
        print_warning "Java no encontrado"
    fi
    
    # Instalar Java
    print_info "Instalando OpenJDK $JAVA_MIN_VERSION..."
    if [ -f /etc/debian_version ]; then
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jre-headless
    else
        print_error "Sistema no soportado para instalación automática de Java"
        print_info "Por favor instala Java $JAVA_MIN_VERSION+ manualmente"
        exit 1
    fi
    
    print_success "Java instalado correctamente"
}

# Verificar Python
check_python() {
    print_info "Verificando Python..."
    
    if command -v python3 &> /dev/null; then
        PYTHON_VERSION=$(python3 -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
        PYTHON_MAJOR=$(echo $PYTHON_VERSION | cut -d'.' -f1)
        PYTHON_MINOR=$(echo $PYTHON_VERSION | cut -d'.' -f2)
        
        if [ "$PYTHON_MAJOR" -ge 3 ] && [ "$PYTHON_MINOR" -ge 8 ]; then
            print_success "Python $PYTHON_VERSION encontrado"
            return 0
        else
            print_warning "Python $PYTHON_VERSION encontrado pero se necesita Python 3.8+"
        fi
    else
        print_warning "Python3 no encontrado"
    fi
    
    # Instalar Python
    print_info "Instalando Python3 y pip..."
    if [ -f /etc/debian_version ]; then
        sudo apt-get update
        sudo apt-get install -y python3 python3-pip python3-venv curl wget rsync
    else
        print_error "Sistema no soportado para instalación automática de Python"
        print_info "Por favor instala Python 3.8+ manualmente"
        exit 1
    fi
    
    print_success "Python instalado correctamente"
}

# Verificar Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_info "Maven no encontrado. Instalando..."
        if [ -f /etc/debian_version ]; then
            sudo apt-get install -y maven
        else
            print_error "Sistema no soportado para instalación automática de Maven"
            print_info "Por favor instala Maven manualmente"
            exit 1
        fi
    fi
    print_success "Maven disponible"
}

# Verificar/Compilar plugin
check_compile_plugin() {
    print_info "Verificando plugin MMORPG..."
    
    if [ ! -d "mmorpg-plugin" ]; then
        print_error "Directorio mmorpg-plugin no encontrado"
        exit 1
    fi
    
    # Verificar si ya está compilado
    if [ -f "mmorpg-plugin/target/mmorpg-plugin-1.0.0-shaded.jar" ]; then
        print_success "Plugin ya está compilado"
        read -p "¿Recompilar de todos modos? [y/N]: " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            return 0
        fi
    fi
    
    # Compilar plugin
    print_info "Compilando plugin MMORPG..."
    check_maven
    
    cd mmorpg-plugin
    mvn clean package -DskipTests
    
    if [ ! -f "target/mmorpg-plugin-1.0.0.jar" ]; then
        print_error "Error al compilar el plugin"
        exit 1
    fi
    
    cd "$INSTALL_DIR"
    print_success "Plugin compilado correctamente"
}

# Seleccionar versión de Paper
select_paper_version() {
    print_cyan ""
    print_cyan "╔════════════════════════════════════════════════════════╗"
    print_cyan "║  Selección de Versión de Minecraft Paper              ║"
    print_cyan "╚════════════════════════════════════════════════════════╝"
    echo ""
    
    print_info "Obteniendo versiones disponibles de Paper $MC_VERSION..."
    
    # Obtener todas las builds disponibles
    BUILD_INFO=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION")
    
    if [ -z "$BUILD_INFO" ]; then
        print_error "No se pudo obtener información de versiones"
        print_warning "Usando última versión disponible"
        MC_BUILD="latest"
        return
    fi
    
    # Extraer lista de builds
    BUILDS=($(echo "$BUILD_INFO" | grep -o '"builds":\[[0-9,]*\]' | grep -o '[0-9]*' | sort -rn))
    
    if [ ${#BUILDS[@]} -eq 0 ]; then
        print_error "No se encontraron builds disponibles"
        exit 1
    fi
    
    # Mostrar hasta 6 opciones (última + 5 anteriores)
    echo "Versiones disponibles:"
    echo ""
    
    local max_options=6
    local count=0
    local options=()
    
    for build in "${BUILDS[@]}"; do
        if [ $count -lt $max_options ]; then
            count=$((count + 1))
            options+=("$build")
            
            if [ $count -eq 1 ]; then
                echo -e "  ${GREEN}$count)${NC} Paper $MC_VERSION build $build ${CYAN}(Más reciente)${NC}"
            else
                echo "  $count) Paper $MC_VERSION build $build"
            fi
        fi
    done
    
    echo ""
    read -p "Selecciona una opción [1]: " selection
    selection=${selection:-1}
    
    # Validar selección
    if ! [[ "$selection" =~ ^[0-9]+$ ]] || [ "$selection" -lt 1 ] || [ "$selection" -gt ${#options[@]} ]; then
        print_warning "Selección inválida. Usando la más reciente."
        selection=1
    fi
    
    MC_BUILD=${options[$((selection - 1))]}
    
    print_success "Seleccionada: Paper $MC_VERSION build $MC_BUILD"
    echo ""
}

# Descargar Paper
download_paper() {
    select_paper_version
    
    print_info "Descargando Paper $MC_VERSION build $MC_BUILD..."
    
    mkdir -p "$MC_DIR"
    cd "$MC_DIR"
    
    DOWNLOAD_URL="https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION/builds/$MC_BUILD/downloads/paper-$MC_VERSION-$MC_BUILD.jar"
    
    # Mostrar progreso con wget
    if ! wget --show-progress -q "$DOWNLOAD_URL" -O paper.jar; then
        print_error "Error al descargar Paper"
        print_info "URL: $DOWNLOAD_URL"
        exit 1
    fi
    
    if [ ! -f "paper.jar" ]; then
        print_error "Error: paper.jar no se descargó correctamente"
        exit 1
    fi
    
    print_success "Paper descargado correctamente ($(du -h paper.jar | cut -f1))"
    cd "$INSTALL_DIR"
}

# Configurar servidor
setup_server() {
    print_info "Configurando servidor Minecraft..."
    
    cd "$MC_DIR"
    
    # Crear estructura de directorios
    mkdir -p plugins config worlds logs backups
    
    # Copiar plugin
    if [ -f "$INSTALL_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar" ]; then
        cp "$INSTALL_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar" plugins/MMORPGPlugin.jar
        print_success "Plugin copiado a plugins/"
    else
        print_error "Plugin no encontrado. Asegúrate de compilarlo primero."
        exit 1
    fi
    
    # Crear eula.txt
    echo "eula=true" > eula.txt
    
    # Crear server.properties
    cat > server.properties << 'SERVER_PROPS'
# Configuración del servidor
server-port=25565
enable-rcon=true
rcon.port=25575
rcon.password=minecraft
max-players=20
view-distance=10
simulation-distance=10
motd=Servidor MMORPG
difficulty=normal
gamemode=survival
pvp=true
online-mode=true
spawn-protection=0
enable-command-block=true
SERVER_PROPS
    
    # Crear script de inicio con 1GB RAM
    cat > start.sh << 'START_SCRIPT'
#!/bin/bash
cd "$(dirname "$0")"
java -Xms1G -Xmx1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 \
     -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch \
     -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M \
     -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 \
     -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
     -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem \
     -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs \
     -Daikars.new.flags=true \
     -jar paper.jar --nogui
START_SCRIPT
    
    chmod +x start.sh
    
    print_success "Servidor configurado en: $MC_DIR"
    print_info "RAM asignada: 1GB (edita start.sh para cambiar)"
    cd "$INSTALL_DIR"
}

# Configurar panel web
setup_web() {
    print_info "Configurando panel web..."
    
    mkdir -p "$WEB_DIR"
    
    # Copiar archivos del panel web
    if [ -d "$INSTALL_DIR/web" ]; then
        cp -r "$INSTALL_DIR/web"/* "$WEB_DIR/"
        print_success "Archivos del panel copiados"
    else
        print_error "Directorio web/ no encontrado"
        exit 1
    fi
    
    cd "$WEB_DIR"
    
    # Crear virtualenv
    print_info "Creando entorno virtual de Python..."
    python3 -m venv venv
    source venv/bin/activate
    
    # Instalar dependencias
    print_info "Instalando dependencias de Python..."
    pip install --upgrade pip
    pip install Flask python-dotenv requests Werkzeug bcrypt flask-login
    
    # Crear archivo .env si no existe
    if [ ! -f .env ]; then
        SECRET_KEY=$(openssl rand -hex 32 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
        cat > .env << ENV_FILE
SECRET_KEY=$SECRET_KEY
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123456
SERVER_HOST=localhost
SERVER_PORT=25575
RCON_PASSWORD=minecraft
FLASK_ENV=production
ENV_FILE
        print_success "Archivo .env creado"
    fi
    
    # Crear script de inicio
    cat > start-web.sh << 'WEB_START'
#!/bin/bash
cd "$(dirname "$0")"
source venv/bin/activate
export FLASK_APP=app.py
python3 app.py
WEB_START
    
    chmod +x start-web.sh
    
    deactivate
    print_success "Panel web configurado en: $WEB_DIR"
    cd "$INSTALL_DIR"
}

# Crear servicios systemd
create_systemd_services() {
    print_info "Creando servicios systemd..."
    
    # Servicio del servidor Minecraft
    sudo tee /etc/systemd/system/mmorpg-server.service > /dev/null << SERVICE1
[Unit]
Description=MMORPG Minecraft Server
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$MC_DIR
ExecStart=$MC_DIR/start.sh
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICE1
    
    # Servicio del panel web
    sudo tee /etc/systemd/system/mmorpg-web.service > /dev/null << SERVICE2
[Unit]
Description=MMORPG Web Panel
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$WEB_DIR
ExecStart=$WEB_DIR/start-web.sh
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
SERVICE2
    
    sudo systemctl daemon-reload
    
    print_success "Servicios systemd creados:"
    print_info "  - mmorpg-server.service"
    print_info "  - mmorpg-web.service"
    print_info ""
    print_info "Comandos útiles:"
    print_info "  sudo systemctl start mmorpg-server"
    print_info "  sudo systemctl start mmorpg-web"
    print_info "  sudo systemctl enable mmorpg-server  # Auto-inicio"
    print_info "  sudo systemctl status mmorpg-server"
}

# Mostrar resumen
show_summary() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║         INSTALACIÓN COMPLETADA EXITOSAMENTE              ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    print_info "Versión instalada:"
    echo "  📦 Paper $MC_VERSION build $MC_BUILD"
    echo "  🔧 Plugin MMORPG v1.0.0"
    echo ""
    print_info "Ubicaciones:"
    echo "  📁 Servidor Minecraft: $MC_DIR"
    echo "  🌐 Panel Web:          $WEB_DIR"
    echo ""
    print_info "Configuración:"
    echo "  💾 RAM Servidor: 1GB (edita minecraft-server/start.sh para cambiar)"
    echo "  🔌 Puerto MC:    25565"
    echo "  🌐 Puerto Web:   5000"
    echo "  🎮 Puerto RCON:  25575"
    echo ""
    print_info "Para iniciar:"
    echo "  Servidor:  ./start-server.sh"
    echo "  Panel Web: ./start-web.sh"
    echo "  Todo:      ./server-control.sh start"
    echo "  Ver logs:  ./logs.sh"
    echo ""
    print_info "Panel Web:"
    echo "  URL:       http://localhost:5000"
    echo "  Usuario:   admin"
    echo "  Password:  admin123456"
    echo ""
    print_info "Scripts disponibles:"
    echo "  ./start-server.sh         - Iniciar servidor"
    echo "  ./start-web.sh            - Iniciar panel web"
    echo "  ./logs.sh                 - Ver logs en tiempo real"
    echo "  ./server-control.sh       - Control unificado"
    echo "  ./update-from-github.sh   - Actualizar desde GitHub"
    echo "  ./uninstall-native.sh     - Desinstalar todo"
    echo ""
}

###############################################################################
# MAIN
###############################################################################

clear
echo -e "${BLUE}"
cat << "BANNER"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║    MMORPG Server - Instalador Nativo                    ║
║    Minecraft Paper + Panel Web                           ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
BANNER
echo -e "${NC}"

print_info "Directorio de instalación: $INSTALL_DIR"
print_info "Usuario: $USER"
echo ""

# Verificaciones
check_not_root
check_java
check_python

echo ""
read -p "¿Continuar con la instalación? [Y/n]: " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]] && [[ ! -z $REPLY ]]; then
    print_warning "Instalación cancelada"
    exit 0
fi

# Instalación
check_compile_plugin
download_paper
setup_server
setup_web

# Preguntar por systemd
if [ -f /etc/debian_version ] || [ -f /etc/redhat-release ]; then
    echo ""
    read -p "¿Crear servicios systemd para auto-inicio? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        create_systemd_services
    fi
fi

# Crear scripts de control
cd "$INSTALL_DIR"

# Script start-server.sh
cat > start-server.sh << 'SRVSTART'
#!/bin/bash
cd "$(dirname "$0")/minecraft-server"
./start.sh
SRVSTART
chmod +x start-server.sh

# Script start-web.sh
cat > start-web.sh << 'WEBSTART'
#!/bin/bash
cd "$(dirname "$0")/mmorpg-web"
./start-web.sh
WEBSTART
chmod +x start-web.sh

# Script logs.sh
cat > logs.sh << 'LOGSCRIPT'
#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Selecciona qué logs ver:"
echo "1) Servidor Minecraft"
echo "2) Panel Web"
echo "3) Ambos (dividido)"
read -p "Opción [1]: " option
option=${option:-1}

case $option in
    1)
        tail -f "$SCRIPT_DIR/minecraft-server/logs/latest.log"
        ;;
    2)
        if [ -f "$SCRIPT_DIR/mmorpg-web/web.log" ]; then
            tail -f "$SCRIPT_DIR/mmorpg-web/web.log"
        else
            echo "Log del panel web no encontrado. ¿Está corriendo?"
        fi
        ;;
    3)
        if command -v tmux &> /dev/null; then
            tmux new-session \; \
              send-keys "tail -f $SCRIPT_DIR/minecraft-server/logs/latest.log" C-m \; \
              split-window -h \; \
              send-keys "tail -f $SCRIPT_DIR/mmorpg-web/web.log 2>/dev/null || echo 'Panel web no corriendo'" C-m
        else
            echo "tmux no está instalado. Mostrando solo logs del servidor..."
            tail -f "$SCRIPT_DIR/minecraft-server/logs/latest.log"
        fi
        ;;
    *)
        echo "Opción inválida"
        exit 1
        ;;
esac
LOGSCRIPT
chmod +x logs.sh

print_success "Scripts de control creados"

show_summary
