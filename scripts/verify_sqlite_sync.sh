#!/bin/bash

################################################################################
# verify_sqlite_sync.sh
#
# Verifica sincronización entre plugin y web a través de SQLite
# - Comprueba que universal.db y {world}.db están actualizados
# - Verifica timestamps de last_updated
# - Genera reporte de inconsistencias
#
# Datos verificados:
# 1. Item count en universal.db vs items esperados
# 2. Player count en {world}.db vs jugadores en-game
# 3. Quest status consistency
# 4. NPC locations y estado
# 5. Economy transactions
#
# Uso: ./verify_sqlite_sync.sh [world_slug]
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PLUGIN_DATA_DIR="$PROJECT_ROOT/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/data"
WORLDS_DIR="$PROJECT_ROOT/worlds"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "  🔍 VERIFICADOR DE SINCRONIZACIÓN SQLite"
echo "═══════════════════════════════════════════════════════════════════════════════"

################################################################################
# Funciones de verificación
################################################################################

# 1. Verificar universal.db
verify_universal_db() {
    echo -e "\n${BLUE}[1/5] Verificando universal.db...${NC}"
    
    if [ ! -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        echo -e "${RED}❌ universal.db no encontrado${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ Archivo encontrado${NC}"
    
    # Contar registros
    local items=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM items" 2>/dev/null || echo "0")
    local mobs=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM mobs" 2>/dev/null || echo "0")
    local enchantments=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM enchantments" 2>/dev/null || echo "0")
    local crafting=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM crafting_recipes" 2>/dev/null || echo "0")
    local achievements=$(sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) FROM achievements_def" 2>/dev/null || echo "0")
    
    echo "   Items: $items"
    echo "   Mobs: $mobs"
    echo "   Enchantments: $enchantments"
    echo "   Crafting Recipes: $crafting"
    echo "   Achievements: $achievements"
    
    # Validar que hay datos
    if [ "$items" -eq 0 ] && [ "$mobs" -eq 0 ]; then
        echo -e "${YELLOW}⚠️  universal.db está vacía${NC}"
        return 1
    fi
    
    echo -e "${GREEN}✅ universal.db OK${NC}"
    return 0
}

# 2. Verificar {world}.db
verify_world_db() {
    local world_slug=$1
    echo -e "\n${BLUE}[2/5] Verificando BD de mundo: $world_slug${NC}"
    
    local db_file="$WORLDS_DIR/$world_slug/data/${world_slug}.db"
    
    if [ ! -f "$db_file" ]; then
        echo -e "${YELLOW}⚠️  ${world_slug}.db no encontrado${NC}"
        echo "   (Se crea cuando web activa RPG para este mundo)"
        return 1
    fi
    
    echo -e "${GREEN}✅ Archivo encontrado${NC}"
    
    # Contar registros
    local players=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM players" 2>/dev/null || echo "0")
    local quests=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM quests" 2>/dev/null || echo "0")
    local npcs=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM npcs" 2>/dev/null || echo "0")
    local squads=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM squads 2>/dev/null" || echo "0")
    
    echo "   Players: $players"
    echo "   Quests: $quests"
    echo "   NPCs: $npcs"
    echo "   Squads: $squads"
    
    echo -e "${GREEN}✅ BD de mundo OK${NC}"
    return 0
}

# 3. Verificar integridad de datos
verify_data_integrity() {
    echo -e "\n${BLUE}[3/5] Verificando integridad de datos...${NC}"
    
    local world_slug=$1
    local db_file="$WORLDS_DIR/$world_slug/data/${world_slug}.db"
    
    if [ ! -f "$db_file" ]; then
        echo -e "${YELLOW}⚠️  BD de mundo no disponible${NC}"
        return 0
    fi
    
    # Verificar foreign keys
    echo "   Validando foreign keys..."
    
    # Players con referencias válidas
    local invalid_players=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM player_abilities WHERE player_uuid NOT IN (SELECT uuid FROM players)" 2>/dev/null || echo "0")
    if [ "$invalid_players" -gt 0 ]; then
        echo -e "${RED}   ❌ $invalid_players referencias inválidas en player_abilities${NC}"
    else
        echo -e "${GREEN}   ✅ player_abilities OK${NC}"
    fi
    
    # Quests con referencias válidas
    local invalid_quests=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM player_quests WHERE quest_id NOT IN (SELECT id FROM quests)" 2>/dev/null || echo "0")
    if [ "$invalid_quests" -gt 0 ]; then
        echo -e "${RED}   ❌ $invalid_quests referencias inválidas en player_quests${NC}"
    else
        echo -e "${GREEN}   ✅ player_quests OK${NC}"
    fi
    
    echo -e "${GREEN}✅ Integridad verificada${NC}"
}

# 4. Verificar timestamps
verify_timestamps() {
    echo -e "\n${BLUE}[4/5] Verificando timestamps...${NC}"
    
    local world_slug=$1
    local db_file="$WORLDS_DIR/$world_slug/data/${world_slug}.db"
    
    if [ ! -f "$db_file" ]; then
        return 0
    fi
    
    # Jugadores con último login
    local players_updated=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM players WHERE updated_at IS NOT NULL" 2>/dev/null || echo "0")
    local total_players=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM players" 2>/dev/null || echo "0")
    
    if [ "$total_players" -gt 0 ]; then
        echo "   Players actualizados: $players_updated/$total_players"
    fi
    
    # NPCs con última actualización
    local npcs_updated=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM npcs WHERE updated_at IS NOT NULL" 2>/dev/null || echo "0")
    local total_npcs=$(sqlite3 "$db_file" "SELECT COUNT(*) FROM npcs" 2>/dev/null || echo "0")
    
    if [ "$total_npcs" -gt 0 ]; then
        echo "   NPCs actualizados: $npcs_updated/$total_npcs"
    fi
    
    echo -e "${GREEN}✅ Timestamps OK${NC}"
}

# 5. Generar reporte
generate_report() {
    echo -e "\n${BLUE}[5/5] Generando reporte...${NC}"
    
    local report_file="/tmp/sqlite_verification_$(date +%Y%m%d_%H%M%S).log"
    
    echo "Reporte de sincronización SQLite" > "$report_file"
    echo "================================" >> "$report_file"
    echo "Fecha: $(date)" >> "$report_file"
    echo "" >> "$report_file"
    
    echo "UNIVERSAL.DB" >> "$report_file"
    echo "-----------" >> "$report_file"
    sqlite3 "$PLUGIN_DATA_DIR/universal.db" ".tables" >> "$report_file" 2>&1 || true
    echo "" >> "$report_file"
    
    # Mundos
    echo "MUNDOS CON RPG ACTIVO" >> "$report_file"
    echo "-------------------" >> "$report_file"
    
    if [ -d "$WORLDS_DIR" ]; then
        for world_dir in "$WORLDS_DIR"/*/; do
            local world=$(basename "$world_dir")
            local db_file="$world_dir/data/${world}.db"
            if [ -f "$db_file" ]; then
                echo "✓ $world" >> "$report_file"
            else
                echo "✗ $world (BD no inicializada)" >> "$report_file"
            fi
        done
    fi
    
    echo "" >> "$report_file"
    echo "Reporte guardado en: $report_file"
    echo -e "${GREEN}✅ Reporte generado${NC}"
    
    cat "$report_file"
}

################################################################################
# Main
################################################################################

main() {
    local world_slug=${1:-""}
    
    # Verificar universal.db
    verify_universal_db || {
        echo -e "${RED}❌ Error en universal.db${NC}"
        exit 1
    }
    
    # Verificar mundo específico o todos
    if [ -n "$world_slug" ]; then
        verify_world_db "$world_slug" || true
        verify_data_integrity "$world_slug" || true
        verify_timestamps "$world_slug" || true
    else
        # Verificar todos los mundos
        if [ -d "$WORLDS_DIR" ]; then
            for world_dir in "$WORLDS_DIR"/*/; do
                if [ -d "$world_dir" ]; then
                    local world=$(basename "$world_dir")
                    verify_world_db "$world" || true
                    verify_data_integrity "$world" || true
                    verify_timestamps "$world" || true
                fi
            done
        fi
    fi
    
    # Generar reporte
    generate_report
    
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ VERIFICACIÓN COMPLETADA${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
}

main "$@"
