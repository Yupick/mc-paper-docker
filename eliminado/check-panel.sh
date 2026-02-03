#!/bin/bash

# Script de verificación del Panel Web Minecraft
# Ejecutar: ./check-panel.sh

echo "╔════════════════════════════════════════════════════════╗"
echo "║   🔍 VERIFICACIÓN DEL PANEL WEB MINECRAFT              ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_mark="${GREEN}✅${NC}"
cross_mark="${RED}❌${NC}"
warning_mark="${YELLOW}⚠️${NC}"

# Función para verificar
check() {
    if [ $1 -eq 0 ]; then
        echo -e "$check_mark $2"
        return 0
    else
        echo -e "$cross_mark $2"
        return 1
    fi
}

# 1. Verificar estructura de directorios
echo "📁 Verificando estructura de directorios..."
[ -d "/home/mkd/contenedores/mc-paper/web" ]; check $? "Directorio web/"
[ -d "/home/mkd/contenedores/mc-paper/backups" ]; check $? "Directorio backups/"
[ -d "/home/mkd/contenedores/mc-paper/web/templates" ]; check $? "Directorio templates/"
echo ""

# 2. Verificar archivos principales
echo "📄 Verificando archivos principales..."
[ -f "/home/mkd/contenedores/mc-paper/web/app.py" ]; check $? "app.py"
[ -f "/home/mkd/contenedores/mc-paper/web/.env" ]; check $? ".env"
[ -f "/home/mkd/contenedores/mc-paper/web/templates/dashboard.html" ]; check $? "dashboard.html"
[ -f "/home/mkd/contenedores/mc-paper/web/templates/login.html" ]; check $? "login.html"
[ -f "/home/mkd/contenedores/mc-paper/start-web-panel.sh" ]; check $? "start-web-panel.sh"
[ -f "/home/mkd/contenedores/mc-paper/web/generate_hash.py" ]; check $? "generate_hash.py"
echo ""

# 3. Verificar backups
echo "💾 Verificando backups..."
[ -f "/home/mkd/contenedores/mc-paper/web/templates/dashboard.html.backup" ]; check $? "dashboard.html.backup"
echo ""

# 4. Verificar permisos de ejecución
echo "🔐 Verificando permisos de scripts..."
[ -x "/home/mkd/contenedores/mc-paper/start-web-panel.sh" ]; check $? "start-web-panel.sh ejecutable"
[ -x "/home/mkd/contenedores/mc-paper/web/generate_hash.py" ]; check $? "generate_hash.py ejecutable"
echo ""

# 5. Verificar dependencias de Python
echo "🐍 Verificando dependencias de Python..."
python3 -c "import flask" 2>/dev/null; check $? "Flask instalado"
python3 -c "import flask_login" 2>/dev/null; check $? "Flask-Login instalado"
python3 -c "import docker" 2>/dev/null; check $? "Docker SDK instalado"
python3 -c "from dotenv import load_dotenv" 2>/dev/null; check $? "python-dotenv instalado"
python3 -c "from werkzeug.security import generate_password_hash" 2>/dev/null; check $? "werkzeug instalado"
echo ""

# 6. Verificar configuración en .env
echo "⚙️  Verificando configuración .env..."
cd /home/mkd/contenedores/mc-paper/web
if [ -f ".env" ]; then
    source .env
    [ ! -z "$ADMIN_USERNAME" ]; check $? "ADMIN_USERNAME configurado"
    
    if [ ! -z "$ADMIN_PASSWORD_HASH" ]; then
        echo -e "$check_mark ADMIN_PASSWORD_HASH configurado (seguro)"
    elif [ ! -z "$ADMIN_PASSWORD" ]; then
        echo -e "$warning_mark ADMIN_PASSWORD configurado (considera usar hash)"
    else
        echo -e "$cross_mark Ninguna contraseña configurada"
    fi
    
    [ ! -z "$SECRET_KEY" ]; check $? "SECRET_KEY configurado"
else
    echo -e "$cross_mark Archivo .env no encontrado"
fi
echo ""

# 7. Verificar Docker
echo "🐳 Verificando Docker..."
docker ps >/dev/null 2>&1; check $? "Docker funcionando"
docker ps -a | grep -q "mc-paper"; check $? "Contenedor mc-paper existe"
echo ""

# 8. Verificar puerto 5000
echo "🌐 Verificando disponibilidad de puerto..."
if lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "$warning_mark Puerto 5000 en uso (panel puede estar corriendo)"
else
    echo -e "$check_mark Puerto 5000 disponible"
fi
echo ""

# 9. Verificar nuevos endpoints en app.py
echo "🔌 Verificando nuevos endpoints API..."
grep -q "def get_players" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/server/players"
grep -q "def get_version" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/server/version"
grep -q "def get_uptime" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/server/uptime"
grep -q "def update_server" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/server/update"
grep -q "def update_all_plugins" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/plugins/update-all"
grep -q "def create_backup" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/backup/create"
grep -q "def list_backups" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/backup/list"
grep -q "def download_backup" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/backup/download"
grep -q "def delete_backup" /home/mkd/contenedores/mc-paper/web/app.py; check $? "Endpoint /api/backup/delete"
echo ""

# 10. Verificar funciones JavaScript en dashboard
echo "⚡ Verificando funciones JavaScript en dashboard..."
grep -q "function loadPlayers" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "loadPlayers()"
grep -q "function loadVersion" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "loadVersion()"
grep -q "function loadUptime" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "loadUptime()"
grep -q "function updateServer" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "updateServer()"
grep -q "function updateAllPlugins" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "updateAllPlugins()"
grep -q "function createBackup" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "createBackup()"
grep -q "function loadBackups" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "loadBackups()"
grep -q "function deleteBackup" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "deleteBackup()"
echo ""

# 11. Verificar elementos UI en dashboard
echo "🎨 Verificando elementos UI en dashboard..."
grep -q "players-count" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Card de jugadores online"
grep -q "server-version" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Display de versión"
grep -q "server-uptime" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Display de uptime"
grep -q "updateServer()" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Botón actualizar servidor"
grep -q "updateAllPlugins()" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Botón actualizar plugins"
grep -q "backups-list" /home/mkd/contenedores/mc-paper/web/templates/dashboard.html; check $? "Card de backups"
echo ""

# Resumen final
echo "╔════════════════════════════════════════════════════════╗"
echo "║   📊 RESUMEN DE VERIFICACIÓN                           ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Contar éxitos
total_checks=$(grep -E "check \$\?" "$0" | wc -l)
echo "✅ Verificación completada"
echo ""

# Siguiente paso
echo "╔════════════════════════════════════════════════════════╗"
echo "║   🚀 SIGUIENTE PASO                                    ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Para iniciar el panel web, ejecuta:"
echo ""
echo -e "  ${GREEN}./start-web-panel.sh${NC}"
echo ""
echo "O manualmente:"
echo ""
echo -e "  ${GREEN}cd /home/mkd/contenedores/mc-paper/web${NC}"
echo -e "  ${GREEN}python3 app.py${NC}"
echo ""
echo "Luego accede a: http://localhost:5000"
echo ""

# Advertencias finales
if [ -z "$ADMIN_PASSWORD_HASH" ] && [ ! -z "$ADMIN_PASSWORD" ]; then
    echo -e "${YELLOW}⚠️  ADVERTENCIA DE SEGURIDAD:${NC}"
    echo "   Considera generar un hash de contraseña:"
    echo -e "   ${GREEN}python3 /home/mkd/contenedores/mc-paper/web/generate_hash.py${NC}"
    echo ""
fi

# Verificar si faltan dependencias
if ! python3 -c "import flask" 2>/dev/null || ! python3 -c "import docker" 2>/dev/null; then
    echo -e "${YELLOW}⚠️  DEPENDENCIAS FALTANTES:${NC}"
    echo "   Instala las dependencias con:"
    echo -e "   ${GREEN}pip3 install flask flask-login python-dotenv docker --break-system-packages${NC}"
    echo ""
fi

echo "✨ ¡Listo para usar!"
