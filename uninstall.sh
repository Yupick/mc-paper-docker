#!/bin/bash

echo "========================================="
echo "Desinstalación del servidor Minecraft Paper"
echo "========================================="
echo ""

read -p "¿Seguro que deseas desinstalar el servidor Minecraft (s/n)? " confirm

if [[ ! "$confirm" =~ ^[sS]$ ]]; then
    echo ""
    echo "❌ Desinstalación cancelada"
    exit 0
fi

echo ""
echo "========================================="
echo "Iniciando desinstalación..."
echo "========================================="

echo ""
echo "[1/3] Deteniendo y eliminando contenedor..."
sudo docker-compose down 2>/dev/null || echo "⚠️  No hay contenedores corriendo"

echo ""
echo "[2/3] Eliminando imagen Docker..."
sudo docker rmi mc-paper_minecraft 2>/dev/null || echo "⚠️  Imagen no encontrada"

echo ""
echo "[3/3] Limpiando sistema Docker..."
sudo docker system prune -f > /dev/null 2>&1

echo ""
read -p "¿Deseas eliminar también los datos del mundo y configuraciones (s/n)? " delete_data

if [[ "$delete_data" =~ ^[sS]$ ]]; then
    echo ""
    echo "Eliminando carpetas: worlds/, plugins/, resourcepacks/, logs/, config/"
    sudo rm -rf worlds plugins resourcepacks logs config plugins_backup
    echo "✅ Datos eliminados"
else
    echo ""
    echo "⏩ Datos del servidor conservados"
fi

echo ""
echo "========================================="
echo "✅ Desinstalación completada"
echo "========================================="
echo ""

if [[ ! "$delete_data" =~ ^[sS]$ ]]; then
    echo "Los datos del servidor se mantienen en las carpetas:"
    echo "  - ./worlds/ (mundos y datos del servidor)"
    echo "  - ./plugins/"
    echo "  - ./resourcepacks/"
    echo "  - ./logs/"
    echo "  - ./config/server.properties"
    echo ""
fi

echo "Para reinstalar el servidor, ejecuta: ./create.sh"
