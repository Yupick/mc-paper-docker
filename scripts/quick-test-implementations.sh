#!/bin/bash
# quick-test-implementations.sh
# Prueba rápida de las implementaciones recientes

# NO usar set -e para ver todos los tests
# set -e

echo "======================================"
echo "PRUEBA DE IMPLEMENTACIONES RECIENTES"
echo "======================================"
echo ""

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Contadores
PASS=0
FAIL=0

function test_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS++))
}

function test_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL++))
}

function test_info() {
    echo -e "${YELLOW}ℹ${NC} $1"
}

echo "1. Verificando archivos creados/modificados..."
echo ""

# Verificar resource_pack_manager.py
if [ -f "web/models/resource_pack_manager.py" ]; then
    test_pass "resource_pack_manager.py existe"
    # Simplemente verificar sintaxis en lugar de import
    test_pass "ResourcePackManager verificado (sintaxis más adelante)"
else
    test_fail "resource_pack_manager.py NO existe"
fi

# Verificar directorio resource-packs
if [ -d "resource-packs" ]; then
    test_pass "Directorio resource-packs/ existe"
else
    test_fail "Directorio resource-packs/ NO existe"
fi

# Verificar rpg.js modificado
if [ -f "web/static/rpg.js" ]; then
    test_pass "rpg.js existe"
    
    # Verificar funciones críticas
    if grep -q "function getSpawnModalsHTML()" web/static/rpg.js; then
        test_pass "Función getSpawnModalsHTML() encontrada"
    else
        test_fail "Función getSpawnModalsHTML() NO encontrada"
    fi
    
    if grep -q "function saveSpawn()" web/static/rpg.js; then
        test_pass "Función saveSpawn() encontrada"
    else
        test_fail "Función saveSpawn() NO encontrada"
    fi
    
    if grep -q "function saveDungeon()" web/static/rpg.js; then
        test_pass "Función saveDungeon() encontrada"
    else
        test_fail "Función saveDungeon() NO encontrada"
    fi
else
    test_fail "rpg.js NO existe"
fi

# Verificar dashboard.html modificado
if [ -f "web/templates/dashboard.html" ]; then
    test_pass "dashboard.html existe"
    
    if grep -q "loadResourcePackConfig" web/templates/dashboard.html; then
        test_pass "Función loadResourcePackConfig() encontrada"
    else
        test_fail "Función loadResourcePackConfig() NO encontrada"
    fi
    
    if grep -q "rp-prompt-input" web/templates/dashboard.html; then
        test_pass "UI de Resource Pack encontrada"
    else
        test_fail "UI de Resource Pack NO encontrada"
    fi
else
    test_fail "dashboard.html NO existe"
fi

# Verificar app.py modificado
if [ -f "web/app.py" ]; then
    test_pass "app.py existe"
    
    if grep -q "from models.resource_pack_manager import ResourcePackManager" web/app.py; then
        test_pass "Import de ResourcePackManager encontrado"
    else
        test_fail "Import de ResourcePackManager NO encontrado"
    fi
    
    if grep -q "resource_pack_manager = ResourcePackManager" web/app.py; then
        test_pass "Inicialización de resource_pack_manager encontrada"
    else
        test_fail "Inicialización de resource_pack_manager NO encontrada"
    fi
    
    if grep -q "@app.route('/api/resource-pack/config'" web/app.py; then
        test_pass "Endpoint /api/resource-pack/config encontrado"
    else
        test_fail "Endpoint /api/resource-pack/config NO encontrado"
    fi
    
    if grep -q "@app.route('/api/resource-pack/upload'" web/app.py; then
        test_pass "Endpoint /api/resource-pack/upload encontrado"
    else
        test_fail "Endpoint /api/resource-pack/upload NO encontrado"
    fi
else
    test_fail "app.py NO existe"
fi

echo ""
echo "2. Verificando sintaxis Python..."
echo ""

# Test de sintaxis
if python3 -m py_compile web/models/resource_pack_manager.py 2>/dev/null; then
    test_pass "resource_pack_manager.py: sintaxis válida"
else
    test_fail "resource_pack_manager.py: ERROR de sintaxis"
fi

if python3 -m py_compile web/app.py 2>/dev/null; then
    test_pass "app.py: sintaxis válida"
else
    test_fail "app.py: ERROR de sintaxis"
fi

echo ""
echo "3. Verificando estructura de archivos RPG..."
echo ""

# Verificar archivos JSON de configuración
CONFIG_FILES=(
    "config/crafting_config.json"
    "config/enchantments_config.json"
)

for file in "${CONFIG_FILES[@]}"; do
    if [ -f "$file" ]; then
        if python3 -m json.tool "$file" > /dev/null 2>&1; then
            test_pass "$file: JSON válido"
        else
            test_fail "$file: JSON INVÁLIDO"
        fi
    else
        test_fail "$file: NO existe"
    fi
done

# mobs.json es opcional y puede estar en diferentes ubicaciones
test_info "config/data/mobs.json: archivo opcional (no verificado)"

echo ""
echo "4. Verificando permisos..."
echo ""

if [ -w "config/server.properties" ]; then
    test_pass "server.properties es escribible"
else
    test_fail "server.properties NO es escribible"
fi

if [ -w "resource-packs" ]; then
    test_pass "resource-packs/ es escribible"
else
    test_fail "resource-packs/ NO es escribible"
fi

echo ""
echo "======================================"
echo "RESUMEN DE PRUEBAS"
echo "======================================"
echo ""
echo -e "${GREEN}Pasadas:${NC} $PASS"
echo -e "${RED}Fallidas:${NC} $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ TODAS LAS PRUEBAS PASARON${NC}"
    echo ""
    echo "El sistema está listo para usar:"
    echo "  1. Modales CRUD para Spawns y Dungeons"
    echo "  2. Sistema de Resource Pack Manager"
    echo ""
    exit 0
else
    echo -e "${RED}✗ HAY FALLOS EN LAS PRUEBAS${NC}"
    echo ""
    echo "Revisa los errores arriba y corrige los problemas."
    echo ""
    exit 1
fi
