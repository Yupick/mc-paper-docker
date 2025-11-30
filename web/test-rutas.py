#!/usr/bin/env python3
"""
Script de prueba para verificar que las rutas relativas funcionan correctamente
"""
import os
import sys

# Agregar el directorio actual al path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from dotenv import load_dotenv

# Cargar variables de entorno
load_dotenv()

# Calcular BASE_DIR de la misma forma que app.py
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MINECRAFT_DIR = os.getenv('MINECRAFT_DIR', BASE_DIR)

print("╔═══════════════════════════════════════════════════════════════╗")
print("║   🔍 VERIFICACIÓN DE RUTAS - PANEL WEB MINECRAFT             ║")
print("╚═══════════════════════════════════════════════════════════════╝")
print()

print("📁 Rutas calculadas:")
print(f"   BASE_DIR:      {BASE_DIR}")
print(f"   MINECRAFT_DIR: {MINECRAFT_DIR}")
print()

print("📂 Directorios esperados:")
directories = {
    'plugins': os.path.join(MINECRAFT_DIR, 'plugins'),
    'worlds': os.path.join(MINECRAFT_DIR, 'worlds'),
    'config': os.path.join(MINECRAFT_DIR, 'config'),
    'backups': os.path.join(MINECRAFT_DIR, 'backups'),
    'logs': os.path.join(MINECRAFT_DIR, 'logs'),
}

all_ok = True
for name, path in directories.items():
    exists = "✅" if os.path.exists(path) else "❌"
    print(f"   {exists} {name:15} → {path}")
    if not os.path.exists(path):
        all_ok = False

print()
print("📄 Archivos esperados:")
files = {
    'docker-compose.yml': os.path.join(MINECRAFT_DIR, 'docker-compose.yml'),
    'update.sh': os.path.join(MINECRAFT_DIR, 'update.sh'),
    'server.properties': os.path.join(MINECRAFT_DIR, 'config', 'server.properties'),
}

for name, path in files.items():
    exists = "✅" if os.path.exists(path) else "❌"
    print(f"   {exists} {name:20} → {path}")
    if not os.path.exists(path):
        all_ok = False

print()
print("🐳 Docker:")
container_name = os.getenv('DOCKER_CONTAINER_NAME', 'mc-paper')
print(f"   Nombre contenedor: {container_name}")

print()
if all_ok:
    print("✅ TODAS LAS RUTAS ESTÁN CORRECTAS")
else:
    print("⚠️  ALGUNAS RUTAS NO EXISTEN (pueden crearse al iniciar el servidor)")

print()
print("💡 Esta configuración funciona en cualquier ubicación porque usa rutas relativas.")
print()
