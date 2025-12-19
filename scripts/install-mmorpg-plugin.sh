#!/bin/bash

# Script para instalar y configurar el plugin MMORPG
# Copia archivos de configuración y datos desde config/ al plugin

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   🎮 INSTALACIÓN DEL PLUGIN MMORPG                           ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Configurar rutas
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_PLUGIN_DIR="$PROJECT_ROOT/config/plugin"
CONFIG_DATA_DIR="$PROJECT_ROOT/config/plugin-data"
PLUGIN_DIR="$PROJECT_ROOT/plugins/MMORPGPlugin"
PLUGIN_DATA_DIR="$PLUGIN_DIR/data"

echo "📁 Rutas configuradas:"
echo "   - Proyecto: $PROJECT_ROOT"
echo "   - Config plugin: $CONFIG_PLUGIN_DIR"
echo "   - Config datos: $CONFIG_DATA_DIR"
echo "   - Plugin destino: $PLUGIN_DIR"
echo ""

# Verificar que existen los directorios de config
if [ ! -d "$CONFIG_PLUGIN_DIR" ]; then
    echo "❌ Error: $CONFIG_PLUGIN_DIR no existe"
    echo "   Ejecuta create.sh primero"
    exit 1
fi

if [ ! -d "$CONFIG_DATA_DIR" ]; then
    echo "❌ Error: $CONFIG_DATA_DIR no existe"
    echo "   Ejecuta create.sh primero"
    exit 1
fi

# Crear directorio del plugin si no existe
if [ ! -d "$PLUGIN_DIR" ]; then
    echo "📁 [1/3] Creando directorio del plugin..."
    mkdir -p "$PLUGIN_DIR"
    echo "   ✅ Creado: $PLUGIN_DIR"
fi

# Crear directorio de datos universales
echo "📁 [2/3] Creando directorio de datos universales..."
mkdir -p "$PLUGIN_DATA_DIR"
echo "   ✅ Creado: $PLUGIN_DATA_DIR"

# Copiar archivos de configuración (sin .example)
echo "📋 [3/3] Copiando archivos de configuración..."
for file in "$CONFIG_PLUGIN_DIR"/*.example; do
    if [ -f "$file" ]; then
        basename_file=$(basename "$file" .example)
        dest="$PLUGIN_DIR/$basename_file"
        
        # Solo copiar si no existe (no sobrescribir ediciones)
        if [ ! -f "$dest" ]; then
            cp "$file" "$dest"
            echo "   ✅ Copiado: $basename_file"
        else
            echo "   ⏭️  Ya existe: $basename_file (no se sobrescribió)"
        fi
    fi
done

echo ""
echo "📊 Copiando datos universales..."
for file in "$CONFIG_DATA_DIR"/*.example; do
    if [ -f "$file" ]; then
        basename_file=$(basename "$file" .example)
        dest="$PLUGIN_DATA_DIR/$basename_file"
        
        # Solo copiar si no existe
        if [ ! -f "$dest" ]; then
            cp "$file" "$dest"
            echo "   ✅ Copiado: $basename_file"
        else
            echo "   ⏭️  Ya existe: $basename_file (no se sobrescribió)"
        fi
    fi
done

echo ""
echo "✅ Instalación del plugin MMORPG completada"
echo ""
echo "📂 Estructura creada:"
echo "   $PLUGIN_DIR/"
echo "   ├── achievements_config.json"
echo "   ├── bestiary_config.json"
echo "   ├── crafting_config.json"
echo "   ├── ... (otros archivos de config)"
echo "   └── data/"
echo "       ├── items.json"
echo "       ├── mobs.json"
echo "       ├── npcs.json"
echo "       ├── quests.json"
echo "       └── enchantments.json"
echo ""
echo "💡 Próximos pasos:"
echo "   1. Reinicia el servidor: ./run.sh"
echo "   2. Verifica los logs para confirmar carga del plugin"
echo "   3. Accede al panel web: http://localhost:5000"
echo ""

# Verificar si Docker está corriendo
if docker ps --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER}$"; then
    show_status "Contenedor Docker encontrado"
    echo ""
    show_info "Copiando archivos al contenedor..."
    
    DOCKER_INSTALLED=0
    for config_file in "${CONFIG_FILES[@]}"; do
        SOURCE="$PLUGIN_DIR/$config_file"
        DEST="$DOCKER_CONTAINER:/server/plugins/MMORPGPlugin/$config_file"
        
        if [ -f "$SOURCE" ]; then
            echo -n "  Copiando $config_file... "
            if docker cp "$SOURCE" "$DEST" 2>/dev/null; then
                show_status "✓"
                DOCKER_INSTALLED=$((DOCKER_INSTALLED+1))
            else
                show_error "Fallo"
            fi
        fi
    done
    
    echo ""
    show_status "$DOCKER_INSTALLED archivos sincronizados al contenedor"
    echo ""
    echo "📝 Recomendación:"
    echo "   El plugin necesita reiniciar para cargar las nuevas configuraciones"
    echo "   Ejecuta: ./restart-server.sh o reinicia el contenedor"
else
    show_warning "Contenedor Docker no está corriendo"
    echo ""
    show_info "Los archivos serán sincronizados cuando inicie el servidor"
fi

echo ""
echo "========================================="
echo "📊 Resumen de Instalación"
echo "========================================="
echo ""
echo "Archivos instalados: $INSTALLED"
echo "Archivos no encontrados: $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    show_status "Instalación completada exitosamente"
    echo ""
    echo "El plugin MMORPG está listo. Próximos pasos:"
    echo "  1. Inicia el panel web: ./start-web-panel.sh"
    echo "  2. Accede a: http://localhost:5000"
    echo "  3. Verifica los logs del servidor para confirmar carga del plugin"
    exit 0
else
    show_error "Algunos archivos no pudieron instalarse"
    echo ""
    echo "Verifica que los archivos existan en: $CONFIG_DIR/"
    exit 1
fi
