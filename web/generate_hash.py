#!/usr/bin/env python3
"""
Script para generar hash de contraseña para el panel de administración
"""

from werkzeug.security import generate_password_hash
import sys

if len(sys.argv) > 1:
    password = sys.argv[1]
else:
    password = input("Ingresa la contraseña a hashear: ")

hash_result = generate_password_hash(password)

print("\n" + "="*70)
print("🔐 HASH DE CONTRASEÑA GENERADO")
print("="*70)
print(f"\nContraseña: {password}")
print(f"Hash: {hash_result}")
print("\n" + "="*70)
print("📝 INSTRUCCIONES:")
print("="*70)
print("\n1. Copia el hash de arriba")
print("2. Edita el archivo .env:")
print("   nano /home/mkd/contenedores/mc-paper/web/.env")
print("\n3. Agrega o modifica esta línea:")
print(f"   ADMIN_PASSWORD_HASH={hash_result}")
print("\n4. Guarda y reinicia el servidor web")
print("="*70 + "\n")
