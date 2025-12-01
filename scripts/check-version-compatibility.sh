#!/bin/bash

# Script para verificar configuración de compatibilidad de versiones
# Ejecutar: ./check-version-compatibility.sh

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   🔍 VERIFICACIÓN DE COMPATIBILIDAD DE VERSIONES              ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# 1. Verificar versión del servidor
echo "📊 [1/4] Versión del servidor:"
if sudo docker ps | grep -q mc-paper; then
    echo "   Servidor: CORRIENDO"
    # Buscar versión en logs
    VERSION=$(sudo docker logs mc-paper 2>&1 | grep -oP "This server is running.*Paper version.*\K[0-9]+\.[0-9]+\.[0-9]+" | head -1)
    if [ ! -z "$VERSION" ]; then
        echo "   Versión: PaperMC $VERSION"
    else
        echo "   Versión: No detectada (verifica los logs)"
    fi
else
    echo "   ⚠️  Servidor detenido"
fi
echo ""

# 2. Verificar plugins instalados
echo "🔌 [2/4] Plugins de compatibilidad instalados:"
PLUGINS_DIR="./plugins"

if [ -f "$PLUGINS_DIR/ViaVersion.jar" ]; then
    SIZE=$(ls -lh "$PLUGINS_DIR/ViaVersion.jar" | awk '{print $5}')
    echo "   ✅ ViaVersion ($SIZE)"
else
    echo "   ❌ ViaVersion NO instalado"
fi

if [ -f "$PLUGINS_DIR/ViaBackwards.jar" ]; then
    SIZE=$(ls -lh "$PLUGINS_DIR/ViaBackwards.jar" | awk '{print $5}')
    echo "   ✅ ViaBackwards ($SIZE)"
else
    echo "   ❌ ViaBackwards NO instalado"
fi

if [ -f "$PLUGINS_DIR/ViaRewind.jar" ]; then
    SIZE=$(ls -lh "$PLUGINS_DIR/ViaRewind.jar" | awk '{print $5}')
    echo "   ✅ ViaRewind ($SIZE)"
else
    echo "   ❌ ViaRewind NO instalado"
fi
echo ""

# 3. Verificar plugins activos en el servidor
echo "⚡ [3/4] Plugins activos en el servidor:"
if sudo docker ps | grep -q mc-paper; then
    sudo docker exec mc-paper ls -1 plugins/ 2>/dev/null | grep -E "ViaVersion|ViaBackwards|ViaRewind" || echo "   ⚠️  No se pudieron listar plugins en el contenedor"
else
    echo "   ⚠️  Servidor detenido, no se puede verificar"
fi
echo ""

# 4. Explicación
echo "📚 [4/4] Compatibilidad de versiones:"
echo ""
echo "Con tu configuración actual (PaperMC 1.21.10 + ViaBackwards):"
echo ""
echo "✅ FUNCIONA:"
echo "   • Clientes Minecraft 1.21.x → Servidor 1.21.10"
echo "   • Clientes Minecraft 1.20.x → Servidor 1.21.10 (ViaBackwards)"
echo "   • Clientes Minecraft 1.19.x → Servidor 1.21.10 (ViaBackwards)"
echo "   • Clientes Minecraft 1.18.x → Servidor 1.21.10 (ViaBackwards)"
echo "   • Clientes Minecraft 1.16.x → Servidor 1.21.10 (ViaBackwards + ViaRewind)"
echo "   • Clientes Minecraft 1.12.x → Servidor 1.21.10 (ViaBackwards + ViaRewind)"
echo "   • Clientes Minecraft 1.8.x  → Servidor 1.21.10 (ViaRewind)"
echo ""
echo "❌ NO FUNCIONA:"
echo "   • No puedes conectarte con versiones SUPERIORES a 1.21.10"
echo "   • No puedes hacer que el servidor simule ser una versión anterior"
echo ""

# 5. Verificar en logs si hay errores de ViaVersion
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║  🔍 DIAGNÓSTICO                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

if sudo docker ps | grep -q mc-paper; then
    echo "Buscando errores de ViaVersion en logs..."
    ERRORS=$(sudo docker logs mc-paper 2>&1 | grep -i "viaversion\|viabackwards\|viarewind" | grep -i "error\|exception\|failed" | tail -5)
    
    if [ ! -z "$ERRORS" ]; then
        echo "⚠️  Errores encontrados:"
        echo "$ERRORS"
    else
        echo "✅ No se encontraron errores de ViaVersion"
    fi
    echo ""
    
    echo "Versiones soportadas por ViaBackwards:"
    sudo docker logs mc-paper 2>&1 | grep -i "viabackwards.*support" | tail -3
else
    echo "⚠️  Inicia el servidor primero: ./run.sh"
fi

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║  💡 RECOMENDACIONES                                           ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "Si los jugadores no pueden conectarse:"
echo ""
echo "1. Verifica que los plugins estén cargados:"
echo "   sudo docker logs mc-paper | grep -i \"viaversion\""
echo ""
echo "2. Revisa la versión del cliente:"
echo "   • Debe ser 1.8.x o superior"
echo "   • Debe ser IGUAL O INFERIOR a 1.21.10"
echo ""
echo "3. Si quieres que TODOS jueguen en una versión anterior:"
echo "   ./change-server-version.sh 1.20.4"
echo ""
echo "4. Actualiza los plugins a las últimas versiones:"
echo "   ./update.sh --plugins"
echo ""
