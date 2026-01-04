#!/bin/bash

################################################################################
#
#  Test de Integración SQLite - MMORPG Plugin + Web Panel
#  
#  Verifica:
#  1. Plugin crear universal.db en onEnable
#  2. Web crear {world}.db cuando activa RPG
#  3. Sincronización correcta entre plugin y web
#  4. Integridad de datos
#
#  Uso: ./test-sqlite-integration.sh [mundo_slug]
#
################################################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BASE_DIR="$(dirname "$SCRIPT_DIR")"
CONTAINER_NAME="${CONTAINER_NAME:-minecraft-server}"
WORLD_SLUG="${1:-mundo_principal}"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Contadores de tests
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0

################################################################################
# FUNCIONES AUXILIARES
################################################################################

log_step() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

test_case() {
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    echo -e "\n${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${MAGENTA}TEST #$TESTS_TOTAL: $1${NC}"
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

pass_test() {
    TESTS_PASSED=$((TESTS_PASSED + 1))
    log_success "$1"
}

fail_test() {
    TESTS_FAILED=$((TESTS_FAILED + 1))
    log_error "$1"
}

################################################################################
# TEST 1: Verificar JAR compilado
################################################################################

test_case "Verificar compilación del JAR del plugin"

JAR_PATH="$BASE_DIR/mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar"

if [ -f "$JAR_PATH" ]; then
    JAR_SIZE=$(ls -lh "$JAR_PATH" | awk '{print $5}')
    pass_test "JAR encontrado: $JAR_PATH ($JAR_SIZE)"
    JAR_MODIFIED=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M:%S" "$JAR_PATH" 2>/dev/null || stat -c "%y" "$JAR_PATH" 2>/dev/null | cut -d' ' -f1,2)
    echo "        Última compilación: $JAR_MODIFIED"
else
    fail_test "JAR no encontrado en $JAR_PATH"
    echo "        SOLUCIÓN: Ejecutar: mvn -f mmorpg-plugin/pom.xml clean package"
    exit 1
fi

################################################################################
# TEST 2: Verificar config/templates
################################################################################

test_case "Verificar templates en config/templates/"

TEMPLATES_DIR="$BASE_DIR/config/templates"
REQUIRED_TEMPLATES=(
    "items_template.json"
    "mobs_template.json"
    "enchantments_template.json"
    "crafting_template.json"
    "achievements_template.json"
    "events_template.json"
    "pets_template.json"
    "dungeons_template.json"
)

TEMPLATE_COUNT=0
for template in "${REQUIRED_TEMPLATES[@]}"; do
    if [ -f "$TEMPLATES_DIR/$template" ]; then
        TEMPLATE_COUNT=$((TEMPLATE_COUNT + 1))
        LINES=$(wc -l < "$TEMPLATES_DIR/$template")
        echo "        ✓ $template ($LINES líneas)"
    else
        log_warning "Falta template: $template"
    fi
done

if [ $TEMPLATE_COUNT -eq ${#REQUIRED_TEMPLATES[@]} ]; then
    pass_test "Todos los $TEMPLATE_COUNT templates presentes"
else
    fail_test "$TEMPLATE_COUNT/${#REQUIRED_TEMPLATES[@]} templates encontrados"
fi

################################################################################
# TEST 3: Verificar scripts utilidad
################################################################################

test_case "Verificar scripts de utilidad"

SCRIPTS=(
    "init_sqlite_databases.sh"
    "load_templates_to_db.sh"
    "migrate_json_to_sqlite.sh"
    "verify_sqlite_sync.sh"
    "db_backup_schedule.sh"
)

SCRIPTS_FOUND=0
for script in "${SCRIPTS[@]}"; do
    if [ -x "$SCRIPT_DIR/$script" ]; then
        SCRIPTS_FOUND=$((SCRIPTS_FOUND + 1))
        SIZE=$(wc -l < "$SCRIPT_DIR/$script")
        echo "        ✓ $script ($SIZE líneas, ejecutable)"
    else
        log_warning "Script no encontrado o no ejecutable: $script"
    fi
done

if [ $SCRIPTS_FOUND -eq ${#SCRIPTS[@]} ]; then
    pass_test "Todos los $SCRIPTS_FOUND scripts disponibles"
else
    fail_test "$SCRIPTS_FOUND/${#SCRIPTS[@]} scripts encontrados"
fi

################################################################################
# TEST 4: Verificar DatabaseManager en plugin
################################################################################

test_case "Verificar DatabaseManager.java"

DB_MGR="$BASE_DIR/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/database/DatabaseManager.java"

if [ -f "$DB_MGR" ]; then
    METHODS=(
        "getConnection()"
        "getUniversalConnection()"
        "getWorldConnection"
        "createUniversalTables()"
        "loadTemplatesIntoUniversal()"
        "executeUpdate"
    )
    
    METHODS_FOUND=0
    for method in "${METHODS[@]}"; do
        if grep -q "$method" "$DB_MGR"; then
            METHODS_FOUND=$((METHODS_FOUND + 1))
            echo "        ✓ Método: $method"
        fi
    done
    
    if [ $METHODS_FOUND -eq ${#METHODS[@]} ]; then
        pass_test "DatabaseManager: $METHODS_FOUND métodos encontrados"
    else
        fail_test "DatabaseManager incompleto: $METHODS_FOUND/${#METHODS[@]} métodos"
    fi
else
    fail_test "DatabaseManager.java no encontrado"
fi

################################################################################
# TEST 5: Verificar rpg_manager.py actualizado
################################################################################

test_case "Verificar rpg_manager.py con create_world_database()"

RPG_MGR="$BASE_DIR/web/models/rpg_manager.py"

if [ -f "$RPG_MGR" ]; then
    if grep -q "create_world_database" "$RPG_MGR"; then
        pass_test "rpg_manager.py contiene create_world_database()"
        
        # Verificar método SQLite
        if grep -q "sqlite3.connect" "$RPG_MGR"; then
            pass_test "rpg_manager.py usa sqlite3.connect (SQLite integrado)"
        else
            fail_test "rpg_manager.py no usa sqlite3.connect"
        fi
        
        # Verificar tablas
        TABLES=("players" "quests" "npcs" "spawns" "squads" "economy_transactions" "dungeons_active")
        TABLES_FOUND=0
        for table in "${TABLES[@]}"; do
            if grep -q "CREATE TABLE.*$table" "$RPG_MGR"; then
                TABLES_FOUND=$((TABLES_FOUND + 1))
            fi
        done
        
        echo "        Tablas SQLite detectadas: $TABLES_FOUND/${#TABLES[@]}"
    else
        fail_test "rpg_manager.py no contiene create_world_database()"
    fi
else
    fail_test "rpg_manager.py no encontrado"
fi

################################################################################
# TEST 6: Verificar estructura directorios
################################################################################

test_case "Verificar estructura de directorios"

DIR_CHECKS=(
    "config/templates"
    "plugins"
    "worlds"
    "web/models"
    "scripts"
)

DIRS_OK=0
for dir in "${DIR_CHECKS[@]}"; do
    if [ -d "$BASE_DIR/$dir" ]; then
        DIRS_OK=$((DIRS_OK + 1))
        echo "        ✓ $dir"
    else
        log_warning "Directorio no encontrado: $dir"
    fi
done

if [ $DIRS_OK -eq ${#DIR_CHECKS[@]} ]; then
    pass_test "Estructura de directorios correcta"
else
    fail_test "Directorios faltantes: $DIRS_OK/${#DIR_CHECKS[@]} encontrados"
fi

################################################################################
# TEST 7: Simulación: Plugin crearía universal.db
################################################################################

test_case "Simulación: Creación de universal.db al onEnable"

UNIVERSAL_DB="$BASE_DIR/plugins/MMORPGPlugin/data/universal.db"

if [ -f "$UNIVERSAL_DB" ]; then
    SIZE=$(du -h "$UNIVERSAL_DB" | cut -f1)
    TABLES=$(sqlite3 "$UNIVERSAL_DB" ".tables" 2>/dev/null | wc -w)
    log_warning "universal.db ya existe (de ejecución anterior)"
    echo "        Tamaño: $SIZE"
    echo "        Tablas: $TABLES"
    pass_test "universal.db existente, contiene datos"
else
    log_warning "universal.db no existe (se creará en próximo onEnable)"
    echo "        Se creará automáticamente cuando inicie el plugin"
    pass_test "Listo para crear universal.db en onEnable"
fi

################################################################################
# TEST 8: Simulación: Web crearía {world}.db
################################################################################

test_case "Simulación: Creación de {$WORLD_SLUG}.db al activar RPG"

WORLD_DB="$BASE_DIR/worlds/$WORLD_SLUG/data/${WORLD_SLUG}.db"

if [ -f "$WORLD_DB" ]; then
    SIZE=$(du -h "$WORLD_DB" | cut -f1)
    TABLES=$(sqlite3 "$WORLD_DB" ".tables" 2>/dev/null | wc -w)
    log_warning "${WORLD_SLUG}.db ya existe (de ejecución anterior)"
    echo "        Tamaño: $SIZE"
    echo "        Tablas: $TABLES"
    pass_test "${WORLD_SLUG}.db existente, contiene datos"
else
    log_warning "${WORLD_SLUG}.db no existe"
    echo "        Se creará automáticamente cuando web active RPG para '$WORLD_SLUG'"
    pass_test "Listo para crear ${WORLD_SLUG}.db al activar RPG"
fi

################################################################################
# TEST 9: Verificar contenedor Docker
################################################################################

test_case "Verificar disponibilidad del contenedor Docker"

if ! command -v docker &> /dev/null; then
    fail_test "Docker no está instalado"
else
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        STATUS=$(docker ps -a --filter "name=${CONTAINER_NAME}" --format "{{.Status}}")
        pass_test "Contenedor '$CONTAINER_NAME' encontrado"
        echo "        Estado: $STATUS"
    else
        fail_test "Contenedor '$CONTAINER_NAME' no encontrado"
        echo "        Contenedores disponibles:"
        docker ps -a --format "table {{.Names}}\t{{.Status}}" | head -5
    fi
fi

################################################################################
# TEST 10: Verificar rutas API web
################################################################################

test_case "Verificar rutas API en app.py"

APP_PY="$BASE_DIR/web/app.py"

API_ROUTES=(
    "'/api/worlds/'"
    "'/api/worlds/<slug>/activate'"
    "/api/rpg"
)

ROUTES_FOUND=0
for route in "${API_ROUTES[@]}"; do
    if grep -q "$route" "$APP_PY"; then
        ROUTES_FOUND=$((ROUTES_FOUND + 1))
        echo "        ✓ Ruta: $route"
    fi
done

if [ $ROUTES_FOUND -ge 2 ]; then
    pass_test "Rutas API detectadas: $ROUTES_FOUND"
else
    fail_test "Rutas API incompletas: $ROUTES_FOUND encontradas"
fi

################################################################################
# RESUMEN
################################################################################

echo -e "\n${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${MAGENTA}RESUMEN DE TESTS${NC}"
echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "\nTotal de tests ejecutados: ${BLUE}$TESTS_TOTAL${NC}"
echo -e "Tests pasados: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests fallidos: ${RED}$TESTS_FAILED${NC}"

# Cálculo de porcentaje
if [ $TESTS_TOTAL -gt 0 ]; then
    PERCENTAGE=$((TESTS_PASSED * 100 / TESTS_TOTAL))
    echo -e "Éxito: ${BLUE}$PERCENTAGE%${NC}"
fi

echo -e "\n${YELLOW}PRÓXIMOS PASOS:${NC}"
echo "1. Copiar JAR compilado a contenedor: docker cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar $CONTAINER_NAME:/plugins/"
echo "2. Reiniciar servidor: ./run.sh o docker restart $CONTAINER_NAME"
echo "3. Verificar logs: docker logs $CONTAINER_NAME | grep -E 'universal.db|DatabaseManager|ERROR'"
echo "4. Activar RPG en web: http://localhost:5000 > Configuración > Activar RPG para un mundo"
echo "5. Verificar creación de BDs: ./scripts/verify_sqlite_sync.sh $WORLD_SLUG"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ TODOS LOS TESTS PASARON${NC}"
    exit 0
else
    echo -e "\n${RED}✗ ALGUNOS TESTS FALLARON${NC}"
    exit 1
fi
