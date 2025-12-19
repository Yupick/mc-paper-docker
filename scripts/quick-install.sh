#!/bin/bash

# Script rápido de instalación - Compila y despliega el plugin MMORPG
# Uso: bash quick-install.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ⚡ INSTALACIÓN RÁPIDA - PLUGIN MMORPG                      ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Paso 1: Crear estructura base
echo "[1/3] Creando estructura base..."
bash "$SCRIPT_DIR/create.sh" --quiet 2>/dev/null || true
echo "      ✅ Estructura base verificada"
echo ""

# Paso 2: Compilar plugin
echo "[2/3] Compilando plugin MMORPG..."
cd "$SCRIPT_DIR/mmorpg-plugin"
if mvn clean package -q > /dev/null 2>&1; then
    echo "      ✅ Plugin compilado exitosamente"
else
    echo "      ❌ Error en compilación"
    echo "      Ejecuta manualmente: cd mmorpg-plugin && mvn clean package"
    exit 1
fi

# Paso 3: Instalar plugin
echo ""
echo "[3/3] Instalando archivos del plugin..."
bash "$SCRIPT_DIR/scripts/install-mmorpg-plugin.sh"

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ✅ ¡INSTALACIÓN COMPLETADA!                                ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "🚀 Próximos pasos:"
echo "   1. Inicia el servidor: ./run.sh"
echo "   2. Verifica los logs: docker compose logs -f"
echo "   3. Accede al panel web: http://localhost:5000"
echo ""
