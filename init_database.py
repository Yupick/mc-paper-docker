#!/usr/bin/env python3
"""Script para inicializar la BD universal con las tablas necesarias"""
import sqlite3
import sys
from pathlib import Path

db_path = Path("/home/mkd/contenedores/mc-paper-docker/config/data/universal.db")

print(f"Inicializando BD: {db_path}")

try:
    conn = sqlite3.connect(str(db_path))
    cursor = conn.cursor()
    
    # Tabla custom_mobs
    print("Creando tabla custom_mobs...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS custom_mobs (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            entity_type TEXT NOT NULL,
            health REAL NOT NULL,
            damage REAL DEFAULT 0,
            defense REAL DEFAULT 0,
            level INTEGER DEFAULT 1,
            experience_reward INTEGER DEFAULT 0,
            is_boss INTEGER DEFAULT 0,
            spawn_world TEXT,
            spawn_x REAL,
            spawn_y REAL,
            spawn_z REAL,
            attributes TEXT,
            created_at INTEGER
        )
    """)
    
    # Tabla mob_drops
    print("Creando tabla mob_drops...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS mob_drops (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            mob_id TEXT NOT NULL,
            item_type TEXT NOT NULL,
            min_amount INTEGER DEFAULT 1,
            max_amount INTEGER DEFAULT 1,
            drop_chance REAL DEFAULT 1.0,
            FOREIGN KEY (mob_id) REFERENCES custom_mobs(id)
        )
    """)
    
    # Tabla npcs
    print("Creando tabla npcs...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS npcs (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            type TEXT NOT NULL,
            entity_type TEXT DEFAULT 'VILLAGER',
            world TEXT NOT NULL,
            x REAL NOT NULL,
            y REAL NOT NULL,
            z REAL NOT NULL,
            yaw REAL,
            pitch REAL,
            quest_id TEXT,
            initial_dialogue_id TEXT,
            created_at INTEGER
        )
    """)
    
    # Tabla npc_dialogues
    print("Creando tabla npc_dialogues...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS npc_dialogues (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            npc_id TEXT NOT NULL,
            dialogue_id TEXT NOT NULL,
            lines_json TEXT NOT NULL,
            options_json TEXT,
            next_dialogue_id TEXT,
            FOREIGN KEY (npc_id) REFERENCES npcs(id),
            UNIQUE(npc_id, dialogue_id)
        )
    """)
    
    # Tabla quests
    print("Creando tabla quests...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS quests (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT,
            difficulty TEXT,
            min_level INTEGER,
            npc_giver_id TEXT,
            exp_reward INTEGER,
            money_reward REAL,
            skill_points_reward INTEGER,
            created_at INTEGER
        )
    """)
    
    # Tabla quest_objectives
    print("Creando tabla quest_objectives...")
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS quest_objectives (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            quest_id TEXT NOT NULL,
            objective_id TEXT NOT NULL,
            type TEXT NOT NULL,
            target TEXT,
            amount INTEGER,
            FOREIGN KEY (quest_id) REFERENCES quests(id),
            UNIQUE(quest_id, objective_id)
        )
    """)
    
    # Insertar un mob de prueba
    print("Insertando mob de prueba...")
    import time
    cursor.execute("""
        INSERT OR REPLACE INTO custom_mobs 
        (id, name, entity_type, health, damage, defense, level, experience_reward, is_boss, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, ('test_zombie', 'Zombie Guerrero', 'ZOMBIE', 100.0, 15.0, 5.0, 5, 50, 0, int(time.time())))
    
    cursor.execute("""
        INSERT OR REPLACE INTO mob_drops (mob_id, item_type, min_amount, max_amount, drop_chance)
        VALUES (?, ?, ?, ?, ?)
    """, ('test_zombie', 'DIAMOND', 1, 3, 0.1))
    
    # Insertar un NPC de prueba
    print("Insertando NPC de prueba...")
    cursor.execute("""
        INSERT OR REPLACE INTO npcs
        (id, name, type, entity_type, world, x, y, z, yaw, pitch, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, ('test_npc', 'Maestro de Misiones', 'QUEST_GIVER', 'VILLAGER', 'world', 0.0, 64.0, 0.0, 0.0, 0.0, int(time.time())))
    
    # Insertar una quest de prueba
    print("Insertando quest de prueba...")
    cursor.execute("""
        INSERT OR REPLACE INTO quests
        (id, name, description, difficulty, min_level, npc_giver_id, exp_reward, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, ('test_quest', 'Bienvenida', 'Quest de prueba', 'EASY', 1, 'test_npc', 100, int(time.time())))
    
    cursor.execute("""
        INSERT OR REPLACE INTO quest_objectives
        (quest_id, objective_id, type, target, amount)
        VALUES (?, ?, ?, ?, ?)
    """, ('test_quest', 'talk_1', 'TALK', 'Maestro de Misiones', 1))
    
    conn.commit()
    conn.close()
    
    print("\n✅ Base de datos inicializada correctamente")
    print(f"   - Tablas creadas: custom_mobs, mob_drops, npcs, npc_dialogues, quests, quest_objectives")
    print(f"   - Datos de prueba insertados: 1 mob, 1 NPC, 1 quest")
    
except Exception as e:
    print(f"\n❌ Error: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
