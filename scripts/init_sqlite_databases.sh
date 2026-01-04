#!/bin/bash

################################################################################
# init_sqlite_databases.sh
# 
# Inicializa las bases de datos SQLite para la migración MMORPG
# - Verifica que universal.db existe (creado por plugin)
# - Crea BD de mundos si no existen
# - Carga templates iniciales
# 
# Uso: ./init_sqlite_databases.sh [world_slug]
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
# Usar el volumen host mapeado al contenedor: config/ → /server/plugins/MMORPGPlugin
PLUGIN_DATA_DIR="$PROJECT_ROOT/config/data"
CONFIG_TEMPLATES_DIR="$PROJECT_ROOT/config/templates"
WORLDS_DIR="$PROJECT_ROOT/worlds"

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "  🗄️  INICIALIZADOR DE BASES DE DATOS SQLite - MMORPG"
echo "═══════════════════════════════════════════════════════════════════════════════"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

################################################################################
# Funciones
################################################################################

check_universal_db() {
    echo -e "\n${BLUE}[1/5] Verificando universal.db...${NC}"
    
    if [ ! -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        echo -e "${RED}❌ ERROR: universal.db no encontrado en $PLUGIN_DATA_DIR${NC}"
        echo "   El plugin debe crear este archivo en onEnable (primera ejecución)"
        exit 1
    fi
    
    echo -e "${GREEN}✅ universal.db encontrado${NC}"
    
    # Verificar tablas
    local tables=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" ".tables" 2>/dev/null | wc -w)
    echo "   📊 Tablas en universal.db: $tables"
}

check_templates() {
    echo -e "\n${BLUE}[2/5] Verificando templates...${NC}"
    
    if [ ! -d "$CONFIG_TEMPLATES_DIR" ]; then
        echo -e "${RED}❌ ERROR: Directorio templates no encontrado${NC}"
        exit 1
    fi
    
    local template_count=$(ls -1 "$CONFIG_TEMPLATES_DIR"/*.json 2>/dev/null | wc -l)
    echo -e "${GREEN}✅ Templates encontrados: $template_count${NC}"
    
    # Listar templates
    echo "   Template files:"
    ls -1 "$CONFIG_TEMPLATES_DIR"/*.json 2>/dev/null | while read -r file; do
        local name=$(basename "$file")
        echo "     • $name"
    done
}

create_world_database() {
    local world_slug=$1
    local world_db_dir="$WORLDS_DIR/$world_slug/data"
    local world_db_file="$world_db_dir/${world_slug}.db"
    
    echo -e "\n${BLUE}[3/5] Creando base de datos para mundo: $world_slug${NC}"
    
    if [ -f "$world_db_file" ]; then
        echo -e "${YELLOW}⚠️  $world_db_file ya existe${NC}"
        return 0
    fi
    
    # Crear directorio
    mkdir -p "$world_db_dir"
    
    # Crear BD con esquema
    echo "   📁 Directorio: $world_db_dir"
    
    # Crear tablas básicas de mundo
    sqlite3 "$world_db_file" << 'EOF'
CREATE TABLE IF NOT EXISTS players (
    uuid TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    class_type TEXT,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    health REAL,
    max_health REAL,
    mana REAL,
    max_mana REAL,
    skill_points INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    last_login INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

CREATE TABLE IF NOT EXISTS quests (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    difficulty TEXT,
    min_level INTEGER,
    npc_giver_id TEXT,
    exp_reward INTEGER,
    money_reward REAL,
    skill_points_reward INTEGER,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

CREATE TABLE IF NOT EXISTS npcs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    yaw REAL,
    pitch REAL,
    quest_id TEXT,
    dialogue TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

CREATE TABLE IF NOT EXISTS spawns (
    id TEXT PRIMARY KEY,
    mob_id TEXT NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    radius REAL DEFAULT 10,
    respawn_time INTEGER DEFAULT 300,
    max_spawns INTEGER DEFAULT 1,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

CREATE TABLE IF NOT EXISTS squads (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    leader_uuid TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    max_members INTEGER DEFAULT 10,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

CREATE INDEX IF NOT EXISTS idx_players_class ON players(class_type);
CREATE INDEX IF NOT EXISTS idx_players_level ON players(level);
CREATE INDEX IF NOT EXISTS idx_quests_difficulty ON quests(difficulty);
CREATE INDEX IF NOT EXISTS idx_npcs_world ON npcs(world);
CREATE INDEX IF NOT EXISTS idx_spawns_world ON spawns(world);
EOF
    
    echo -e "${GREEN}✅ BD de mundo creada: $world_slug${NC}"
    
    # Verificar tablas
    local table_count=$(sqlite3 "$world_db_file" ".tables" | wc -w)
    echo "   📊 Tablas creadas: $table_count"
}

generate_world_report() {
    echo -e "\n${BLUE}[4/5] Generando reporte de mundos...${NC}"
    
    if [ ! -d "$WORLDS_DIR" ]; then
        echo -e "${YELLOW}⚠️  No hay directorio worlds${NC}"
        return
    fi
    
    echo "   Mundos detectados:"
    for world_dir in "$WORLDS_DIR"/*/; do
        if [ -d "$world_dir" ]; then
            local world_slug=$(basename "$world_dir")
            local db_file="$world_dir/data/${world_slug}.db"
            
            if [ -f "$db_file" ]; then
                local size=$(du -h "$db_file" | cut -f1)
                echo "     ✅ $world_slug (${size})"
            else
                echo "     ⏳ $world_slug (BD no creada)"
            fi
        fi
    done
}

display_summary() {
    echo -e "\n${BLUE}[5/5] Resumen de inicialización${NC}"
    
    echo -e "\n${GREEN}=== UNIVERSAL.DB ===${NC}"
    echo "   Ubicación: $PLUGIN_DATA_DIR/universal.db"
    
    if [ -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        local size=$(du -h "$PLUGIN_DATA_DIR/universal.db" | cut -f1)
        echo "   Tamaño: $size"
        
        if command -v sqlite3 >/dev/null 2>&1; then
            echo "   Contenido:"
            sqlite3 "$PLUGIN_DATA_DIR/universal.db" << 'EOF'
.mode column
.headers on
SELECT 'items' as tabla, COUNT(*) as registros FROM items
UNION ALL
SELECT 'mobs', COUNT(*) FROM mobs
UNION ALL
SELECT 'enchantments', COUNT(*) FROM enchantments
UNION ALL
SELECT 'crafting_recipes', COUNT(*) FROM crafting_recipes
UNION ALL
SELECT 'achievements_def', COUNT(*) FROM achievements_def
ORDER BY tabla;
EOF
        else
            echo "   Omitido resumen detallado: instala sqlite3 para mostrar conteos."
        fi
    fi
    
    echo -e "\n${GREEN}=== TEMPLATES ===${NC}"
    echo "   Ubicación: $CONFIG_TEMPLATES_DIR"
    local template_size=$(du -sh "$CONFIG_TEMPLATES_DIR" 2>/dev/null | cut -f1 || echo "0")
    echo "   Tamaño total: $template_size"
}

################################################################################
# Main
################################################################################

main() {
    local world_slug=${1:-""}
    
    # Paso 1: Verificar universal.db
    check_universal_db
    
    # Paso 2: Verificar templates
    check_templates
    
    # Paso 3: Crear BD de mundos
    if [ -n "$world_slug" ]; then
        create_world_database "$world_slug"
    else
        # Si no se especifica mundo, crear para todos los mundos existentes
        if [ -d "$WORLDS_DIR" ]; then
            for world_dir in "$WORLDS_DIR"/*/; do
                if [ -d "$world_dir" ]; then
                    local world=$(basename "$world_dir")
                    create_world_database "$world"
                fi
            done
        fi
    fi
    
    # Paso 4: Reporte de mundos
    generate_world_report
    
    # Paso 5: Resumen
    display_summary
    
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ INICIALIZACIÓN COMPLETADA${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
}

main "$@"
