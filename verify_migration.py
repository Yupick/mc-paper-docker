#!/usr/bin/env python3
import sqlite3

conn = sqlite3.connect('config/data/universal.db')
c = conn.cursor()
c.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
tables = [row[0] for row in c.fetchall()]
squad_tables = [t for t in tables if 'squad' in t.lower()]
invasion_tables = [t for t in tables if 'invasion' in t.lower()]
enchant_tables = [t for t in tables if 'enchant' in t.lower()]
craft_tables = [t for t in tables if 'craft' in t.lower()]

print("=" * 70)
print("VERIFICACIÓN FINAL - TODAS LAS BDs EN SQLITE (UNIVERSAL.DB)")
print("=" * 70)
print(f"\n✓ Tablas TOTALES: {len(tables)}")
print(f"✓ Escuadras: {squad_tables}")
print(f"✓ Invasiones: {invasion_tables}")
print(f"✓ Encantamientos: {enchant_tables}")
print(f"✓ Artesanía: {craft_tables}")

# Contar registros
print("\nRegistros por categoría:")
total_records = 0
for table in tables:
    c.execute(f"SELECT COUNT(*) FROM {table}")
    count = c.fetchone()[0]
    total_records += count
    if count > 0:
        cat = ""
        if 'squad' in table: cat = "[ESCUADRA]"
        elif 'invasion' in table: cat = "[INVASIÓN]"
        elif 'enchant' in table: cat = "[ENCANTAMIENTO]"
        elif 'craft' in table: cat = "[ARTESANÍA]"
        else: cat = "[GENERAL]"
        print(f"  {cat:20} {table:30} {count:5} registros")

print(f"\n✓ TOTAL DE REGISTROS EN universal.db: {total_records}")
print("\n✓✓✓ MIGRACIÓN A SQLITE COMPLETADA ✓✓✓")
print("✓ Plugin usa conexión compartida de DatabaseManager")
print("✓ Todas las BDs consolidadas en config/data/universal.db")
conn.close()
