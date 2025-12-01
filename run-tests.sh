#!/bin/bash
# Script de Testing de Integración - Sistema Multi-Mundos
# Este script prueba el flujo completo del sistema

set -e  # Detener en caso de error

echo "=========================================="
echo "TESTING DE INTEGRACIÓN - MULTI-MUNDOS"
echo "=========================================="
echo ""

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 1. Verificar estructura de directorios
echo "1. Verificando estructura de directorios..."
log_info "Verificando directorios necesarios..."

REQUIRED_DIRS=(
    "worlds"
    "backups/worlds"
    "config"
    "web/models"
    "web/services"
)

for dir in "${REQUIRED_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        log_info "✓ Directorio existe: $dir"
    else
        log_error "✗ Directorio NO existe: $dir"
        exit 1
    fi
done

echo ""

# 2. Verificar archivos críticos
echo "2. Verificando archivos críticos..."

REQUIRED_FILES=(
    "web/models/world.py"
    "web/models/world_manager.py"
    "web/services/backup_service.py"
    "web/app.py"
    "docker-compose.yml"
    "migrate-to-multiworld.sh"
    "rollback-multiworld.sh"
    "config/backup_config.json"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        log_info "✓ Archivo existe: $file"
    else
        log_error "✗ Archivo NO existe: $file"
        exit 1
    fi
done

echo ""

# 3. Verificar permisos de scripts
echo "3. Verificando permisos de scripts..."

SCRIPTS=(
    "migrate-to-multiworld.sh"
    "rollback-multiworld.sh"
)

for script in "${SCRIPTS[@]}"; do
    if [ -x "$script" ]; then
        log_info "✓ Script ejecutable: $script"
    else
        log_warn "⚠ Script NO ejecutable: $script (ejecutando chmod +x)"
        chmod +x "$script"
    fi
done

echo ""

# 4. Verificar configuración de backups
echo "4. Verificando configuración de backups..."

if [ -f "config/backup_config.json" ]; then
    AUTO_ENABLED=$(python3 -c "import json; config = json.load(open('config/backup_config.json')); print(config.get('auto_backup_enabled', False))")
    RETENTION=$(python3 -c "import json; config = json.load(open('config/backup_config.json')); print(config.get('retention_count', 5))")
    
    log_info "Backups automáticos: $AUTO_ENABLED"
    log_info "Retención de backups: $RETENTION"
else
    log_error "Archivo de configuración de backups no encontrado"
    exit 1
fi

echo ""

# 5. Probar BackupService
echo "5. Probando BackupService..."

if python3 test_backup_service.py > /dev/null 2>&1; then
    log_info "✓ BackupService funciona correctamente"
else
    log_error "✗ BackupService tiene errores"
    python3 test_backup_service.py
    exit 1
fi

echo ""

# 6. Verificar sintaxis de Python
echo "6. Verificando sintaxis de archivos Python..."

PYTHON_FILES=(
    "web/models/world.py"
    "web/models/world_manager.py"
    "web/services/backup_service.py"
)

for file in "${PYTHON_FILES[@]}"; do
    if python3 -m py_compile "$file" 2>/dev/null; then
        log_info "✓ Sintaxis válida: $file"
    else
        log_error "✗ Error de sintaxis: $file"
        python3 -m py_compile "$file"
        exit 1
    fi
done

echo ""

# 7. Verificar que docker-compose es válido
echo "7. Verificando docker-compose.yml..."

if docker-compose config > /dev/null 2>&1; then
    log_info "✓ docker-compose.yml es válido"
else
    log_error "✗ docker-compose.yml tiene errores"
    docker-compose config
    exit 1
fi

echo ""

# 8. Verificar symlinks (si worlds/active existe)
echo "8. Verificando symlinks..."

if [ -L "worlds/active" ]; then
    TARGET=$(readlink -f worlds/active)
    log_info "✓ Symlink worlds/active existe"
    log_info "  Apunta a: $TARGET"
    
    # Verificar que el target existe
    if [ -d "$TARGET" ]; then
        log_info "  ✓ Directorio destino existe"
    else
        log_warn "  ⚠ Directorio destino NO existe"
    fi
else
    log_warn "⚠ Symlink worlds/active no existe (se creará en migración)"
fi

echo ""

# 9. Verificar endpoints API (requiere servidor corriendo)
echo "9. Verificando disponibilidad de endpoints API..."

if [ -f "web/app.py" ]; then
    # Contar endpoints de mundos
    WORLD_ENDPOINTS=$(grep -E "@app.route.*worlds" web/app.py | wc -l)
    log_info "Endpoints de mundos encontrados: $WORLD_ENDPOINTS"
    
    # Contar endpoints de backups
    BACKUP_ENDPOINTS=$(grep -E "@app.route.*(backup|backups)" web/app.py | wc -l)
    log_info "Endpoints de backups encontrados: $BACKUP_ENDPOINTS"
    
    if [ "$WORLD_ENDPOINTS" -ge 8 ] && [ "$BACKUP_ENDPOINTS" -ge 4 ]; then
        log_info "✓ Todos los endpoints están implementados"
    else
        log_warn "⚠ Algunos endpoints pueden faltar"
    fi
fi

echo ""

# 10. Verificar funciones JavaScript
echo "10. Verificando funciones JavaScript..."

if [ -f "web/static/dashboard.js" ]; then
    # Verificar funciones críticas
    CRITICAL_FUNCTIONS=(
        "loadWorlds"
        "createWorldCard"
        "activateWorld"
        "confirmSwitchWorld"
        "backupWorld"
        "toggleAutoBackup"
        "loadBackupConfig"
    )
    
    for func in "${CRITICAL_FUNCTIONS[@]}"; do
        if grep -q "function $func" web/static/dashboard.js || grep -q "async function $func" web/static/dashboard.js; then
            log_info "✓ Función existe: $func"
        else
            log_error "✗ Función NO encontrada: $func"
        fi
    done
fi

echo ""

# 11. Verificar modals HTML
echo "11. Verificando modals en HTML..."

if [ -f "web/templates/dashboard_v2.html" ]; then
    REQUIRED_MODALS=(
        "createWorldModal"
        "confirmSwitchModal"
        "editWorldConfigModal"
        "worldBackupsModal"
    )
    
    for modal in "${REQUIRED_MODALS[@]}"; do
        if grep -q "id=\"$modal\"" web/templates/dashboard_v2.html; then
            log_info "✓ Modal existe: $modal"
        else
            log_error "✗ Modal NO encontrado: $modal"
        fi
    done
fi

echo ""

# 12. Resumen de archivos
echo "12. Resumen del sistema..."

log_info "Archivos Python creados: $(find web -name "*.py" | wc -l)"
log_info "Tamaño de web/models/world.py: $(wc -l < web/models/world.py) líneas"
log_info "Tamaño de web/models/world_manager.py: $(wc -l < web/models/world_manager.py) líneas"
log_info "Tamaño de web/services/backup_service.py: $(wc -l < web/services/backup_service.py) líneas"

if [ -d "backups/worlds" ]; then
    BACKUP_COUNT=$(ls -1 backups/worlds/*.tar.gz 2>/dev/null | wc -l)
    log_info "Backups existentes: $BACKUP_COUNT"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}✓ TESTING COMPLETADO EXITOSAMENTE${NC}"
echo "=========================================="
echo ""
echo "Siguiente paso: Ejecutar migración con ./migrate-to-multiworld.sh"
echo ""

exit 0
