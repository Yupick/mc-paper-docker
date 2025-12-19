#!/bin/bash

# Script para verificar y arreglar permisos de Docker
# Ejecutar: ./fix-docker-permissions.sh

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║   🔧 VERIFICACIÓN Y CORRECCIÓN DE PERMISOS DOCKER             ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# 1. Verificar si Docker está instalado
echo "🐳 [1/4] Verificando instalación de Docker..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado"
    echo ""
    echo "Instala Docker con:"
    echo "  ./install-dependencies.sh"
    exit 1
fi
echo "✅ Docker instalado: $(docker --version)"
echo ""

# 2. Verificar si Docker está corriendo
echo "🔍 [2/4] Verificando servicio de Docker..."
if ! sudo systemctl is-active --quiet docker; then
    echo "⚠️  Docker no está corriendo. Iniciando..."
    sudo systemctl start docker
    sudo systemctl enable docker
    echo "✅ Docker iniciado"
else
    echo "✅ Docker está corriendo"
fi
echo ""

# 3. Verificar permisos del usuario
echo "👤 [3/4] Verificando permisos de usuario..."
echo "   Usuario actual: $USER"
echo "   Grupos actuales: $(groups)"
echo ""

if groups | grep -q docker; then
    echo "✅ Tu usuario YA está en el grupo 'docker'"
    echo ""
    
    # Verificar si puede conectarse a Docker
    if docker ps &>/dev/null; then
        echo "✅ Puedes usar Docker sin problemas"
        echo ""
        echo "╔═══════════════════════════════════════════════════════════════╗"
        echo "║  ✅ TODO ESTÁ CONFIGURADO CORRECTAMENTE                        ║"
        echo "╚═══════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Puedes ejecutar:"
        echo "  ./start-web-panel.sh"
        exit 0
    else
        echo "⚠️  Estás en el grupo docker pero los permisos no se han aplicado"
        echo ""
        echo "╔═══════════════════════════════════════════════════════════════╗"
        echo "║  ⚠️  REINICIA TU SESIÓN PARA APLICAR PERMISOS                  ║"
        echo "╚═══════════════════════════════════════════════════════════════╝"
        echo ""
        echo "Opción 1 (Recomendada):"
        echo "  logout"
        echo "  # Vuelve a conectarte por SSH y ejecuta ./start-web-panel.sh"
        echo ""
        echo "Opción 2 (Temporal):"
        echo "  newgrp docker"
        echo "  ./start-web-panel.sh"
        echo ""
        exit 0
    fi
else
    echo "⚠️  Tu usuario NO está en el grupo 'docker'"
    echo ""
    echo "[4/4] Agregando usuario al grupo docker..."
    sudo usermod -aG docker $USER
    
    if [ $? -eq 0 ]; then
        echo "✅ Usuario agregado al grupo docker exitosamente"
        echo ""
        echo "╔═══════════════════════════════════════════════════════════════╗"
        echo "║  ⚠️  REINICIA TU SESIÓN PARA APLICAR PERMISOS                  ║"
        echo "╚═══════════════════════════════════════════════════════════════╝"
        echo ""
        echo "IMPORTANTE: Los cambios de grupo requieren reiniciar la sesión."
        echo ""
        echo "Opciones:"
        echo ""
        echo "  Opción 1 - Reiniciar sesión SSH (Recomendada):"
        echo "    logout"
        echo "    # Vuelve a conectarte y ejecuta: ./start-web-panel.sh"
        echo ""
        echo "  Opción 2 - Aplicar temporalmente en esta sesión:"
        echo "    newgrp docker"
        echo "    ./start-web-panel.sh"
        echo ""
        echo "  Opción 3 - Reiniciar el servidor:"
        echo "    sudo reboot"
        echo ""
    else
        echo "❌ Error al agregar usuario al grupo docker"
        echo ""
        echo "Intenta manualmente:"
        echo "  sudo usermod -aG docker $USER"
        exit 1
    fi
fi
