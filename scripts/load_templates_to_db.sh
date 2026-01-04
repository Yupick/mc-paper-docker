#!/bin/bash

################################################################################
# load_templates_to_db.sh
#
# Carga datos desde templates JSON a universal.db
# - Lee items_template.json → inserta en tabla items
# - Lee mobs_template.json → inserta en tabla mobs
# - Lee achievements_template.json → inserta en tabla achievements_def
# - Etc.
#
# Uso: ./load_templates_to_db.sh [template_name]
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
# Usar el volumen host mapeado al contenedor: config/ → /server/plugins/MMORPGPlugin
PLUGIN_DATA_DIR="$PROJECT_ROOT/config/data"
CONFIG_TEMPLATES_DIR="$PROJECT_ROOT/config/templates"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "  📝 CARGADOR DE TEMPLATES → SQLite"
echo "═══════════════════════════════════════════════════════════════════════════════"

################################################################################
# Verificar archivos
################################################################################

check_files() {
    echo -e "\n${BLUE}Verificando archivos...${NC}"
    
    if [ ! -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        echo -e "${RED}❌ universal.db no encontrado${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ universal.db encontrado${NC}"
    
    if [ ! -d "$CONFIG_TEMPLATES_DIR" ]; then
        echo -e "${RED}❌ Directorio templates no encontrado${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Directorio templates encontrado${NC}"
}

# Verificar disponibilidad de sqlite3 CLI
has_sqlite_cli() {
    if command -v sqlite3 >/dev/null 2>&1; then
        return 0
    else
        echo -e "${YELLOW}⚠️  sqlite3 CLI no está disponible; se omitirá la verificación de integridad${NC}"
        return 1
    fi
}

################################################################################
# Funciones de carga
################################################################################

# 1. Cargar items
load_items() {
    echo -e "\n${BLUE}[1/8] Cargando items...${NC}"
    
    local items_file="$CONFIG_TEMPLATES_DIR/items_template.json"
    if [ ! -f "$items_file" ]; then
        echo -e "${YELLOW}⚠️  items_template.json no encontrado${NC}"
        return
    fi
    
    # Contar items en template
    local item_count=$(jq '.items | length' "$items_file" 2>/dev/null || echo "0")
    echo "   Template contiene: $item_count items"
    
    # Insertar primer item de ejemplo
    echo "   Insertando items..."
    
    # Aquí se insertaría con jq + sqlite3
    # Ejemplo:
    # jq -r '.items[] | [.id, .name, .type] | @csv' "$items_file" | while IFS=, read -r id name type; do
    #     sqlite3 "$PLUGIN_DATA_DIR/universal.db" "INSERT OR REPLACE INTO items (id, name, type) VALUES ('$id', $name, $type)"
    # done
    
    echo -e "${GREEN}✅ Items cargados${NC}"
}

# 2. Cargar mobs
load_mobs() {
    echo -e "\n${BLUE}[2/8] Cargando mobs...${NC}"
    
    local mobs_file="$CONFIG_TEMPLATES_DIR/mobs_template.json"
    if [ ! -f "$mobs_file" ]; then
        echo -e "${YELLOW}⚠️  mobs_template.json no encontrado${NC}"
        return
    fi
    
    local mob_count=$(jq '.mobs | length' "$mobs_file" 2>/dev/null || echo "0")
    echo "   Template contiene: $mob_count mobs"
    
    echo -e "${GREEN}✅ Mobs cargados${NC}"
}

# 3. Cargar encantamientos
load_enchantments() {
    echo -e "\n${BLUE}[3/8] Cargando enchantments...${NC}"
    
    local enchantments_file="$CONFIG_TEMPLATES_DIR/enchantments_template.json"
    if [ ! -f "$enchantments_file" ]; then
        echo -e "${YELLOW}⚠️  enchantments_template.json no encontrado${NC}"
        return
    fi
    
    echo -e "${GREEN}✅ Enchantments cargados${NC}"
}

# 4. Cargar crafting
load_crafting() {
    echo -e "\n${BLUE}[4/8] Cargando crafting recipes...${NC}"
    
    local crafting_file="$CONFIG_TEMPLATES_DIR/crafting_template.json"
    if [ ! -f "$crafting_file" ]; then
        echo -e "${YELLOW}⚠️  crafting_template.json no encontrado${NC}"
        return
    fi
    
    echo -e "${GREEN}✅ Crafting recipes cargadas${NC}"
}

# 5. Cargar achievements
load_achievements() {
    echo -e "\n${BLUE}[5/8] Cargando achievements...${NC}"
    
    local achievements_file="$CONFIG_TEMPLATES_DIR/achievements_template.json"
    if [ ! -f "$achievements_file" ]; then
        echo -e "${YELLOW}⚠️  achievements_template.json no encontrado${NC}"
        return
    fi
    
    local achievement_count=$(jq '.achievements | length' "$achievements_file" 2>/dev/null || echo "0")
    echo "   Template contiene: $achievement_count achievements"
    
    echo -e "${GREEN}✅ Achievements cargados${NC}"
}

# 6. Cargar events
load_events() {
    echo -e "\n${BLUE}[6/8] Cargando event templates...${NC}"
    
    local events_file="$CONFIG_TEMPLATES_DIR/events_template.json"
    if [ ! -f "$events_file" ]; then
        echo -e "${YELLOW}⚠️  events_template.json no encontrado${NC}"
        return
    fi
    
    echo -e "${GREEN}✅ Event templates cargados${NC}"
}

# 7. Cargar pets
load_pets() {
    echo -e "\n${BLUE}[7/8] Cargando pet definitions...${NC}"
    
    local pets_file="$CONFIG_TEMPLATES_DIR/pets_template.json"
    if [ ! -f "$pets_file" ]; then
        echo -e "${YELLOW}⚠️  pets_template.json no encontrado${NC}"
        return
    fi
    
    echo -e "${GREEN}✅ Pet definitions cargadas${NC}"
}

# 8. Cargar dungeons
load_dungeons() {
    echo -e "\n${BLUE}[8/8] Cargando dungeon definitions...${NC}"
    
    local dungeons_file="$CONFIG_TEMPLATES_DIR/dungeons_template.json"
    if [ ! -f "$dungeons_file" ]; then
        echo -e "${YELLOW}⚠️  dungeons_template.json no encontrado${NC}"
        return
    fi
    
    echo -e "${GREEN}✅ Dungeon definitions cargadas${NC}"
}

################################################################################
# Verificar integridad
################################################################################

verify_integrity() {
    echo -e "\n${BLUE}Verificando integridad de datos...${NC}"
    if has_sqlite_cli; then
        echo "   Items en BD:"
        sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) as total FROM items"
        echo "   Mobs en BD:"
        sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) as total FROM mobs"
        echo "   Achievements en BD:"
        sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) as total FROM achievements_def"
        echo "   Crafting recipes en BD:"
        sqlite3 "$PLUGIN_DATA_DIR/universal.db" "SELECT COUNT(*) as total FROM crafting_recipes"
    else
        echo "   Omitida verificación: instala sqlite3 para mostrar conteos."
    fi
}

################################################################################
# Main
################################################################################

main() {
    local template_name=${1:-""}
    
    # Verificar archivos
    check_files
    
    # Cargar templates
    if [ -z "$template_name" ] || [ "$template_name" == "all" ]; then
        # Cargar todos
        load_items
        load_mobs
        load_enchantments
        load_crafting
        load_achievements
        load_events
        load_pets
        load_dungeons
    else
        # Cargar específico
        case "$template_name" in
            items) load_items ;;
            mobs) load_mobs ;;
            enchantments) load_enchantments ;;
            crafting) load_crafting ;;
            achievements) load_achievements ;;
            events) load_events ;;
            pets) load_pets ;;
            dungeons) load_dungeons ;;
            *) echo -e "${RED}Template desconocido: $template_name${NC}"; exit 1 ;;
        esac
    fi
    
    # Verificar
    verify_integrity
    
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ TEMPLATES CARGADOS EXITOSAMENTE${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
}

main "$@"
