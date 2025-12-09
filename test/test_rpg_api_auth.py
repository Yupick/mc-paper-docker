#!/usr/bin/env python3
"""
Script para probar los endpoints RPG con autenticación simulada
"""

import sys
import os
import json
import requests
from requests.auth import HTTPBasicAuth

# Configurar credenciales
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "minecraft123"
BASE_URL = "http://localhost:5000"

def test_endpoints():
    """Probar endpoints con sesión autenticada"""
    print("\n=== PRUEBA DE ENDPOINTS CON AUTENTICACIÓN ===\n")
    
    session = requests.Session()
    
    # Primero hacer login
    print("1. Intentando login...")
    login_data = {
        'username': ADMIN_USERNAME,
        'password': ADMIN_PASSWORD
    }
    
    response = session.post(f"{BASE_URL}/login", data=login_data, allow_redirects=True)
    
    if response.status_code == 200:
        print("   ✓ Login exitoso")
    else:
        print(f"   ✗ Error en login (Status: {response.status_code})")
        return
    
    # Ahora probar los endpoints RPG
    endpoints = [
        ('/api/rpg/npcs', 'npcs'),
        ('/api/rpg/quests', 'quests'),
        ('/api/rpg/mobs', 'mobs'),
    ]
    
    print("\n2. Probando endpoints RPG...\n")
    
    for endpoint, key in endpoints:
        try:
            response = session.get(f"{BASE_URL}{endpoint}")
            
            if response.status_code == 200:
                data = response.json()
                
                if data.get('success'):
                    count = len(data.get(key, []))
                    print(f"✓ GET {endpoint}")
                    print(f"  Status: 200")
                    print(f"  Datos: {count} {key}")
                    
                    # Mostrar primero elemento
                    if count > 0:
                        first = data[key][0]
                        if 'id' in first:
                            print(f"  Primer elemento ID: {first['id']}")
                        elif 'name' in first:
                            print(f"  Primer elemento: {first['name']}")
                else:
                    print(f"✗ GET {endpoint}")
                    print(f"  Error: {data.get('message', 'Unknown')}")
            else:
                print(f"✗ GET {endpoint}")
                print(f"  Status: {response.status_code}")
                print(f"  Response: {response.text[:100]}")
        except Exception as e:
            print(f"✗ GET {endpoint}")
            print(f"  Exception: {e}")
        
        print()

if __name__ == '__main__':
    print("=" * 50)
    print("PRUEBA DE ENDPOINTS RPG")
    print("=" * 50)
    print(f"URL Base: {BASE_URL}")
    print(f"Usuario: {ADMIN_USERNAME}")
    
    test_endpoints()
    
    print("=" * 50)
    print("PRUEBA COMPLETADA")
    print("=" * 50)
