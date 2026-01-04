#!/bin/bash

################################################################################
# db_backup_schedule.sh
#
# Respalda bases de datos SQLite de forma programada
# - Realiza backup de universal.db
# - Realiza backup de cada {world}.db
# - Gestiona rotación de backups antiguos
# - Genera checksums para verificación
#
# Backups realizados:
# 1. universal.db → backups/universal_TIMESTAMP.db
# 2. {world}.db → backups/{world}_TIMESTAMP.db (para cada mundo)
# 3. Checksum files para verificar integridad
# 4. Mantiene últimos 10 backups
# 5. Comprime backups más antiguos
#
# Uso: ./db_backup_schedule.sh [--full|--incremental|--cleanup]
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PLUGIN_DATA_DIR="$PROJECT_ROOT/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/data"
WORLDS_DIR="$PROJECT_ROOT/worlds"
BACKUP_DIR="$PLUGIN_DATA_DIR/backups"

# Configuración
KEEP_BACKUPS=10
BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "═══════════════════════════════════════════════════════════════════════════════"
echo "  💾 GESTOR DE BACKUPS SQLite"
echo "═══════════════════════════════════════════════════════════════════════════════"

################################################################################
# Funciones de backup
################################################################################

# Crear directorio de backup
init_backup_dir() {
    mkdir -p "$BACKUP_DIR"
    echo -e "${GREEN}✅ Directorio de backups: $BACKUP_DIR${NC}"
}

# Realizar backup completo
backup_universal_full() {
    echo -e "\n${BLUE}[1/4] Realizando backup de universal.db...${NC}"
    
    if [ ! -f "$PLUGIN_DATA_DIR/universal.db" ]; then
        echo -e "${YELLOW}⚠️  universal.db no encontrado${NC}"
        return
    fi
    
    local backup_file="$BACKUP_DIR/universal_${BACKUP_TIMESTAMP}.db"
    cp "$PLUGIN_DATA_DIR/universal.db" "$backup_file"
    
    local size=$(du -h "$backup_file" | cut -f1)
    echo -e "${GREEN}✅ Backup creado: $backup_file ($size)${NC}"
    
    # Generar checksum
    sha256sum "$backup_file" > "${backup_file}.sha256"
    echo -e "${GREEN}✅ Checksum generado${NC}"
}

# Realizar backup de mundos
backup_worlds() {
    echo -e "\n${BLUE}[2/4] Realizando backup de mundos...${NC}"
    
    if [ ! -d "$WORLDS_DIR" ]; then
        echo -e "${YELLOW}⚠️  No hay directorio worlds${NC}"
        return
    fi
    
    local world_count=0
    for world_dir in "$WORLDS_DIR"/*/; do
        if [ -d "$world_dir" ]; then
            local world=$(basename "$world_dir")
            local db_file="$world_dir/data/${world}.db"
            
            if [ -f "$db_file" ]; then
                local backup_file="$BACKUP_DIR/${world}_${BACKUP_TIMESTAMP}.db"
                cp "$db_file" "$backup_file"
                
                local size=$(du -h "$backup_file" | cut -f1)
                echo "   ✅ $world ($size)"
                
                # Checksum
                sha256sum "$backup_file" > "${backup_file}.sha256"
                
                ((world_count++))
            fi
        fi
    done
    
    echo -e "${GREEN}✅ $world_count mundos respaldados${NC}"
}

# Limpiar backups antiguos
cleanup_old_backups() {
    echo -e "\n${BLUE}[3/4] Limpiando backups antiguos...${NC}"
    
    # Contar backups de universal.db
    local universal_backups=$(ls -1 "$BACKUP_DIR"/universal_*.db 2>/dev/null | wc -l)
    echo "   Backups de universal.db: $universal_backups"
    
    # Eliminar backups antiguos (mantener KEEP_BACKUPS)
    if [ "$universal_backups" -gt "$KEEP_BACKUPS" ]; then
        local to_delete=$((universal_backups - KEEP_BACKUPS))
        echo "   Eliminando $to_delete backups antiguos..."
        
        ls -1t "$BACKUP_DIR"/universal_*.db | tail -n "$to_delete" | while read -r file; do
            rm -f "$file"
            rm -f "${file}.sha256"
            echo "   🗑️  Eliminado: $(basename "$file")"
        done
    fi
    
    echo -e "${GREEN}✅ Limpieza completada${NC}"
}

# Comprimir backups antiguos
compress_old_backups() {
    echo -e "\n${BLUE}[4/4] Comprimiendo backups antiguos...${NC}"
    
    # Buscar archivos .db sin comprimir más antiguos de 7 días
    find "$BACKUP_DIR" -name "*.db" -mtime +7 ! -name "*.gz" -type f | while read -r file; do
        local size_before=$(du -h "$file" | cut -f1)
        
        gzip "$file"
        local compressed_file="${file}.gz"
        local size_after=$(du -h "$compressed_file" | cut -f1)
        
        echo "   📦 Comprimido: $(basename "$file") ($size_before → $size_after)"
    done
    
    echo -e "${GREEN}✅ Compresión completada${NC}"
}

################################################################################
# Reportes
################################################################################

show_backup_summary() {
    echo -e "\n${BLUE}📊 RESUMEN DE BACKUPS${NC}"
    echo ""
    
    if [ ! -d "$BACKUP_DIR" ]; then
        echo -e "${YELLOW}No hay backups aún${NC}"
        return
    fi
    
    # Contar backups
    local universal_count=$(ls -1 "$BACKUP_DIR"/universal_*.db 2>/dev/null | wc -l)
    local world_count=$(ls -1 "$BACKUP_DIR"/*.db 2>/dev/null | grep -v universal | wc -l)
    local total_size=$(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1)
    
    echo "   Backups de universal.db: $universal_count"
    echo "   Backups de mundos: $world_count"
    echo "   Tamaño total: $total_size"
    echo ""
    
    echo "   Backups más recientes:"
    ls -1lhrt "$BACKUP_DIR"/*.db 2>/dev/null | tail -5 | awk '{print "   " $9 " (" $5 ")"}'
}

verify_backup_integrity() {
    echo -e "\n${BLUE}🔍 VERIFICANDO INTEGRIDAD DE BACKUPS...${NC}"
    
    local failed=0
    
    # Verificar checksums
    find "$BACKUP_DIR" -name "*.sha256" | while read -r checksum_file; do
        if ! sha256sum -c "$checksum_file" > /dev/null 2>&1; then
            echo -e "${RED}   ❌ Checksum INVÁLIDO: $checksum_file${NC}"
            ((failed++))
        fi
    done
    
    if [ "$failed" -eq 0 ]; then
        echo -e "${GREEN}   ✅ Todos los backups son válidos${NC}"
    else
        echo -e "${RED}   ❌ $failed backups con errores${NC}"
    fi
}

################################################################################
# Main
################################################################################

main() {
    local mode=${1:-"--full"}
    
    # Crear directorio
    init_backup_dir
    
    case "$mode" in
        --full)
            echo -e "\n${MAGENTA}Realizando backup COMPLETO...${NC}"
            backup_universal_full
            backup_worlds
            cleanup_old_backups
            compress_old_backups
            ;;
        --incremental)
            echo -e "\n${MAGENTA}Realizando backup INCREMENTAL...${NC}"
            backup_universal_full
            backup_worlds
            ;;
        --cleanup)
            echo -e "\n${MAGENTA}Limpiando backups antiguos...${NC}"
            cleanup_old_backups
            compress_old_backups
            ;;
        *)
            echo -e "${RED}Modo desconocido: $mode${NC}"
            echo "Opciones: --full | --incremental | --cleanup"
            exit 1
            ;;
    esac
    
    # Mostrar resumen
    show_backup_summary
    
    # Verificar integridad
    verify_backup_integrity
    
    echo -e "\n${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ BACKUP COMPLETADO${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
}

main "$@"
