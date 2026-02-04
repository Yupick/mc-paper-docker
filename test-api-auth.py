#!/usr/bin/env python3
import requests

base_url = 'http://localhost:5000'

# Crear sesión
session = requests.Session()

# Login
response = session.post(f'{base_url}/login', data={
    'username': 'admin',
    'password': 'minecraft123'
}, allow_redirects=False)

print(f"Login Status: {response.status_code}")
print(f"Login Headers: {response.headers.get('Location', 'No redirect')}")

# Intentar ejecutar comando
response = session.post(f'{base_url}/api/server/command', json={
    'command': 'list'
})

print(f"\nCommand Status: {response.status_code}")
print(f"Command Response: {response.json()}")
