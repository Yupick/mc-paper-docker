#!/bin/bash

# Script para ver el estado del panel web
# Ejecutar: ./status-web-panel.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEB_DIR="$SCRIPT_DIR/web"
PID_FILE="$WEB_DIR/panel.pid"
LOG_FILE="$WEB_DIR/panel.log"

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║        📊 ESTADO DEL PANEL WEB MINECRAFT                      ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Verificar si existe el archivo PID
if [ ! -f "$PID_FILE" ]; then
    echo "🔴 Estado: DETENIDO"
    echo ""
    echo "El panel web no está corriendo."
    echo ""
    echo "Para iniciarlo: ./start-web-panel.sh"
    exit 0
fi

# Leer PID
PID=$(cat "$PID_FILE")

# Verificar si el proceso está corriendo
if ! ps -p $PID > /dev/null 2>&1; then
    echo "🔴 Estado: DETENIDO (proceso no encontrado)"
    echo ""
    echo "El archivo PID existe pero el proceso no está corriendo."
    echo ""
    echo "Para limpiarlo e iniciarlo: ./start-web-panel.sh"
    rm -f "$PID_FILE"
    exit 0
fi

echo "🟢 Estado: CORRIENDO"
echo ""
echo "📊 Información del Proceso:"
echo "   PID: $PID"
echo "   Comando: $(ps -p $PID -o comm=)"
echo "   CPU: $(ps -p $PID -o %cpu= | xargs)%"
echo "   RAM: $(ps -p $PID -o %mem= | xargs)%"
echo "   Tiempo: $(ps -p $PID -o etime= | xargs)"
echo ""

# Verificar puerto
if lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "🌐 Red:"
    echo "   Puerto: 5000 (ESCUCHANDO)"
    echo "   Acceso local: http://localhost:5000"
    IP=$(hostname -I 2>/dev/null | awk '{print $1}')
    if [ ! -z "$IP" ]; then
        echo "   Acceso remoto: http://$IP:5000"
    fi
else
    echo "⚠️  Puerto 5000: NO ESCUCHANDO"
fi
echo ""

# Información de logs
if [ -f "$LOG_FILE" ]; then
    LOG_SIZE=$(du -h "$LOG_FILE" | cut -f1)
    LOG_LINES=$(wc -l < "$LOG_FILE")
    echo "📋 Logs:"
    echo "   Archivo: $LOG_FILE"
    echo "   Tamaño: $LOG_SIZE"
    echo "   Líneas: $LOG_LINES"
    echo ""
    echo "   Últimas 3 líneas:"
    tail -n 3 "$LOG_FILE" | sed 's/^/   /'
fi
echo ""

# Credenciales
if [ -f "$WEB_DIR/.env" ]; then
    source "$WEB_DIR/.env"
    echo "🔐 Credenciales:"
    echo "   Usuario: $ADMIN_USERNAME"
    echo "   Contraseña: [configurada en .env]"
fi
echo ""

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║  🔧 COMANDOS DISPONIBLES                                      ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "   ./logs-web-panel.sh     # Ver logs en tiempo real"
echo "   ./stop-web-panel.sh     # Detener el panel"
echo "   ./restart-web-panel.sh  # Reiniciar el panel"
echo ""
