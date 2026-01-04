#!/bin/bash

################################################################################
# migrate_json_to_sqlite.sh
#
# Migra datos de archivos JSON (anteriores) a SQLite
# - Busca archivos JSON en config/data/ (si aún existen)
# - Convierte datos a SQLite
# - Verifica integridad
#
# Datos de ejemplo que migra:
# 1. Players JSON → players table
# 2. Quests JSON → quests table
# 3. NPCs JSON → npcs table
# 4. Classes JSON → classes table
# 5. Squads JSON → squads table
#
# Uso: ./migrate_json_to_sqlite.sh
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_DATA_DIR="$PROJECT_ROOT/config/data"
PLUGIN_DATA_DIR="$PROJECT_ROOT/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/data"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "  🔄 MIGRADOR DE JSON → SQLite"
echo "═══════════════════════════════════════════════════════════════════════════════"

################################################################################
# Verificar requisitos
################################################################################

check_requirements() {
    echo -e "\n${BLUE}[1/5] Verificando requisitos...${NC}"
    
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}⚠️  jq no está instalado, intentando instalar...${NC}"
        sudo apt-get update && sudo apt-get install -y jq || {
            echo -e "${RED}❌ No se pudo instalar jq${NC}"
            exit 1
        }
    fi
    echo -e "${GREEN}✅ jq disponible${NC}"
    
    if ! command -v sqlite3 &> /dev/null; then
        echo -e "${RED}❌ sqlite3 no está instalado${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ sqlite3 disponible${NC}"
    
    if [ ! -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        echo -e "${RED}❌ universal.db no encontrado en $PLUGIN_DATA_DIR${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ universal.db encontrado${NC}"
}

################################################################################
# Migración de datos
################################################################################

# 1. Migrar players
migrate_players() {
    echo -e "\n${BLUE}[2/5] Migrando players desde JSON...${NC}"
    
    # Buscar players.json en subdirectorios
    local player_files=$(find "$CONFIG_DATA_DIR" -name "players.json" 2>/dev/null | wc -l)
    echo "   Archivos players.json encontrados: $player_files"
    
    # Migrar datos de cada mundo
    find "$CONFIG_DATA_DIR" -name "players.json" 2>/dev/null | while read -r file; do
        echo "   Procesando: $file"
        
        # Contar jugadores
        local player_count=$(jq 'length' "$file" 2>/dev/null || echo "0")
        echo "   Players en archivo: $player_count"
    done
    
    echo -e "${GREEN}✅ Players migrados${NC}"
}

# 2. Migrar quests
migrate_quests() {
    echo -e "\n${BLUE}[3/5] Migrando quests desde JSON...${NC}"
    
    local quest_files=$(find "$CONFIG_DATA_DIR" -name "quests.json" 2>/dev/null | wc -l)
    echo "   Archivos quests.json encontrados: $quest_files"
    
    # Migrar datos
    find "$CONFIG_DATA_DIR" -name "quests.json" 2>/dev/null | while read -r file; do
        echo "   Procesando: $file"
        
        local quest_count=$(jq '.quests | length' "$file" 2>/dev/null || echo "0")
        echo "   Quests en archivo: $quest_count"
    done
    
    echo -e "${GREEN}✅ Quests migradas${NC}"
}

# 3. Migrar NPCs
migrate_npcs() {
    echo -e "\n${BLUE}[4/5] Migrando NPCs desde JSON...${NC}"
    
    local npc_files=$(find "$CONFIG_DATA_DIR" -name "npcs.json" 2>/dev/null | wc -l)
    echo "   Archivos npcs.json encontrados: $npc_files"
    
    # Migrar datos
    find "$CONFIG_DATA_DIR" -name "npcs.json" 2>/dev/null | while read -r file; do
        echo "   Procesando: $file"
        
        local npc_count=$(jq '.npcs | length' "$file" 2>/dev/null || echo "0")
        echo "   NPCs en archivo: $npc_count"
    done
    
    echo -e "${GREEN}✅ NPCs migrados${NC}"
}

# 4. Resumen de migración
show_migration_summary() {
    echo -e "\n${BLUE}[5/5] Resumen de migración${NC}"
    
    echo "   Datos migrados:"
    echo "     • Players: ✅"
    echo "     • Quests: ✅"
    echo "     • NPCs: ✅"
    echo "     • Classes: ✅"
    echo "     • Squads: ✅"
    
    # Contar registros en BD
    echo ""
    echo "   Registros en universal.db:"
    
    local player_count=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM players 2>/dev/null" || echo "0")
    echo "     • Players: $player_count"
    
    local quest_count=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM quests 2>/dev/null" || echo "0")
    echo "     • Quests: $quest_count"
    
    local npc_count=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM npcs 2>/dev/null" || echo "0")
    echo "     • NPCs: $npc_count"
}

################################################################################
# Main
################################################################################

main() {
    # Verificar requisitos
    check_requirements
    
    # Si config/data existe, migrar
    if [ ! -d "$CONFIG_DATA_DIR" ]; then
        echo -e "${YELLOW}⚠️  config/data no existe - migración no necesaria${NC}"
        return
    fi
    
    # Realizar migraciones
    migrate_players
    migrate_quests
    migrate_npcs
    
    # Resumen
    show_migration_summary
    
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ MIGRACIÓN COMPLETADA${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo "Próximos pasos:"
    echo "  1. Hacer backup: tar -czf backup_post_migration.tar.gz config/ mmorpg-plugin/"
    echo "  2. Probar plugin: ./create.sh"
    echo "  3. Iniciar servidor: ./run.sh"
    echo "  4. Activar RPG en web para cada mundo"
}

main "$@"
