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

# Descargar plugins de compatibilidad
download_compatibility_plugins() {
    print_info "Descargando plugins de compatibilidad..."
    
    cd "$MC_DIR/plugins"
    
    # Descargar GeyserMC (permite jugadores de Bedrock)
    print_info "  → Descargando GeyserMC..."
    if curl -L -o Geyser-Spigot.jar "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" 2>/dev/null; then
        if [ -f Geyser-Spigot.jar ]; then
            GEYSER_SIZE=$(du -h Geyser-Spigot.jar | cut -f1)
            # Verificar que no sea un archivo corrupto (< 100KB)
            GEYSER_BYTES=$(stat -f%z Geyser-Spigot.jar 2>/dev/null || stat -c%s Geyser-Spigot.jar 2>/dev/null)
            if [ "$GEYSER_BYTES" -gt 100000 ]; then
                print_success "    ✅ GeyserMC descargado ($GEYSER_SIZE)"
            else
                print_warning "    ⚠️  Archivo GeyserMC corrupto, eliminando..."
                rm -f Geyser-Spigot.jar
            fi
        fi
    else
        print_warning "    ⚠️  Error descargando GeyserMC"
    fi
    
    # Descargar Floodgate (autenticación para Bedrock)
    print_info "  → Descargando Floodgate..."
    if curl -L -o floodgate-spigot.jar "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" 2>/dev/null; then
        if [ -f floodgate-spigot.jar ]; then
            FLOODGATE_SIZE=$(du -h floodgate-spigot.jar | cut -f1)
            FLOODGATE_BYTES=$(stat -f%z floodgate-spigot.jar 2>/dev/null || stat -c%s floodgate-spigot.jar 2>/dev/null)
            if [ "$FLOODGATE_BYTES" -gt 100000 ]; then
                print_success "    ✅ Floodgate descargado ($FLOODGATE_SIZE)"
            else
                print_warning "    ⚠️  Archivo Floodgate corrupto, eliminando..."
                rm -f floodgate-spigot.jar
            fi
        fi
    else
        print_warning "    ⚠️  Error descargando Floodgate"
    fi
    
    # Descargar ViaVersion (permite diferentes versiones de clientes)
    print_info "  → Descargando ViaVersion..."
    VIAVERSION_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaVersion/releases/latest | grep "browser_download_url.*ViaVersion-.*\.jar" | cut -d '"' -f 4)
    if [ -n "$VIAVERSION_URL" ]; then
        if curl -L -o ViaVersion.jar "$VIAVERSION_URL" 2>/dev/null; then
            if [ -f ViaVersion.jar ]; then
                VIAVERSION_SIZE=$(du -h ViaVersion.jar | cut -f1)
                print_success "    ✅ ViaVersion descargado ($VIAVERSION_SIZE)"
            fi
        else
            print_warning "    ⚠️  Error descargando ViaVersion"
        fi
    else
        print_warning "    ⚠️  No se pudo obtener URL de ViaVersion"
    fi
    
    # Descargar ViaBackwards (soporte para versiones antiguas)
    print_info "  → Descargando ViaBackwards..."
    VIABACKWARDS_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaBackwards/releases/latest | grep "browser_download_url.*ViaBackwards-.*\.jar" | cut -d '"' -f 4)
    if [ -n "$VIABACKWARDS_URL" ]; then
        if curl -L -o ViaBackwards.jar "$VIABACKWARDS_URL" 2>/dev/null; then
            if [ -f ViaBackwards.jar ]; then
                VIABACKWARDS_SIZE=$(du -h ViaBackwards.jar | cut -f1)
                print_success "    ✅ ViaBackwards descargado ($VIABACKWARDS_SIZE)"
            fi
        else
            print_warning "    ⚠️  Error descargando ViaBackwards"
        fi
    else
        print_warning "    ⚠️  No se pudo obtener URL de ViaBackwards"
    fi
    
    # Descargar ViaRewind (soporte para versiones muy antiguas 1.7-1.8)
    print_info "  → Descargando ViaRewind..."
    VIAREWIND_URL=$(curl -s https://api.github.com/repos/ViaVersion/ViaRewind/releases/latest | grep "browser_download_url.*ViaRewind-.*\.jar" | cut -d '"' -f 4)
    if [ -n "$VIAREWIND_URL" ]; then
        if curl -L -o ViaRewind.jar "$VIAREWIND_URL" 2>/dev/null; then
            if [ -f ViaRewind.jar ]; then
                VIAREWIND_SIZE=$(du -h ViaRewind.jar | cut -f1)
                print_success "    ✅ ViaRewind descargado ($VIAREWIND_SIZE)"
            fi
        else
            print_warning "    ⚠️  Error descargando ViaRewind"
        fi
    else
        print_warning "    ⚠️  No se pudo obtener URL de ViaRewind"
    fi
    
    cd "$INSTALL_DIR"
    print_success "Plugins de compatibilidad descargados"
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

# Configurar estructura de mundos
setup_world_structure() {
    print_info "Configurando estructura de mundos..."
    
    cd "$INSTALL_DIR"
    
    # Si ya existe worlds/ y tiene mundos, no sobrescribir
    if [ -d "worlds" ] && [ -f "worlds/worlds.json" ]; then
        print_success "Estructura de mundos existente detectada, preservando..."
        return 0
    fi
    
    # Crear directorio maestro de mundos
    mkdir -p worlds/mundo-inicial
    
    # Crear subdirectorios de dimensiones
    mkdir -p worlds/mundo-inicial/world
    mkdir -p worlds/mundo-inicial/world_nether
    mkdir -p worlds/mundo-inicial/world_the_end
    
    # Crear metadata.json del mundo inicial
    cat > worlds/mundo-inicial/metadata.json << META
{
  "name": "Mundo Inicial",
  "slug": "mundo-inicial",
  "description": "Mundo principal del servidor",
  "gamemode": "survival",
  "difficulty": "normal",
  "created_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "last_played": null,
  "size_mb": 0,
  "seed": "",
  "version": "1.20.6",
  "spawn": {"x": 0, "y": 64, "z": 0},
  "settings": {
    "pvp": true,
    "spawn_monsters": true,
    "spawn_animals": true,
    "view_distance": 10,
    "max_players": 20
  },
  "tags": ["principal", "rpg"],
  "isRPG": true
}
META
    
    # Crear worlds.json
    cat > worlds/worlds.json << WCONFIG
{
  "active_world": "mundo-inicial",
  "worlds": [
    {
      "slug": "mundo-inicial",
      "status": "active",
      "auto_backup": true,
      "backup_interval": "6h"
    }
  ],
  "settings": {
    "max_worlds": 10,
    "auto_backup_before_switch": true,
    "keep_backups": 5
  }
}
WCONFIG
    
    # Crear symlink 'active' al mundo inicial
    cd worlds
    ln -sf mundo-inicial active
    cd "$INSTALL_DIR"
    
    print_success "Estructura de mundos creada"
}

# Crear symlinks de mundos en minecraft-server
create_world_symlinks() {
    print_info "Creando symlinks de mundos..."
    
    cd "$MC_DIR"
    
    # Eliminar directorios/symlinks si existen
    rm -rf world world_nether world_the_end 2>/dev/null || true
    
    # Crear symlinks relativos apuntando a ../worlds/active/
    ln -sf ../worlds/active/world world
    ln -sf ../worlds/active/world_nether world_nether
    ln -sf ../worlds/active/world_the_end world_the_end
    
    print_success "Symlinks de mundos creados"
    cd "$INSTALL_DIR"
}

# Forzar server.properties antes del inicio
force_server_properties() {
    print_info "Forzando configuración del servidor..."
    
    # Si existe config/server.properties, usarlo como base
    if [ -f "$CONFIG_DIR/server.properties" ]; then
        cp "$CONFIG_DIR/server.properties" "$MC_DIR/server.properties"
    fi
    
    # Forzar level-name=world para que use el symlink
    sed -i 's/^level-name=.*/level-name=world/' "$MC_DIR/server.properties"
    
    # Asegurar que RCON esté habilitado
    if ! grep -q "^enable-rcon=" "$MC_DIR/server.properties"; then
        echo "enable-rcon=true" >> "$MC_DIR/server.properties"
    else
        sed -i 's/^enable-rcon=.*/enable-rcon=true/' "$MC_DIR/server.properties"
    fi
    
    if ! grep -q "^rcon.port=" "$MC_DIR/server.properties"; then
        echo "rcon.port=25575" >> "$MC_DIR/server.properties"
    fi
    
    if ! grep -q "^rcon.password=" "$MC_DIR/server.properties"; then
        echo "rcon.password=minecraft" >> "$MC_DIR/server.properties"
    fi
    
    print_success "Configuración forzada"
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
    pip install Flask python-dotenv requests Werkzeug bcrypt flask-login docker mcrcon psutil
    
    # Crear archivo .env si no existe
    if [ ! -f .env ]; then
        SECRET_KEY=$(openssl rand -hex 32 2>/dev/null || cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
        
        # Generar hash de contraseña usando Python
        source venv/bin/activate
        ADMIN_PASSWORD_HASH=$(python3 -c "from werkzeug.security import generate_password_hash; print(generate_password_hash('admin123456'))")
        deactivate
        
        cat > .env << ENV_FILE
SECRET_KEY=$SECRET_KEY
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=$ADMIN_PASSWORD_HASH
MINECRAFT_DIR=$MC_DIR
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
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/panel.pid"

cd "$SCRIPT_DIR"

# Verificar si ya está corriendo
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Panel web ya está ejecutándose (PID: $PID)"
        exit 1
    fi
    rm -f "$PID_FILE"
fi

source venv/bin/activate
export FLASK_APP=app.py

# Iniciar en background con nohup
nohup python3 app.py > web.log 2>&1 &
echo $! > "$PID_FILE"

echo "Panel web iniciado (PID: $(cat "$PID_FILE"))"
echo "URL: http://localhost:5000"
echo "Logs: tail -f $SCRIPT_DIR/web.log"
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
setup_world_structure
create_world_symlinks
force_server_properties
download_compatibility_plugins
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
# Script server-control.sh (control unificado)
cat > server-control.sh << 'CONTROL'
#!/bin/bash
# Script de control unificado para servidor Minecraft y panel web

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MC_DIR="$SCRIPT_DIR/minecraft-server"
WEB_DIR="$SCRIPT_DIR/mmorpg-web"
MC_PID_FILE="$MC_DIR/server.pid"
WEB_PID_FILE="$WEB_DIR/panel.pid"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

show_help() {
    echo "Uso: $0 {start|stop|restart|status|logs} {server|web|all}"
    echo ""
    echo "Comandos:"
    echo "  start    - Iniciar servicio"
    echo "  stop     - Detener servicio"
    echo "  restart  - Reiniciar servicio"
    echo "  status   - Ver estado del servicio"
    echo "  logs     - Ver logs en tiempo real"
    echo ""
    echo "Servicios:"
    echo "  server   - Servidor Minecraft"
    echo "  web      - Panel Web"
    echo "  all      - Ambos servicios"
    echo ""
    echo "Ejemplos:"
    echo "  $0 start server    # Iniciar solo el servidor"
    echo "  $0 stop all        # Detener todo"
    echo "  $0 restart web     # Reiniciar panel web"
}

start_server() {
    if [ -f "$MC_PID_FILE" ]; then
        PID=$(cat "$MC_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo -e "${YELLOW}[!] Servidor ya está ejecutándose (PID: $PID)${NC}"
            return 1
        fi
    fi
    
    echo -e "${CYAN}[INFO] Iniciando servidor Minecraft...${NC}"
    cd "$MC_DIR"
    nohup ./start.sh > /dev/null 2>&1 &
    echo $! > "$MC_PID_FILE"
    sleep 2
    
    if kill -0 $(cat "$MC_PID_FILE") 2>/dev/null; then
        echo -e "${GREEN}[✓] Servidor iniciado (PID: $(cat "$MC_PID_FILE"))${NC}"
        return 0
    else
        echo -e "${RED}[✗] Error al iniciar el servidor${NC}"
        return 1
    fi
}

stop_server() {
    if [ ! -f "$MC_PID_FILE" ]; then
        echo -e "${YELLOW}[!] Servidor no está ejecutándose${NC}"
        return 1
    fi
    
    PID=$(cat "$MC_PID_FILE")
    if ! kill -0 "$PID" 2>/dev/null; then
        echo -e "${YELLOW}[!] Servidor no está ejecutándose${NC}"
        rm -f "$MC_PID_FILE"
        return 1
    fi
    
    echo -e "${CYAN}[INFO] Deteniendo servidor Minecraft (PID: $PID)...${NC}"
    kill "$PID"
    
    # Esperar hasta 30 segundos
    for i in {1..30}; do
        if ! kill -0 "$PID" 2>/dev/null; then
            rm -f "$MC_PID_FILE"
            echo -e "${GREEN}[✓] Servidor detenido${NC}"
            return 0
        fi
        sleep 1
    done
    
    # Si aún no se detuvo, forzar
    kill -9 "$PID" 2>/dev/null
    rm -f "$MC_PID_FILE"
    echo -e "${GREEN}[✓] Servidor detenido (forzado)${NC}"
    return 0
}

start_web() {
    if [ -f "$WEB_PID_FILE" ]; then
        PID=$(cat "$WEB_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo -e "${YELLOW}[!] Panel web ya está ejecutándose (PID: $PID)${NC}"
            return 1
        fi
    fi
    
    echo -e "${CYAN}[INFO] Iniciando panel web...${NC}"
    cd "$WEB_DIR"
    ./start-web.sh
    sleep 2
    
    if [ -f "$WEB_PID_FILE" ] && kill -0 $(cat "$WEB_PID_FILE") 2>/dev/null; then
        echo -e "${GREEN}[✓] Panel web iniciado (PID: $(cat "$WEB_PID_FILE"))${NC}"
        echo -e "${CYAN}[INFO] URL: http://localhost:5000${NC}"
        return 0
    else
        echo -e "${RED}[✗] Error al iniciar el panel web${NC}"
        return 1
    fi
}

stop_web() {
    if [ ! -f "$WEB_PID_FILE" ]; then
        echo -e "${YELLOW}[!] Panel web no está ejecutándose${NC}"
        return 1
    fi
    
    PID=$(cat "$WEB_PID_FILE")
    if ! kill -0 "$PID" 2>/dev/null; then
        echo -e "${YELLOW}[!] Panel web no está ejecutándose${NC}"
        rm -f "$WEB_PID_FILE"
        return 1
    fi
    
    echo -e "${CYAN}[INFO] Deteniendo panel web (PID: $PID)...${NC}"
    kill "$PID"
    sleep 2
    
    if ! kill -0 "$PID" 2>/dev/null; then
        rm -f "$WEB_PID_FILE"
        echo -e "${GREEN}[✓] Panel web detenido${NC}"
        return 0
    fi
    
    # Si aún no se detuvo, forzar
    kill -9 "$PID" 2>/dev/null
    rm -f "$WEB_PID_FILE"
    echo -e "${GREEN}[✓] Panel web detenido (forzado)${NC}"
    return 0
}

status_server() {
    if [ -f "$MC_PID_FILE" ]; then
        PID=$(cat "$MC_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo -e "${GREEN}[✓] Servidor Minecraft: EJECUTÁNDOSE (PID: $PID)${NC}"
            return 0
        fi
    fi
    echo -e "${RED}[✗] Servidor Minecraft: DETENIDO${NC}"
    return 1
}

status_web() {
    if [ -f "$WEB_PID_FILE" ]; then
        PID=$(cat "$WEB_PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo -e "${GREEN}[✓] Panel Web: EJECUTÁNDOSE (PID: $PID)${NC}"
            echo -e "${CYAN}    URL: http://localhost:5000${NC}"
            return 0
        fi
    fi
    echo -e "${RED}[✗] Panel Web: DETENIDO${NC}"
    return 1
}

logs_server() {
    if [ -f "$MC_DIR/logs/latest.log" ]; then
        tail -f "$MC_DIR/logs/latest.log"
    else
        echo -e "${RED}[✗] No se encontró el archivo de logs${NC}"
        return 1
    fi
}

logs_web() {
    if [ -f "$WEB_DIR/web.log" ]; then
        tail -f "$WEB_DIR/web.log"
    else
        echo -e "${RED}[✗] No se encontró el archivo de logs${NC}"
        return 1
    fi
}

# Procesar comando
COMMAND=$1
TARGET=$2

if [ -z "$COMMAND" ] || [ -z "$TARGET" ]; then
    show_help
    exit 1
fi

case "$COMMAND" in
    start)
        case "$TARGET" in
            server) start_server ;;
            web) start_web ;;
            all)
                start_server
                start_web
                ;;
            *) show_help; exit 1 ;;
        esac
        ;;
    stop)
        case "$TARGET" in
            server) stop_server ;;
            web) stop_web ;;
            all)
                stop_server
                stop_web
                ;;
            *) show_help; exit 1 ;;
        esac
        ;;
    restart)
        case "$TARGET" in
            server)
                stop_server
                sleep 2
                start_server
                ;;
            web)
                stop_web
                sleep 2
                start_web
                ;;
            all)
                stop_server
                stop_web
                sleep 2
                start_server
                start_web
                ;;
            *) show_help; exit 1 ;;
        esac
        ;;
    status)
        case "$TARGET" in
            server) status_server ;;
            web) status_web ;;
            all)
                status_server
                status_web
                ;;
            *) show_help; exit 1 ;;
        esac
        ;;
    logs)
        case "$TARGET" in
            server) logs_server ;;
            web) logs_web ;;
            *) show_help; exit 1 ;;
        esac
        ;;
    *)
        show_help
        exit 1
        ;;
esac
CONTROL
chmod +x server-control.sh

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
