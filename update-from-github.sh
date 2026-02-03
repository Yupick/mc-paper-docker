#!/bin/bash

###############################################################################
# MMORPG Server - Actualizador desde GitHub
###############################################################################

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MC_DIR="$SCRIPT_DIR/minecraft-server"
WEB_DIR="$SCRIPT_DIR/mmorpg-web"
PLUGIN_CHANGED=false

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

clear
echo -e "${BLUE}"
cat << "BANNER"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║    MMORPG Server - Actualización desde GitHub           ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
BANNER
echo -e "${NC}"

# Verificar que estamos en un repositorio git
if [ ! -d ".git" ]; then
    print_warning "No se detectó repositorio git"
    read -p "¿Clonar desde GitHub? Ingresa URL del repo: " repo_url
    if [ -z "$repo_url" ]; then
        print_warning "Actualización cancelada"
        exit 1
    fi
    git clone "$repo_url" temp_repo
    cp -r temp_repo/* .
    cp -r temp_repo/.git .
    rm -rf temp_repo
    PLUGIN_CHANGED=true
else
    print_info "Repositorio git detectado"
    
    # Mostrar estado actual
    print_info "Rama actual: $(git branch --show-current)"
    print_info "Último commit: $(git log -1 --oneline)"
    echo ""
    
    # Verificar cambios locales
    if ! git diff-index --quiet HEAD --; then
        print_warning "Hay cambios locales sin commitear:"
        git status --short
        echo ""
        read -p "¿Descartar cambios y actualizar? [y/N]: " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git reset --hard
            print_success "Cambios descartados"
        else
            print_warning "Actualización cancelada"
            exit 1
        fi
    fi
    
    # Actualizar desde GitHub
    print_info "Descargando actualizaciones..."
    git fetch origin
    
    CURRENT_COMMIT=$(git rev-parse HEAD)
    REMOTE_COMMIT=$(git rev-parse origin/master 2>/dev/null || git rev-parse origin/main 2>/dev/null)
    
    if [ "$CURRENT_COMMIT" = "$REMOTE_COMMIT" ]; then
        print_success "Ya estás en la última versión"
        exit 0
    fi
    
    print_info "Nuevos cambios disponibles:"
    git log --oneline HEAD..origin/master 2>/dev/null || git log --oneline HEAD..origin/main 2>/dev/null
    echo ""
    
    # Verificar si hay cambios en el plugin
    if git diff --name-only HEAD origin/master 2>/dev/null | grep -q "^mmorpg-plugin/" || \
       git diff --name-only HEAD origin/main 2>/dev/null | grep -q "^mmorpg-plugin/"; then
        print_warning "Se detectaron cambios en el plugin MMORPG"
        PLUGIN_CHANGED=true
    fi
    
    read -p "¿Aplicar actualizaciones? [Y/n]: " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        git pull origin master 2>/dev/null || git pull origin main 2>/dev/null
        print_success "Código actualizado"
    else
        print_warning "Actualización cancelada"
        exit 0
    fi
fi

# Compilar plugin si hay cambios
if [ "$PLUGIN_CHANGED" = true ]; then
    print_warning "Compilando plugin debido a cambios detectados..."
    
    # Verificar Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven no está instalado. Instalando..."
        if [ -f /etc/debian_version ]; then
            sudo apt-get update
            sudo apt-get install -y maven
        else
            print_error "No se pudo instalar Maven automáticamente"
            print_info "Por favor instala Maven manualmente y vuelve a ejecutar este script"
            exit 1
        fi
    fi
    
    cd mmorpg-plugin
    print_info "Ejecutando: mvn clean package..."
    
    if mvn clean package -DskipTests; then
        print_success "Plugin compilado exitosamente"
        
        # Copiar nuevo plugin al servidor
        if [ -f "target/mmorpg-plugin-1.0.0.jar" ]; then
            if [ -d "$MC_DIR/plugins" ]; then
                print_info "Actualizando plugin en el servidor..."
                
                # Backup del plugin anterior
                if [ -f "$MC_DIR/plugins/MMORPGPlugin.jar" ]; then
                    mv "$MC_DIR/plugins/MMORPGPlugin.jar" "$MC_DIR/plugins/MMORPGPlugin.jar.backup"
                    print_info "Backup creado: MMORPGPlugin.jar.backup"
                fi
                
                cp target/mmorpg-plugin-1.0.0.jar "$MC_DIR/plugins/MMORPGPlugin.jar"
                print_success "Plugin actualizado en el servidor"
                
                print_warning "¡IMPORTANTE! Debes reiniciar el servidor para aplicar los cambios del plugin"
            else
                print_warning "Directorio de plugins no encontrado. ¿El servidor está instalado?"
            fi
        else
            print_error "No se encontró el JAR compilado"
        fi
    else
        print_error "Error al compilar el plugin"
        print_warning "Continuando con actualización del panel web..."
    fi
    
    cd "$SCRIPT_DIR"
else
    print_info "No hay cambios en el plugin, omitiendo compilación"
fi

# Actualizar archivos del panel web
if [ -d "$WEB_DIR" ] && [ -d "web" ]; then
    print_info "Actualizando panel web..."
    
    # Respaldar .env
    if [ -f "$WEB_DIR/.env" ]; then
        cp "$WEB_DIR/.env" "$WEB_DIR/.env.backup"
        print_info "Backup de .env creado"
    fi
    
    # Copiar nuevos archivos (excepto .env y venv)
    rsync -av --exclude='.env' --exclude='venv' --exclude='*.pyc' --exclude='__pycache__' \
          web/ "$WEB_DIR/"
    
    # Restaurar .env
    if [ -f "$WEB_DIR/.env.backup" ]; then
        mv "$WEB_DIR/.env.backup" "$WEB_DIR/.env"
    fi
    
    # Actualizar dependencias Python
    print_info "Actualizando dependencias de Python..."
    cd "$WEB_DIR"
    
    if [ -d "venv" ]; then
        source venv/bin/activate
        pip install --upgrade pip
        pip install --upgrade Flask python-dotenv requests Werkzeug bcrypt flask-login
        deactivate
        print_success "Dependencias actualizadas"
    else
        print_warning "Entorno virtual no encontrado. Saltando actualización de dependencias."
    fi
    
    cd "$SCRIPT_DIR"
    print_success "Panel web actualizado"
fi

echo ""
print_success "╔═══════════════════════════════════════════════════════════╗"
print_success "║         ACTUALIZACIÓN COMPLETADA                          ║"
print_success "╚═══════════════════════════════════════════════════════════╝"
echo ""

if [ "$PLUGIN_CHANGED" = true ]; then
    print_warning "RECUERDA: El plugin fue actualizado"
    print_info "Reinicia el servidor para aplicar cambios:"
    echo "  ./server-control.sh restart"
    echo "  O manualmente:"
    echo "  ./server-control.sh mc-stop"
    echo "  ./server-control.sh mc-start"
else
    print_info "Solo se actualizó el panel web"
    print_info "Reinicia el panel si está corriendo:"
    echo "  ./server-control.sh web-restart"
fi

echo ""
print_info "Resumen de cambios:"
git log --oneline -5
echo ""
