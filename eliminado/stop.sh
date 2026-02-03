#!/bin/bash

echo "========================================="
echo "Deteniendo servidor Minecraft Paper"
echo "========================================="

echo ""
echo "Deteniendo contenedor..."
sudo docker-compose stop

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "✅ Servidor detenido correctamente"
    echo "========================================="
    echo ""
    echo "Para iniciar nuevamente: ./run.sh"
    echo "Para eliminar completamente: sudo docker-compose down"
else
    echo "❌ Error al detener el contenedor"
    exit 1
fi
