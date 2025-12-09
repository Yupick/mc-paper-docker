#!/usr/bin/env python3
"""
Script de prueba para registrar kills de mobs en el panel web.
Simula que el plugin Java está registrando kills de jugadores.
"""

import requests
import json
from datetime import datetime
import time

# Configuración
API_URL = "http://localhost:5000"
USERNAME = "admin"
PASSWORD = "admin123"

# Mobs para probar
MOBS_TO_KILL = [
    {"mobId": "zombie_warrior", "displayName": "Guerrero Zombie", "xpReward": 150},
    {"mobId": "skeleton_archer", "displayName": "Arquero Esqueleto", "xpReward": 175},
    {"mobId": "ice_golem", "displayName": "Gólem de Hielo", "xpReward": 400},
    {"mobId": "elite_vindicator", "displayName": "Vengador de Élite", "xpReward": 700},
    {"mobId": "corrupted_dragon", "displayName": "Dragón Corrupto", "xpReward": 5000},
]

PLAYERS = ["Steve", "Alex", "Creeper", "Enderman"]

def login():
    """Autentica con el panel web"""
    session = requests.Session()
    
    # Obtener token CSRF (si es necesario)
    response = session.get(f"{API_URL}/login")
    
    # Intentar login
    login_data = {
        'username': USERNAME,
        'password': PASSWORD
    }
    response = session.post(f"{API_URL}/login", data=login_data)
    
    if response.status_code == 200:
        print("✅ Login exitoso")
        return session
    else:
        print(f"❌ Error en login: {response.status_code}")
        return None

def record_kill(session, player, mob):
    """Registra un kill en el servidor"""
    kill_data = {
        "playerName": player,
        "mobId": mob["mobId"],
        "mobName": mob["displayName"],
        "xpReward": mob["xpReward"],
        "world": "mmorpg",
        "location": {
            "x": 100,
            "y": 64,
            "z": 200
        }
    }
    
    try:
        response = session.post(
            f"{API_URL}/api/rpg/kill/record",
            json=kill_data
        )
        
        if response.status_code == 200:
            result = response.json()
            if result.get('success'):
                print(f"✅ {player} mató a {mob['displayName']} (+{mob['xpReward']} XP)")
                return True
            else:
                print(f"❌ Error: {result.get('message')}")
                return False
        else:
            print(f"❌ Error HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Error al registrar kill: {e}")
        return False

def get_quest_progress(session, player):
    """Obtiene progreso de quests del jugador"""
    try:
        response = session.get(
            f"{API_URL}/api/rpg/quest-progress",
            params={"player": player}
        )
        
        if response.status_code == 200:
            result = response.json()
            quests = result.get('quests', [])
            if quests:
                print(f"\n📊 Quests de {player}:")
                for quest in quests:
                    print(f"  - {quest['name']}")
                    for obj_id, progress in quest.get('progress', {}).items():
                        current = progress.get('current', 0)
                        required = progress.get('required', 0)
                        status = "✅" if progress.get('completed') else "⏳"
                        print(f"    {status} {current}/{required}")
            else:
                print(f"ℹ️ No hay quests con objetivos KILL_MOB")
    except Exception as e:
        print(f"❌ Error al obtener progreso: {e}")

def get_kills_stats(session):
    """Obtiene estadísticas de kills"""
    try:
        response = session.get(f"{API_URL}/api/rpg/kills")
        
        if response.status_code == 200:
            result = response.json()
            stats = result.get('playerStats', {})
            
            print("\n📈 Estadísticas de Kills:")
            for player, data in stats.items():
                total = data.get('totalKills', 0)
                xp = data.get('totalXpGained', 0)
                print(f"\n  👤 {player}:")
                print(f"    - Total kills: {total}")
                print(f"    - XP ganado: {xp}")
                print(f"    - Kills por mob:")
                for mob_id, count in data.get('killsByMob', {}).items():
                    print(f"      • {mob_id}: {count}")
    except Exception as e:
        print(f"❌ Error al obtener estadísticas: {e}")

def main():
    """Función principal"""
    print("🎮 Script de Prueba - MMORPG Kills Tracking\n")
    print(f"📌 API: {API_URL}")
    print(f"👤 Usuario: {USERNAME}\n")
    
    # Login
    session = login()
    if not session:
        print("❌ No se pudo autenticar. Abortando...")
        return
    
    # Simular kills
    print("\n🔫 Registrando kills de prueba...\n")
    
    kill_count = 0
    for player in PLAYERS:
        for mob in MOBS_TO_KILL[:3]:  # 3 primeros mobs
            if record_kill(session, player, mob):
                kill_count += 1
            time.sleep(0.5)
    
    print(f"\n✅ {kill_count} kills registrados\n")
    
    # Obtener estadísticas
    time.sleep(1)
    get_kills_stats(session)
    
    # Obtener progreso de quests
    print("\n🎯 Verificando progreso de quests...\n")
    for player in PLAYERS[:1]:  # Solo primer jugador
        get_quest_progress(session, player)
    
    print("\n✨ Prueba completada!")

if __name__ == "__main__":
    main()
