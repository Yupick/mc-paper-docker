#!/bin/bash

# Script de verificación del Panel de Administración Minecraft
# Verifica que todos los componentes estén correctamente instalados

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ✅ VERIFICACIÓN DEL PANEL DE ADMINISTRACIÓN                 ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/web"
ERRORS=0
WARNINGS=0

# Función para mostrar check
check_ok() {
    echo "   ✅ $1"
}

check_error() {
    echo "   ❌ $1"
    ((ERRORS++))
}

check_warning() {
    echo "   ⚠️  $1"
    ((WARNINGS++))
}

# 1. Verificar estructura de directorios
echo "📁 [1/8] Verificando estructura de directorios..."

if [ -d "$WEB_DIR" ]; then
    check_ok "Directorio web/ existe"
else
    check_error "Directorio web/ NO existe"
fi

if [ -d "$WEB_DIR/templates" ]; then
    check_ok "Directorio web/templates/ existe"
else
    check_error "Directorio web/templates/ NO existe"
fi

if [ -d "$WEB_DIR/static" ]; then
    check_ok "Directorio web/static/ existe"
else
    check_error "Directorio web/static/ NO existe"
fi

if [ -d "$SCRIPT_DIR/backups" ]; then
    check_ok "Directorio backups/ existe"
else
    check_warning "Directorio backups/ no existe (se creará automáticamente)"
fi

echo ""

# 2. Verificar archivos Python
echo "🐍 [2/8] Verificando archivos Python..."

if [ -f "$WEB_DIR/app.py" ]; then
    check_ok "app.py existe"
    
    # Verificar que tenga las rutas nuevas
    if grep -q "dashboard_v2.html" "$WEB_DIR/app.py"; then
        check_ok "app.py tiene ruta a dashboard_v2.html"
    else
        check_error "app.py NO tiene ruta a dashboard_v2.html"
    fi
else
    check_error "app.py NO existe"
fi

if [ -f "$WEB_DIR/requirements.txt" ]; then
    check_ok "requirements.txt existe"
else
    check_error "requirements.txt NO existe"
fi

echo ""

# 3. Verificar templates
echo "📄 [3/8] Verificando templates HTML..."

if [ -f "$WEB_DIR/templates/login.html" ]; then
    check_ok "login.html existe"
else
    check_error "login.html NO existe"
fi

if [ -f "$WEB_DIR/templates/dashboard_v2.html" ]; then
    check_ok "dashboard_v2.html existe (nuevo dashboard)"
else
    check_error "dashboard_v2.html NO existe"
fi

if [ -f "$WEB_DIR/templates/dashboard.html" ]; then
    check_ok "dashboard.html existe (legacy)"
else
    check_warning "dashboard.html no existe (no es crítico)"
fi

echo ""

# 4. Verificar archivos estáticos
echo "📦 [4/8] Verificando archivos estáticos..."

if [ -f "$WEB_DIR/static/dashboard.js" ]; then
    SIZE=$(stat -f%z "$WEB_DIR/static/dashboard.js" 2>/dev/null || stat -c%s "$WEB_DIR/static/dashboard.js" 2>/dev/null)
    if [ $SIZE -gt 10000 ]; then
        check_ok "dashboard.js existe (${SIZE} bytes)"
    else
        check_error "dashboard.js existe pero es muy pequeño (${SIZE} bytes)"
    fi
else
    check_error "dashboard.js NO existe"
fi

echo ""

# 5. Verificar Python y dependencias
echo "🔧 [5/8] Verificando Python y dependencias..."

if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    check_ok "Python instalado: $PYTHON_VERSION"
else
    check_error "Python3 NO está instalado"
fi

if [ -d "$WEB_DIR/.venv" ] || [ -d "$SCRIPT_DIR/.venv" ]; then
    check_ok "Virtual environment (.venv) existe"
    
    # Activar venv y verificar dependencias
    if [ -d "$SCRIPT_DIR/.venv" ]; then
        VENV_DIR="$SCRIPT_DIR/.venv"
    else
        VENV_DIR="$WEB_DIR/.venv"
    fi
    
    source "$VENV_DIR/bin/activate" 2>/dev/null
    
    if pip list 2>/dev/null | grep -q "Flask"; then
        check_ok "Flask instalado en venv"
    else
        check_error "Flask NO instalado en venv"
    fi
    
    if pip list 2>/dev/null | grep -q "docker"; then
        check_ok "docker (Python SDK) instalado"
    else
        check_error "docker (Python SDK) NO instalado"
    fi
    
    if pip list 2>/dev/null | grep -q "python-dotenv"; then
        check_ok "python-dotenv instalado"
    else
        check_error "python-dotenv NO instalado"
    fi
    
    deactivate 2>/dev/null
else
    check_warning "Virtual environment no existe (se puede crear con install-dependencies.sh)"
fi

echo ""

# 6. Verificar configuración .env
echo "⚙️  [6/8] Verificando configuración (.env)..."

if [ -f "$WEB_DIR/.env" ]; then
    check_ok ".env existe"
    
    if grep -q "ADMIN_USERNAME" "$WEB_DIR/.env"; then
        check_ok "ADMIN_USERNAME configurado"
    else
        check_error "ADMIN_USERNAME NO configurado en .env"
    fi
    
    if grep -q "ADMIN_PASSWORD" "$WEB_DIR/.env"; then
        check_ok "ADMIN_PASSWORD configurado"
    else
        check_error "ADMIN_PASSWORD NO configurado en .env"
    fi
    
    if grep -q "SECRET_KEY" "$WEB_DIR/.env"; then
        check_ok "SECRET_KEY configurado"
    else
        check_error "SECRET_KEY NO configurado en .env"
    fi
else
    check_error ".env NO existe (se creará al ejecutar start-web-panel.sh)"
fi

echo ""

# 7. Verificar Docker
echo "🐳 [7/8] Verificando Docker..."

if command -v docker &> /dev/null; then
    check_ok "Docker instalado"
    
    if docker ps &> /dev/null; then
        check_ok "Usuario tiene permisos de Docker"
    else
        check_warning "Usuario NO tiene permisos de Docker (intenta con sudo o agrega usuario al grupo docker)"
    fi
    
    if docker ps | grep -q "minecraft-paper"; then
        check_ok "Contenedor mc-paper está corriendo"
    else
        check_warning "Contenedor mc-paper NO está corriendo"
    fi
else
    check_error "Docker NO está instalado"
fi

echo ""

# 8. Verificar scripts de gestión
echo "📜 [8/8] Verificando scripts de gestión..."

SCRIPTS=("start-web-panel.sh" "stop-web-panel.sh" "restart-web-panel.sh" "logs-web-panel.sh" "status-web-panel.sh")

for script in "${SCRIPTS[@]}"; do
    if [ -f "$SCRIPT_DIR/$script" ]; then
        if [ -x "$SCRIPT_DIR/$script" ]; then
            check_ok "$script existe y es ejecutable"
        else
            check_warning "$script existe pero NO es ejecutable (chmod +x $script)"
        fi
    else
        check_error "$script NO existe"
    fi
done

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║  RESUMEN DE VERIFICACIÓN                                      ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ ¡TODO PERFECTO! El panel está listo para usar."
    echo ""
    echo "📝 Próximos pasos:"
    echo "   1. Inicia el panel: ./start-web-panel.sh"
    echo "   2. Accede a: http://localhost:5000"
    echo "   3. Usuario/contraseña: Ver web/.env"
    echo ""
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo "⚠️  TODO FUNCIONAL con $WARNINGS advertencias menores."
    echo ""
    echo "📝 Puedes iniciar el panel:"
    echo "   ./start-web-panel.sh"
    echo ""
    exit 0
else
    echo "❌ Se encontraron $ERRORS errores y $WARNINGS advertencias."
    echo ""
    echo "🔧 Soluciones:"
    
    if [ $ERRORS -gt 0 ]; then
        echo ""
        echo "Para instalar dependencias:"
        echo "   cd web"
        echo "   python3 -m venv .venv"
        echo "   source .venv/bin/activate"
        echo "   pip install -r requirements.txt"
        echo ""
        echo "Para configurar .env:"
        echo "   ./start-web-panel.sh  # Creará .env automáticamente"
        echo ""
    fi
    
    echo "Para más ayuda, consulta:"
    echo "   README.md (raíz del proyecto - GitHub)"
    echo "   web/docs/GUIA_COMPLETA.md (guía completa del panel)"
    echo "   web/docs/PANEL_README.md"
    echo ""
    exit 1
fi
