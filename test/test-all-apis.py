#!/usr/bin/env python3
"""
Script de prueba completa de todas las APIs del panel web
"""
import requests
import json

base_url = 'http://localhost:5000'

# Crear sesión
session = requests.Session()

# Login
print("=" * 60)
print("PROBANDO LOGIN")
print("=" * 60)
response = session.post(f'{base_url}/login', data={
    'username': 'admin',
    'password': 'minecraft123'
}, allow_redirects=False)
print(f"✓ Login Status: {response.status_code}")

# APIs a probar
apis = [
    ('GET', '/api/server/status', None, 'Estado del servidor'),
    ('GET', '/api/server/logs', None, 'Logs del servidor'),
    ('GET', '/api/server/players', None, 'Jugadores online'),
    ('GET', '/api/server/uptime', None, 'Tiempo de actividad'),
    ('GET', '/api/server/version', None, 'Versión del servidor'),
    ('GET', '/api/server/tps', None, 'TPS del servidor'),
    ('POST', '/api/server/command', {'command': 'list'}, 'Comando LIST'),
    ('POST', '/api/server/command', {'command': 'tps'}, 'Comando TPS'),
    ('POST', '/api/server/command', {'command': 'plugins'}, 'Comando PLUGINS'),
]

print("\n" + "=" * 60)
print("PROBANDO APIs DE SERVIDOR")
print("=" * 60)

for method, endpoint, data, description in apis:
    try:
        if method == 'GET':
            response = session.get(f'{base_url}{endpoint}')
        else:
            response = session.post(f'{base_url}{endpoint}', json=data)
        
        if response.status_code == 200:
            result = response.json()
            print(f"\n✓ {description} ({endpoint})")
            if 'output' in result:
                print(f"  Output: {result['output'][:100]}...")
            elif 'status' in result:
                print(f"  Status: {result['status']}")
            else:
                print(f"  Data: {json.dumps(result, indent=2)[:150]}...")
        else:
            print(f"\n✗ {description} ({endpoint})")
            print(f"  Status: {response.status_code}")
            print(f"  Error: {response.text[:100]}")
    except Exception as e:
        print(f"\n✗ {description} ({endpoint})")
        print(f"  Exception: {str(e)}")

print("\n" + "=" * 60)
print("PRUEBAS COMPLETADAS")
print("=" * 60)
