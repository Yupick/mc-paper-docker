#!/bin/bash

###############################################################################
# MMORPG Server - Desinstalador
###############################################################################

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MC_DIR="$SCRIPT_DIR/minecraft-server"
WEB_DIR="$SCRIPT_DIR/mmorpg-web"

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

clear
echo -e "${RED}"
cat << "BANNER"
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║    ADVERTENCIA: Desinstalación de MMORPG Server         ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
BANNER
echo -e "${NC}"

print_warning "Esto eliminará:"
echo "  - Servidor Minecraft ($MC_DIR)"
echo "  - Panel Web ($WEB_DIR)"
echo "  - Servicios systemd (si existen)"
echo "  - Scripts de control"
echo ""
print_warning "Los siguientes archivos NO se eliminarán:"
echo "  - Código fuente (mmorpg-plugin/, web/)"
echo "  - Archivos de configuración en config/"
echo ""

read -p "¿Estás seguro de que quieres continuar? Escribe 'DESINSTALAR': " confirm

if [ "$confirm" != "DESINSTALAR" ]; then
    print_warning "Desinstalación cancelada"
    exit 0
fi

echo ""
print_warning "Última oportunidad. ¿Continuar?"
read -p "[y/N]: " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Desinstalación cancelada"
    exit 0
fi

# Detener servicios si existen
if systemctl list-units --full -all | grep -q "mmorpg-server.service"; then
    print_warning "Deteniendo servicio mmorpg-server..."
    sudo systemctl stop mmorpg-server.service
    sudo systemctl disable mmorpg-server.service
    sudo rm /etc/systemd/system/mmorpg-server.service
fi

if systemctl list-units --full -all | grep -q "mmorpg-web.service"; then
    print_warning "Deteniendo servicio mmorpg-web..."
    sudo systemctl stop mmorpg-web.service
    sudo systemctl disable mmorpg-web.service
    sudo rm /etc/systemd/system/mmorpg-web.service
fi

sudo systemctl daemon-reload 2>/dev/null

# Eliminar directorios
if [ -d "$MC_DIR" ]; then
    print_warning "Eliminando servidor Minecraft..."
    rm -rf "$MC_DIR"
    print_success "Servidor eliminado"
fi

if [ -d "$WEB_DIR" ]; then
    print_warning "Eliminando panel web..."
    rm -rf "$WEB_DIR"
    print_success "Panel web eliminado"
fi

# Eliminar scripts
for script in start-server.sh start-web.sh logs.sh server-control.sh update-from-github.sh; do
    if [ -f "$SCRIPT_DIR/$script" ]; then
        rm "$SCRIPT_DIR/$script"
    fi
done

print_success "Desinstalación completada"
print_warning "El código fuente permanece intacto en: $SCRIPT_DIR"
