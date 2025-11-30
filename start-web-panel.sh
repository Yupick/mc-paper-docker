#!/bin/bash

# Script para iniciar el panel de administración web de Minecraft
# Ejecutar: ./start-web-panel.sh

# Obtener la ruta del script (funciona tanto en la PC de diseño como en el servidor)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/web"
VENV_DIR="$SCRIPT_DIR/.venv"
PID_FILE="$WEB_DIR/panel.pid"
LOG_FILE="$WEB_DIR/panel.log"

cd "$SCRIPT_DIR"

# Función para verificar si el proceso está corriendo
is_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null 2>&1; then
            return 0  # Está corriendo
        else
            rm -f "$PID_FILE"
            return 1  # No está corriendo
        fi
    fi
    return 1  # No hay archivo PID
}

# Función para mostrar logs en tiempo real
show_logs() {
    echo "========================================="
    echo "📋 Logs del Panel Web (Ctrl+C para salir)"
    echo "========================================="
    echo ""
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        echo "⚠️  No hay logs disponibles aún"
        echo "   El archivo de logs se creará cuando el panel inicie"
    fi
}

# Si ya está corriendo, mostrar logs
if is_running; then
    PID=$(cat "$PID_FILE")
    echo "========================================="
    echo "ℹ️  El panel web ya está ejecutándose"
    echo "========================================="
    echo ""
    echo "📊 Estado:"
    echo "   PID: $PID"
    echo "   Puerto: 5000"
    echo "   Logs: $LOG_FILE"
    echo ""
    echo "📍 Acceso:"
    echo "   http://localhost:5000"
    echo "   http://$(hostname -I 2>/dev/null | awk '{print $1}'):5000"
    echo ""
    echo "🔧 Comandos útiles:"
    echo "   ./stop-web-panel.sh    # Detener el panel"
    echo "   ./restart-web-panel.sh # Reiniciar el panel"
    echo ""
    read -p "¿Deseas ver los logs en tiempo real? (s/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[SsYy]$ ]]; then
        show_logs
    fi
    exit 0
fi

cd "$SCRIPT_DIR"

echo "========================================="
echo "🚀 Panel Web Minecraft Server"
echo "========================================="
echo ""

# Verificar/Crear entorno virtual
if [ ! -d "$VENV_DIR" ]; then
    echo "📦 Creando entorno virtual Python..."
    if ! command -v python3 &> /dev/null; then
        echo "❌ ERROR: Python3 no está instalado"
        echo ""
        echo "Instala Python3 con:"
        echo "  sudo apt update && sudo apt install python3 python3-venv python3-pip -y"
        exit 1
    fi
    
    python3 -m venv "$VENV_DIR"
    if [ $? -ne 0 ]; then
        echo "❌ ERROR: No se pudo crear el entorno virtual"
        echo ""
        echo "Instala python3-venv con:"
        echo "  sudo apt install python3-venv -y"
        exit 1
    fi
    echo "✅ Entorno virtual creado"
fi

# Activar entorno virtual
echo "🔧 Activando entorno virtual..."
source "$VENV_DIR/bin/activate"

if [ $? -ne 0 ]; then
    echo "❌ ERROR: No se pudo activar el entorno virtual"
    exit 1
fi

echo "✅ Entorno virtual activado: $VIRTUAL_ENV"
echo ""

# Verificar permisos de Docker
echo "🐳 Verificando permisos de Docker..."
if ! docker ps &>/dev/null; then
    echo "⚠️  Tu usuario no tiene permisos para usar Docker"
    echo ""
    
    # Verificar si el usuario está en el grupo docker
    if ! groups | grep -q docker; then
        echo "📝 Agregando tu usuario al grupo 'docker'..."
        sudo usermod -aG docker $USER
        
        if [ $? -eq 0 ]; then
            echo "✅ Usuario agregado al grupo docker"
            echo ""
            echo "╔═══════════════════════════════════════════════════════════════╗"
            echo "║  ⚠️  ACCIÓN REQUERIDA                                          ║"
            echo "╚═══════════════════════════════════════════════════════════════╝"
            echo ""
            echo "Para aplicar los permisos, necesitas cerrar sesión y volver a entrar."
            echo ""
            echo "Opciones:"
            echo "  1. Cierra esta sesión SSH y vuelve a conectarte"
            echo "  2. O ejecuta: newgrp docker"
            echo ""
            echo "Luego vuelve a ejecutar: ./start-web-panel.sh"
            echo ""
            exit 0
        else
            echo "❌ Error al agregar usuario al grupo docker"
            echo ""
            echo "Ejecuta manualmente:"
            echo "  sudo usermod -aG docker $USER"
            echo "  logout  # Cierra sesión y vuelve a entrar"
            exit 1
        fi
    else
        echo "✅ Tu usuario ya está en el grupo docker"
        echo ""
        echo "╔═══════════════════════════════════════════════════════════════╗"
        echo "║  ⚠️  REINICIA TU SESIÓN                                        ║"
        echo "╚═══════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Los permisos de grupo no se han aplicado aún."
        echo ""
        echo "Opciones para aplicar permisos:"
        echo ""
        echo "  Opción 1 (Recomendada):"
        echo "    logout  # Cierra sesión SSH"
        echo "    # Vuelve a conectarte y ejecuta ./start-web-panel.sh"
        echo ""
        echo "  Opción 2 (Temporal para esta sesión):"
        echo "    newgrp docker"
        echo "    ./start-web-panel.sh"
        echo ""
        exit 0
    fi
else
    echo "✅ Permisos de Docker verificados"
fi
echo ""

cd "$WEB_DIR"

# Crear archivo .env si no existe
if [ ! -f ".env" ]; then
    echo "📝 Creando archivo .env con configuración inicial..."
    
    # Generar SECRET_KEY aleatoria
    SECRET_KEY=$(openssl rand -hex 32 2>/dev/null || python3 -c "import secrets; print(secrets.token_hex(32))")
    
    # Generar hash de contraseña por defecto
    DEFAULT_PASSWORD="minecraft123"
    PASSWORD_HASH=$(python3 -c "from werkzeug.security import generate_password_hash; print(generate_password_hash('$DEFAULT_PASSWORD'))" 2>/dev/null || echo "")
    
    # Crear archivo .env con rutas relativas
    cat > .env << EOF
# Panel de administración del servidor Minecraft
SECRET_KEY=$SECRET_KEY
ADMIN_USERNAME=admin

# Contraseña con hash (más seguro)
# Para cambiarla: python3 generate_hash.py tu_nueva_contraseña
ADMIN_PASSWORD_HASH=$PASSWORD_HASH

# O usa ADMIN_PASSWORD (menos seguro, solo para desarrollo)
ADMIN_PASSWORD=$DEFAULT_PASSWORD

# Nombre del contenedor Docker
DOCKER_CONTAINER_NAME=mc-paper
EOF
    
    echo "✅ Archivo .env creado"
    echo ""
    echo "🔐 Credenciales por defecto:"
    echo "   Usuario: admin"
    echo "   Contraseña: $DEFAULT_PASSWORD"
    echo ""
    echo "⚠️  IMPORTANTE: Cambia la contraseña con:"
    echo "   python3 generate_hash.py tu_nueva_contraseña"
    echo ""
fi

# Verificar e instalar dependencias de Python
echo "📦 Verificando dependencias de Python..."
MISSING_DEPS=0

for module in flask flask_login dotenv docker werkzeug; do
    if ! python3 -c "import $module" 2>/dev/null; then
        MISSING_DEPS=1
        break
    fi
done

if [ $MISSING_DEPS -eq 1 ]; then
    echo "⚙️  Instalando dependencias en el entorno virtual..."
    pip install --upgrade pip --quiet
    pip install flask flask-login python-dotenv docker werkzeug --quiet
    
    if [ $? -ne 0 ]; then
        echo "❌ ERROR: No se pudieron instalar las dependencias"
        exit 1
    fi
    echo "✅ Dependencias instaladas correctamente"
else
    echo "✅ Todas las dependencias ya están instaladas"
fi
echo ""

# Verificar si el puerto está en uso
if lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  El puerto 5000 ya está en uso"
    echo ""
    read -p "¿Deseas detener el proceso existente? (s/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[SsYy]$ ]]; then
        PID=$(lsof -ti:5000)
        kill -9 $PID
        echo "✅ Proceso detenido"
        sleep 1
    else
        echo "❌ Cancelado"
        exit 1
    fi
fi

echo ""
echo "✅ Todo listo!"
echo ""
echo "========================================="
echo "🌐 Iniciando servidor web en segundo plano..."
echo "========================================="
echo ""

# Crear archivo de log si no existe
touch "$LOG_FILE"

# Iniciar servidor Flask en segundo plano
nohup python3 app.py > "$LOG_FILE" 2>&1 &
SERVER_PID=$!

# Guardar PID
echo $SERVER_PID > "$PID_FILE"

# Esperar un momento para verificar que inició correctamente
sleep 2

if ps -p $SERVER_PID > /dev/null 2>&1; then
    echo "✅ Panel web iniciado exitosamente"
    echo ""
    echo "📊 Información:"
    echo "   PID: $SERVER_PID"
    echo "   Logs: $LOG_FILE"
    echo ""
    echo "📍 Accede al panel en:"
    echo "   http://localhost:5000"
    echo "   http://$(hostname -I 2>/dev/null | awk '{print $1}'):5000"
    echo ""
    echo "🔐 Credenciales:"
    source .env
    echo "   Usuario: $ADMIN_USERNAME"
    echo "   Contraseña: [configurada en .env]"
    echo ""
    echo "🔧 Comandos útiles:"
    echo "   ./start-web-panel.sh   # Ver logs (si ya está corriendo)"
    echo "   ./stop-web-panel.sh    # Detener el panel"
    echo "   tail -f $LOG_FILE      # Ver logs en tiempo real"
    echo ""
    echo "========================================="
    echo "💡 El panel está corriendo en segundo plano"
    echo "========================================="
else
    echo "❌ ERROR: El panel no pudo iniciarse"
    echo ""
    echo "Revisa los logs en: $LOG_FILE"
    rm -f "$PID_FILE"
    exit 1
fi
