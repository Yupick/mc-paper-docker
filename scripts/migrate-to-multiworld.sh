#!/bin/bash

set -e

echo "========================================="
echo "Migración a Sistema Multi-Mundos"
echo "========================================="
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Rutas
WORLDS_DIR="./worlds"
WORLD_DEFAULT_DIR="$WORLDS_DIR/world-default"
ACTIVE_LINK="$WORLDS_DIR/active"
BACKUP_DIR="./backups/migration-$(date +%Y%m%d_%H%M%S)"
CONFIG_DIR="./config"

# Verificar que no esté corriendo el servidor
if docker ps --filter "name=minecraft-paper" --filter "status=running" | grep -q minecraft-paper; then
    echo -e "${RED}❌ Error: El servidor está corriendo${NC}"
    echo "Por favor, detén el servidor primero: ./stop.sh"
    exit 1
fi

# Función para crear backup de seguridad
create_backup() {
    echo "[1/6] Creando backup de seguridad..."
    mkdir -p "$BACKUP_DIR"
    
    if [ -d "$WORLDS_DIR" ]; then
        cp -r "$WORLDS_DIR" "$BACKUP_DIR/worlds_backup"
        echo -e "${GREEN}✅ Backup creado en: $BACKUP_DIR${NC}"
    fi
}

# Función para crear estructura de directorios
create_structure() {
    echo ""
    echo "[2/6] Creando estructura de directorios..."
    
    # Crear directorio principal de mundos si no existe
    mkdir -p "$WORLDS_DIR"
    
    # Crear directorio para templates
    mkdir -p "$WORLDS_DIR/templates"
    
    echo -e "${GREEN}✅ Estructura creada${NC}"
}

# Función para mover mundo actual
migrate_current_world() {
    echo ""
    echo "[3/6] Migrando mundo actual..."
    
    # Si existe un mundo en ./worlds/, moverlo a world-default
    if [ -d "$WORLDS_DIR/world" ] || [ -d "$WORLDS_DIR/world_nether" ] || [ -d "$WORLDS_DIR/world_the_end" ]; then
        mkdir -p "$WORLD_DEFAULT_DIR"
        
        # Mover dimensiones
        [ -d "$WORLDS_DIR/world" ] && mv "$WORLDS_DIR/world" "$WORLD_DEFAULT_DIR/"
        [ -d "$WORLDS_DIR/world_nether" ] && mv "$WORLDS_DIR/world_nether" "$WORLD_DEFAULT_DIR/"
        [ -d "$WORLDS_DIR/world_the_end" ] && mv "$WORLDS_DIR/world_the_end" "$WORLD_DEFAULT_DIR/"
        
        # Copiar server.properties del mundo
        if [ -f "$CONFIG_DIR/server.properties" ]; then
            cp "$CONFIG_DIR/server.properties" "$WORLD_DEFAULT_DIR/server.properties"
        fi
        
        echo -e "${GREEN}✅ Mundo migrado a: $WORLD_DEFAULT_DIR${NC}"
    else
        # Si no hay mundo, crear uno nuevo vacío
        echo -e "${YELLOW}⚠️  No se encontró mundo existente, creando mundo-default vacío${NC}"
        mkdir -p "$WORLD_DEFAULT_DIR"
        
        # Copiar server.properties base
        if [ -f "$CONFIG_DIR/server.properties" ]; then
            cp "$CONFIG_DIR/server.properties" "$WORLD_DEFAULT_DIR/server.properties"
        fi
    fi
}

# Función para crear metadata.json
create_metadata() {
    echo ""
    echo "[4/6] Generando metadata del mundo..."
    
    # Calcular tamaño del mundo (en MB)
    WORLD_SIZE=0
    if [ -d "$WORLD_DEFAULT_DIR/world" ]; then
        WORLD_SIZE=$(du -sm "$WORLD_DEFAULT_DIR" | cut -f1)
    fi
    
    # Leer configuración de server.properties si existe
    GAMEMODE="survival"
    DIFFICULTY="normal"
    SEED=""
    
    if [ -f "$WORLD_DEFAULT_DIR/server.properties" ]; then
        GAMEMODE=$(grep "^gamemode=" "$WORLD_DEFAULT_DIR/server.properties" | cut -d'=' -f2 | tr -d '\r' || echo "survival")
        DIFFICULTY=$(grep "^difficulty=" "$WORLD_DEFAULT_DIR/server.properties" | cut -d'=' -f2 | tr -d '\r' || echo "normal")
        SEED=$(grep "^level-seed=" "$WORLD_DEFAULT_DIR/server.properties" | cut -d'=' -f2 | tr -d '\r' || echo "")
    fi
    
    # Crear metadata.json
    cat > "$WORLD_DEFAULT_DIR/metadata.json" << EOF
{
  "name": "Mundo Principal",
  "slug": "world-default",
  "description": "Mundo migrado desde instalación anterior",
  "gamemode": "$GAMEMODE",
  "difficulty": "$DIFFICULTY",
  "created_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "last_played": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "size_mb": $WORLD_SIZE,
  "seed": "$SEED",
  "version": "1.21.1",
  "spawn": {
    "x": 0,
    "y": 64,
    "z": 0
  },
  "settings": {
    "pvp": true,
    "spawn_monsters": true,
    "spawn_animals": true,
    "view_distance": 10,
    "max_players": 20
  },
  "tags": ["principal", "migrado"]
}
EOF
    
    echo -e "${GREEN}✅ Metadata creado${NC}"
}

# Función para crear symlink
create_symlink() {
    echo ""
    echo "[5/6] Creando symlink al mundo activo..."
    
    # Eliminar symlink si ya existe
    [ -L "$ACTIVE_LINK" ] && rm "$ACTIVE_LINK"
    
    # Crear symlink relativo
    cd "$WORLDS_DIR"
    ln -s "world-default" "active"
    cd - > /dev/null
    
    echo -e "${GREEN}✅ Symlink creado: $ACTIVE_LINK -> world-default${NC}"
}

# Función para crear worlds.json
create_worlds_config() {
    echo ""
    echo "[6/6] Creando configuración de mundos..."
    
    cat > "$WORLDS_DIR/worlds.json" << EOF
{
  "active_world": "world-default",
  "worlds": [
    {
      "slug": "world-default",
      "status": "active",
      "auto_backup": true,
      "backup_interval": "6h"
    }
  ],
  "settings": {
    "max_worlds": 10,
    "auto_backup_before_switch": true,
    "keep_backups": 5
  }
}
EOF
    
    echo -e "${GREEN}✅ Configuración creada: $WORLDS_DIR/worlds.json${NC}"
}

# Función para mostrar resumen
show_summary() {
    echo ""
    echo "========================================="
    echo -e "${GREEN}✅ Migración completada exitosamente${NC}"
    echo "========================================="
    echo ""
    echo "Estructura creada:"
    echo "  worlds/"
    echo "  ├── active -> world-default (symlink)"
    echo "  ├── world-default/"
    echo "  │   ├── world/"
    echo "  │   ├── world_nether/"
    echo "  │   ├── world_the_end/"
    echo "  │   ├── server.properties"
    echo "  │   └── metadata.json"
    echo "  ├── templates/"
    echo "  └── worlds.json"
    echo ""
    echo "Backup guardado en: $BACKUP_DIR"
    echo ""
    echo "Próximos pasos:"
    echo "  1. Verifica la estructura: ls -la worlds/"
    echo "  2. Inicia el servidor: ./run.sh"
    echo "  3. Verifica que el mundo cargue correctamente"
    echo ""
    echo "Para revertir la migración:"
    echo "  ./rollback-multiworld.sh"
    echo ""
}

# Ejecutar migración
main() {
    create_backup
    create_structure
    migrate_current_world
    create_metadata
    create_symlink
    create_worlds_config
    show_summary
}

main
