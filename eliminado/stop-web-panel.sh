#!/bin/bash

# Script para detener el panel de administración web de Minecraft
# Ejecutar: ./stop-web-panel.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/web"
PID_FILE="$WEB_DIR/panel.pid"
LOG_FILE="$WEB_DIR/panel.log"

echo "========================================="
echo "🛑 Deteniendo Panel Web Minecraft"
echo "========================================="
echo ""

# Verificar si existe el archivo PID
if [ ! -f "$PID_FILE" ]; then
    echo "⚠️  El panel web no está corriendo (no se encontró archivo PID)"
    echo ""
    # Verificar si hay algún proceso en el puerto 5000
    if lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo "⚠️  Pero hay un proceso usando el puerto 5000"
        PID=$(lsof -ti:5000)
        echo "   PID: $PID"
        echo ""
        read -p "¿Deseas detenerlo? (s/n): " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[SsYy]$ ]]; then
            kill -9 $PID
            echo "✅ Proceso detenido"
        fi
    fi
    exit 0
fi

# Leer PID
PID=$(cat "$PID_FILE")

# Verificar si el proceso está corriendo
if ps -p $PID > /dev/null 2>&1; then
    echo "📊 Proceso encontrado:"
    echo "   PID: $PID"
    echo ""
    
    # Intentar detener gracefully
    echo "🔄 Deteniendo proceso..."
    kill $PID
    
    # Esperar hasta 5 segundos
    for i in {1..5}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            echo "✅ Panel web detenido correctamente"
            rm -f "$PID_FILE"
            echo ""
            echo "📋 Los logs se conservan en: $LOG_FILE"
            exit 0
        fi
        sleep 1
    done
    
    # Si no se detuvo, forzar
    echo "⚠️  El proceso no respondió, forzando detención..."
    kill -9 $PID
    
    if ! ps -p $PID > /dev/null 2>&1; then
        echo "✅ Panel web detenido forzosamente"
        rm -f "$PID_FILE"
    else
        echo "❌ ERROR: No se pudo detener el proceso"
        exit 1
    fi
else
    echo "⚠️  El proceso con PID $PID no está corriendo"
    rm -f "$PID_FILE"
    echo "✅ Archivo PID limpiado"
fi

echo ""
echo "========================================="
echo "💡 Para iniciar nuevamente: ./start-web-panel.sh"
echo "========================================="
