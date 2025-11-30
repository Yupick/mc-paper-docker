#!/bin/bash

# Script para ver los logs del panel web en tiempo real
# Ejecutar: ./logs-web-panel.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/web"
LOG_FILE="$WEB_DIR/panel.log"
PID_FILE="$WEB_DIR/panel.pid"

echo "========================================="
echo "📋 Logs del Panel Web Minecraft"
echo "========================================="
echo ""

# Verificar si está corriendo
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "✅ Panel web corriendo (PID: $PID)"
    else
        echo "⚠️  Panel web detenido"
    fi
else
    echo "⚠️  Panel web no está corriendo"
fi

echo ""

# Verificar si existe el archivo de logs
if [ ! -f "$LOG_FILE" ]; then
    echo "⚠️  No hay archivo de logs disponible"
    echo ""
    echo "El archivo de logs se creará cuando el panel inicie."
    echo "Ejecuta: ./start-web-panel.sh"
    exit 0
fi

# Obtener tamaño del archivo
LOG_SIZE=$(du -h "$LOG_FILE" | cut -f1)
echo "📄 Archivo de logs: $LOG_FILE ($LOG_SIZE)"
echo ""
echo "💡 Presiona Ctrl+C para salir"
echo "========================================="
echo ""

# Mostrar logs en tiempo real
tail -f "$LOG_FILE"
