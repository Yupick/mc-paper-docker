#!/usr/bin/env python3
"""
Script para probar que los endpoints RPG funcionan correctamente
después de los cambios en la resolución de rutas.
"""

import sys
import os
import json
from pathlib import Path

# Agregar el directorio web al path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'web'))

# Importar la app
from app import app, _resolve_rpg_file_path

def test_resolve_paths():
    """Probar que las rutas se resuelven correctamente"""
    print("\n=== TEST: Resolución de Rutas ===")
    
    with app.app_context():
        files = ['npcs.json', 'quests.json', 'mobs.json']
        
        for filename in files:
            path = _resolve_rpg_file_path(filename)
            exists = os.path.exists(path)
            
            status = "✓" if exists else "✗"
            print(f"{status} {filename}")
            print(f"   Ruta: {path}")
            print(f"   Existe: {exists}")
            
            if exists:
                size = os.path.getsize(path)
                print(f"   Tamaño: {size} bytes")
                
                # Verificar JSON válido
                try:
                    with open(path) as f:
                        data = json.load(f)
                        if isinstance(data, dict):
                            if 'npcs' in data:
                                print(f"   Contenido: {len(data['npcs'])} NPCs")
                            elif 'quests' in data:
                                print(f"   Contenido: {len(data['quests'])} Quests")
                            else:
                                print(f"   Contenido: {len(data)} Mobs")
                except Exception as e:
                    print(f"   Error JSON: {e}")
            print()

def test_endpoints():
    """Probar que los endpoints retornan datos"""
    print("\n=== TEST: Endpoints API ===")
    
    with app.test_client() as client:
        endpoints = [
            ('/api/rpg/npcs', 'npcs'),
            ('/api/rpg/quests', 'quests'),
            ('/api/rpg/mobs', 'mobs'),
        ]
        
        for endpoint, key in endpoints:
            # Hacer request al endpoint
            response = client.get(endpoint)
            
            # Determinar si fue exitoso (ignorar 401 de autenticación)
            if response.status_code == 401:
                print(f"⚠ {endpoint}: Requiere autenticación (esperado)")
                continue
            
            try:
                data = json.loads(response.data)
                
                if 'success' in data and data['success']:
                    count = len(data.get(key, []))
                    status = "✓"
                    print(f"{status} {endpoint}")
                    print(f"   Respuesta: {count} {key}")
                else:
                    print(f"✗ {endpoint}")
                    print(f"   Error: {data.get('message', 'Unknown error')}")
            except Exception as e:
                print(f"✗ {endpoint}")
                print(f"   Error al parsear JSON: {e}")
                print(f"   Status: {response.status_code}")
            print()

if __name__ == '__main__':
    print("=" * 50)
    print("PRUEBAS DEL SISTEMA RPG")
    print("=" * 50)
    
    test_resolve_paths()
    test_endpoints()
    
    print("\n" + "=" * 50)
    print("PRUEBAS COMPLETADAS")
    print("=" * 50)
