#!/bin/bash

set -e

echo "========================================="
echo "Revertir Migración Multi-Mundos"
echo "========================================="
echo ""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Buscar el backup más reciente
LATEST_BACKUP=$(ls -td ./backups/migration-* 2>/dev/null | head -1)

if [ -z "$LATEST_BACKUP" ]; then
    echo -e "${RED}❌ No se encontró backup de migración${NC}"
    echo "No se puede revertir automáticamente."
    exit 1
fi

echo -e "${YELLOW}Se encontró backup: $LATEST_BACKUP${NC}"
echo ""
read -p "¿Deseas revertir a este backup? (y/n): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operación cancelada"
    exit 0
fi

# Verificar que no esté corriendo el servidor
if docker ps --filter "name=minecraft-paper" --filter "status=running" | grep -q minecraft-paper; then
    echo -e "${RED}❌ Error: El servidor está corriendo${NC}"
    echo "Por favor, detén el servidor primero: ./stop.sh"
    exit 1
fi

echo "[1/3] Eliminando estructura multi-mundos..."
rm -rf ./worlds

echo "[2/3] Restaurando backup..."
cp -r "$LATEST_BACKUP/worlds_backup" ./worlds

echo "[3/3] Restaurando docker-compose.yml..."
# Aquí podrías restaurar la versión anterior de docker-compose.yml
# Por ahora solo mostramos un aviso

echo ""
echo -e "${GREEN}✅ Rollback completado${NC}"
echo ""
echo "IMPORTANTE: Revierte manualmente docker-compose.yml a la versión anterior"
echo "o ejecuta: git checkout docker-compose.yml"
echo ""
