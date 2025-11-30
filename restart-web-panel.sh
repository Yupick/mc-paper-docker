#!/bin/bash

# Script para reiniciar el panel de administración web de Minecraft
# Ejecutar: ./restart-web-panel.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "🔄 Reiniciando Panel Web Minecraft"
echo "========================================="
echo ""

# Detener el panel
echo "[1/2] Deteniendo panel actual..."
"$SCRIPT_DIR/stop-web-panel.sh"

echo ""
echo "[2/2] Iniciando panel..."
sleep 1

# Iniciar el panel
"$SCRIPT_DIR/start-web-panel.sh"
