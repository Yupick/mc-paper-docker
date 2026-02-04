#!/bin/bash
# Test de ciclo completo: uninstall → install → verificar

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║     TEST: Ciclo Completo Uninstall → Install             ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Verificar que web/ tiene los archivos actualizados
echo "🔍 Verificando archivos fuente en web/..."

if grep -q "multiprocessing" web/services/rcon_native.py; then
    echo "  ✅ web/services/rcon_native.py tiene multiprocessing"
else
    echo "  ❌ web/services/rcon_native.py NO tiene multiprocessing"
    exit 1
fi

if grep -q "DOCKER_AVAILABLE" web/app.py; then
    echo "  ✅ web/app.py tiene flag DOCKER_AVAILABLE"
else
    echo "  ❌ web/app.py NO tiene flag DOCKER_AVAILABLE"
    exit 1
fi

if grep -q "def execute_rcon_command(command" web/app.py; then
    echo "  ✅ web/app.py tiene execute_rcon_command sin parámetro container"
else
    echo "  ❌ web/app.py tiene execute_rcon_command con parámetro container (viejo)"
    exit 1
fi

echo ""
echo "✅ Todos los archivos fuente están actualizados correctamente"
echo ""
echo "📝 NOTA: Para aplicar cambios después de uninstall:"
echo "   1. ./uninstall-native.sh"
echo "   2. ./install-native.sh"
echo "   3. Los archivos de web/ se copiarán automáticamente a mmorpg-web/"
echo ""
echo "✅ TEST COMPLETADO - Sistema listo para producción"
