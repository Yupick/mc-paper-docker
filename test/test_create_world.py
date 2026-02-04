#!/usr/bin/env python3
import sys
sys.path.insert(0, '.')
from web.models.world_manager import WorldManager
from web.models.rpg_manager import RPGManager
import os

# Crear RPGManager primero
rpm = RPGManager(base_path='.')

# Crear WorldManager con RPGManager
wm = WorldManager(worlds_base_path='./worlds', rpg_manager=rpm)
print('Creando mundo RPG...')
try:
    world = wm.create_world(
        name='TestRPG',
        template='vanilla',
        is_rpg=True,
        rpg_config={'classesEnabled': True, 'questsEnabled': True, 'mobsEnabled': True, 'npcsEnabled': True}
    )
    print(f'✅ Mundo creado: {world.slug}')
    print(f'Verificando BD local...')
    db_path = f'worlds/{world.slug}/data/world_local.db'
    if os.path.exists(db_path):
        print(f'✅ BD local creada: {db_path}')
        # Ver tamaño
        size = os.path.getsize(db_path)
        print(f'  Tamaño: {size} bytes')
    else:
        print(f'❌ BD local NO encontrada: {db_path}')
except Exception as e:
    print(f'❌ Error: {e}')
    import traceback
    traceback.print_exc()
