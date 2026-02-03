#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Selecciona qué logs ver:"
echo "1) Servidor Minecraft"
echo "2) Panel Web"
echo "3) Ambos (dividido)"
read -p "Opción [1]: " option
option=${option:-1}

case $option in
    1)
        tail -f "$SCRIPT_DIR/minecraft-server/logs/latest.log"
        ;;
    2)
        if [ -f "$SCRIPT_DIR/mmorpg-web/web.log" ]; then
            tail -f "$SCRIPT_DIR/mmorpg-web/web.log"
        else
            echo "Log del panel web no encontrado. ¿Está corriendo?"
        fi
        ;;
    3)
        if command -v tmux &> /dev/null; then
            tmux new-session \; \
              send-keys "tail -f $SCRIPT_DIR/minecraft-server/logs/latest.log" C-m \; \
              split-window -h \; \
              send-keys "tail -f $SCRIPT_DIR/mmorpg-web/web.log 2>/dev/null || echo 'Panel web no corriendo'" C-m
        else
            echo "tmux no está instalado. Mostrando solo logs del servidor..."
            tail -f "$SCRIPT_DIR/minecraft-server/logs/latest.log"
        fi
        ;;
    *)
        echo "Opción inválida"
        exit 1
        ;;
esac
