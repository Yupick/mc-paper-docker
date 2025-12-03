#!/bin/bash

# Script para compilar el plugin MMORPG y copiarlo al directorio de plugins

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
PLUGIN_SOURCE_DIR="$PROJECT_ROOT/mmorpg-plugin"
PLUGIN_TARGET_DIR="$PROJECT_ROOT/plugins"
PLUGIN_SOURCE_JAR="mmorpg-plugin-1.0.0.jar"
PLUGIN_TARGET_JAR="MMORPGPlugin.jar"

echo "=== Build MMORPG Plugin ==="
echo "Proyecto: $PLUGIN_SOURCE_DIR"
echo ""

# Compilar con Maven usando Docker
echo "📦 Compilando plugin con Maven (Docker)..."
docker run --rm \
    -v "$PLUGIN_SOURCE_DIR":/workspace \
    -w /workspace \
    maven:3.9-eclipse-temurin-21 \
    mvn clean package -q

# Verificar que el JAR se haya creado
if [ ! -f "$PLUGIN_SOURCE_DIR/target/$PLUGIN_SOURCE_JAR" ]; then
    echo "❌ Error: No se encontró el JAR compilado en target/$PLUGIN_SOURCE_JAR"
    exit 1
fi

echo "✅ Plugin compilado exitosamente"

# Crear directorio de plugins si no existe
mkdir -p "$PLUGIN_TARGET_DIR"

# Copiar JAR al directorio de plugins
echo "📋 Copiando $PLUGIN_SOURCE_JAR a plugins/$PLUGIN_TARGET_JAR..."
cp "$PLUGIN_SOURCE_DIR/target/$PLUGIN_SOURCE_JAR" "$PLUGIN_TARGET_DIR/$PLUGIN_TARGET_JAR"

echo "✅ Plugin copiado a $PLUGIN_TARGET_DIR/$PLUGIN_TARGET_JAR"
echo ""
echo "=== Build completado ==="
echo "Para aplicar cambios, reinicia el servidor de Minecraft"
