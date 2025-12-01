#!/bin/bash

# Script para aplicar las correcciones del panel web

echo "🔄 Aplicando correcciones del panel web..."
echo ""

# 1. Reconstruir imagen Docker con rcon-cli
echo "🐳 [1/4] Reconstruyendo imagen Docker (instalar rcon-cli)..."
cd /home/mkd/contenedores/mc-paper
docker-compose build --no-cache
echo "   ✅ Imagen reconstruida"
echo ""

# 2. Reiniciar servidor Minecraft para habilitar RCON
echo "📦 [2/4] Reiniciando servidor Minecraft (aplicar RCON)..."
docker-compose down
docker-compose up -d
echo "   ✅ Servidor Minecraft reiniciado"
echo ""

# 3. Reiniciar panel web
echo "🌐 [3/4] Reiniciando panel web..."
cd /home/mkd/contenedores/mc-paper/web
./stop-web-panel.sh 2>/dev/null || true
sleep 2
./start-web-panel.sh
echo "   ✅ Panel web reiniciado"
echo ""

# 4. Verificar que RCON funciona
echo "🔍 [4/4] Verificando RCON (esperando 10 segundos para que inicie el servidor)..."
sleep 10
if docker exec minecraft-paper rcon-cli list 2>/dev/null; then
    echo "   ✅ RCON funciona correctamente"
else
    echo "   ⚠️  RCON no responde (el servidor puede estar iniciando)"
    echo "      Espera 30-60 segundos y prueba: docker exec minecraft-paper rcon-cli list"
fi
echo ""

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ✅ CORRECCIONES APLICADAS                                   ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "📋 Próximos pasos:"
echo ""
echo "1. Accede al panel web: http://localhost:5000"
echo ""
echo "2. Si ya tenías el panel abierto:"
echo "   - Presiona Ctrl + Shift + R (forzar recarga sin caché)"
echo "   - O cierra el navegador completamente y vuelve a abrir"
echo ""
echo "3. Haz login con tus credenciales"
echo ""
echo "4. Si usas contraseña sin hash:"
echo "   - Verás el dashboard cargarse"
echo "   - Después de 1 segundo aparecerá el modal de cambio de contraseña"
echo "   - Cambia tu contraseña"
echo "   - Vuelve a hacer login"
echo ""
echo "5. Prueba la consola:"
echo "   - Ve a la sección 'Consola'"
echo "   - Ejecuta: list"
echo "   - Deberías ver la lista de jugadores (o '0 players online')"
echo ""
echo "🔧 Si la consola no funciona, espera 1-2 minutos para que"
echo "   el servidor termine de iniciar y RCON se active."
echo ""
