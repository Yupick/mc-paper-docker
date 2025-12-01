#!/bin/bash

# Script para instalar todas las dependencias necesarias
# Útil para configurar el servidor por primera vez

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   📦 INSTALACIÓN DE DEPENDENCIAS - SERVIDOR MINECRAFT         ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Verificar si se está ejecutando como root
if [ "$EUID" -eq 0 ]; then
    echo "⚠️  No ejecutes este script como root (sudo)"
    echo "   El script pedirá permisos cuando sea necesario"
    exit 1
fi

# Detectar sistema operativo
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo "❌ No se pudo detectar el sistema operativo"
    exit 1
fi

echo "🖥️  Sistema operativo detectado: $PRETTY_NAME"
echo ""

# 1. Actualizar repositorios
echo "📋 [1/5] Actualizando repositorios..."
sudo apt update -qq

# 2. Instalar Python3 y herramientas
echo "🐍 [2/5] Instalando Python3 y herramientas..."
sudo apt install -y python3 python3-venv python3-pip curl jq lsof

if [ $? -ne 0 ]; then
    echo "❌ Error al instalar Python3"
    exit 1
fi

echo "✅ Python3 instalado: $(python3 --version)"

# 3. Instalar Docker (si no está instalado)
echo "🐳 [3/5] Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo "   Docker no está instalado. Instalando..."
    
    # Instalar Docker
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    rm get-docker.sh
    
    # Agregar usuario al grupo docker
    sudo usermod -aG docker $USER
    
    echo "✅ Docker instalado correctamente"
    echo "⚠️  IMPORTANTE: Cierra sesión y vuelve a entrar para aplicar permisos de Docker"
else
    echo "✅ Docker ya está instalado: $(docker --version)"
    
    # Verificar si el usuario está en el grupo docker
    if ! groups | grep -q docker; then
        echo "   ⚠️  Tu usuario no está en el grupo docker. Agregando..."
        sudo usermod -aG docker $USER
        echo "   ✅ Usuario agregado al grupo docker"
        echo "   ⚠️  IMPORTANTE: Cierra sesión y vuelve a entrar para aplicar permisos"
    else
        echo "   ✅ Usuario ya está en el grupo docker"
    fi
fi

# 4. Instalar Docker Compose
echo "🔧 [4/5] Verificando Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "   Docker Compose no está instalado. Instalando..."
    
    sudo apt install -y docker-compose
    
    if [ $? -ne 0 ]; then
        # Intentar con método alternativo
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    fi
    
    echo "✅ Docker Compose instalado correctamente"
else
    echo "✅ Docker Compose ya está instalado: $(docker-compose --version)"
fi

# 5. Crear entorno virtual de Python
echo "🔧 [5/5] Creando entorno virtual de Python..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"

if [ ! -d "$VENV_DIR" ]; then
    python3 -m venv "$VENV_DIR"
    echo "✅ Entorno virtual creado en $VENV_DIR"
else
    echo "✅ Entorno virtual ya existe"
fi

# Activar entorno virtual e instalar dependencias
source "$VENV_DIR/bin/activate"
pip install --upgrade pip --quiet
pip install flask flask-login python-dotenv docker werkzeug --quiet

if [ $? -ne 0 ]; then
    echo "❌ Error al instalar dependencias de Python"
    exit 1
fi

echo "✅ Dependencias de Python instaladas"

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ✅ INSTALACIÓN COMPLETADA                                   ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "📋 Resumen de lo instalado:"
echo "   • Python3: $(python3 --version)"
echo "   • Docker: $(docker --version 2>/dev/null || echo 'Instalado (reinicia sesión)')"
echo "   • Docker Compose: $(docker-compose --version 2>/dev/null || echo 'Instalado (reinicia sesión)')"
echo "   • Entorno virtual: $VENV_DIR"
echo "   • Dependencias Python: Flask, Docker SDK, etc."
echo ""
echo "🚀 Siguiente paso:"
echo ""
echo "   1. Si instalaste Docker por primera vez, cierra sesión y vuelve a entrar:"
echo "      logout"
echo ""
echo "   2. Luego ejecuta:"
echo "      ./create.sh       # Para crear el servidor por primera vez"
echo "      ./start-web-panel.sh   # Para iniciar el panel web"
echo ""
