#!/bin/bash

echo "========================================="
echo "Migración de datos a nueva estructura"
echo "========================================="
echo ""
echo "Este script migra los datos de la estructura antigua a la nueva:"
echo "  - data/ -> worlds/"
echo "  - Elimina themes/ (ya no se usa)"
echo ""

read -p "¿Deseas continuar con la migración? (s/n): " confirm

if [[ ! "$confirm" =~ ^[sS]$ ]]; then
    echo "❌ Migración cancelada"
    exit 0
fi

echo ""
echo "[1/4] Deteniendo contenedor..."
sudo docker-compose down

if [ $? -ne 0 ]; then
    echo "⚠️  Advertencia: Problema al detener el contenedor"
    echo "Puede que necesites reiniciar el sistema: sudo reboot"
    read -p "¿Deseas continuar de todas formas? (s/n): " force
    if [[ ! "$force" =~ ^[sS]$ ]]; then
        exit 1
    fi
fi

echo ""
echo "[2/4] Creando carpetas necesarias..."
mkdir -p worlds logs

echo ""
echo "[3/4] Migrando datos..."

# Si existe data, moverla a worlds
if [ -d "data" ]; then
    echo "  → Moviendo contenido de data/ a worlds/"
    sudo cp -r data/* worlds/ 2>/dev/null || cp -r data/* worlds/ 2>/dev/null
    echo "  ✅ Datos migrados"
fi

# Eliminar carpetas antiguas
echo ""
echo "[4/4] Limpiando carpetas antiguas..."
if [ -d "themes" ]; then
    sudo rm -rf themes
    echo "  ✅ Carpeta themes/ eliminada"
fi

if [ -d "data" ]; then
    sudo rm -rf data
    echo "  ✅ Carpeta data/ eliminada"
fi

echo ""
echo "========================================="
echo "✅ Migración completada"
echo "========================================="
echo ""
echo "Estructura nueva:"
echo "  - ./worlds/ (mundos: world, world_nether, world_the_end)"
echo "  - ./plugins/ (plugins)"
echo "  - ./resourcepacks/ (texturas)"
echo "  - ./logs/ (logs del servidor)"
echo "  - ./config/server.properties (configuración)"
echo ""
echo "Ahora puedes iniciar el servidor con: ./create.sh"
echo "O si ya está creado: sudo docker-compose up -d"
