#!/bin/bash

# Script rápido para corregir solo el panel web (sin reconstruir Docker)

echo "🔄 Corrección rápida del panel web..."
echo ""

echo "✅ Nombre del contenedor corregido en .env: minecraft-paper"
echo ""

# Reiniciar panel web
echo "🌐 Reiniciando panel web..."
cd /home/mkd/contenedores/mc-paper/web
./stop-web-panel.sh 2>/dev/null || true
sleep 2
./start-web-panel.sh
echo "   ✅ Panel web reiniciado"
echo ""

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   ✅ PANEL WEB CORREGIDO                                      ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "⚠️  IMPORTANTE: Para que la consola funcione, necesitas instalar mcrcon"
echo ""
echo "Ejecuta este comando para instalar mcrcon en el contenedor:"
echo ""
echo "  docker exec -u root minecraft-paper bash -c 'cd /tmp && wget https://github.com/Tiiffi/mcrcon/releases/download/v0.7.2/mcrcon-0.7.2-linux-x86-64.tar.gz && tar -xzf mcrcon-0.7.2-linux-x86-64.tar.gz && mv mcrcon /usr/local/bin/ && chmod +x /usr/local/bin/mcrcon'"
echo ""
echo "Luego prueba:"
echo "  docker exec minecraft-paper mcrcon -H localhost -P 25575 -p minecraft123 list"
echo ""
