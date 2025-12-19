#!/bin/bash

echo "========================================="
echo "Ejecutando servidor Minecraft Paper"
echo "========================================="

echo ""
echo "[1/2] Verificando estado del contenedor..."

# Verificar si el contenedor existe
if ! sudo docker-compose ps -q minecraft-paper 2>/dev/null | grep -q .; then
    echo "⚠️  Contenedor no encontrado. Ejecuta ./create.sh primero."
    exit 1
fi

# Verificar si el contenedor está corriendo
if sudo docker ps --filter "name=minecraft-paper" --filter "status=running" | grep -q minecraft-paper; then
    echo "✅ El servidor ya está corriendo"
else
    echo "Iniciando contenedor..."
    sudo docker-compose up -d
    
    if [ $? -ne 0 ]; then
        echo "❌ Error al iniciar el contenedor"
        exit 1
    fi
    
    echo "✅ Contenedor iniciado"
    sleep 3
fi

echo ""
echo "[2/2] Mostrando logs en tiempo real..."
echo "Presiona Ctrl+C para salir de los logs (el servidor seguirá corriendo)"
echo ""
sleep 2

sudo docker-compose logs -f
