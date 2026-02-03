"""
Sprint 4: API Routes - Configuraciones Universales
Endpoints para gestión de configs (mobs, items, NPCs, etc.)
"""
from flask import Blueprint, jsonify, request
import json
import os

config_bp = Blueprint('config_api', __name__, url_prefix='/api/config')

# Directorio base de configuraciones
CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')

def load_config(config_name):
    """Carga un archivo de configuración JSON"""
    try:
        config_path = os.path.join(CONFIG_DIR, config_name)
        if os.path.exists(config_path):
            with open(config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        return None
    except Exception as e:
        return None

def save_config(config_name, data):
    """Guarda un archivo de configuración JSON"""
    try:
        config_path = os.path.join(CONFIG_DIR, config_name)
        os.makedirs(os.path.dirname(config_path), exist_ok=True)
        with open(config_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        return False

# ===================== MOBS ENDPOINTS =====================

@config_bp.route('/mobs', methods=['GET'])
def get_mobs():
    """GET /api/config/mobs - Lista todos los mobs"""
    data = load_config('mobs_config.json')
    if data:
        return jsonify({'success': True, 'mobs': data.get('mobs', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

@config_bp.route('/mobs', methods=['POST'])
def create_mob():
    """POST /api/config/mobs - Crear nuevo mob"""
    try:
        mob_data = request.json
        data = load_config('mobs_config.json') or {'mobs': []}
        
        # Verificar que no exista el ID
        if any(m.get('id') == mob_data.get('id') for m in data['mobs']):
            return jsonify({'success': False, 'error': 'Mob ID already exists'}), 400
        
        data['mobs'].append(mob_data)
        save_config('mobs_config.json', data)
        return jsonify({'success': True, 'mob': mob_data}), 201
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/mobs/<mob_id>', methods=['PUT'])
def update_mob(mob_id):
    """PUT /api/config/mobs/<id> - Actualizar mob"""
    try:
        mob_data = request.json
        data = load_config('mobs_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        # Buscar y actualizar mob
        for i, mob in enumerate(data['mobs']):
            if mob.get('id') == mob_id:
                data['mobs'][i] = mob_data
                save_config('mobs_config.json', data)
                return jsonify({'success': True, 'mob': mob_data})
        
        return jsonify({'success': False, 'error': 'Mob not found'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/mobs/<mob_id>', methods=['DELETE'])
def delete_mob(mob_id):
    """DELETE /api/config/mobs/<id> - Eliminar mob"""
    try:
        data = load_config('mobs_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        # Filtrar mob
        original_count = len(data['mobs'])
        data['mobs'] = [m for m in data['mobs'] if m.get('id') != mob_id]
        
        if len(data['mobs']) == original_count:
            return jsonify({'success': False, 'error': 'Mob not found'}), 404
        
        save_config('mobs_config.json', data)
        return jsonify({'success': True, 'message': 'Mob deleted'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/mobs/<world>', methods=['GET'])
def get_mobs_by_world(world):
    """GET /api/config/mobs/<world> - Mobs específicos de mundo"""
    # Intenta cargar config de mundo específico
    world_config_path = f'worlds/{world}/mobs_config.json'
    data = load_config(world_config_path)
    
    if not data:
        # Fallback a config universal
        data = load_config('mobs_config.json')
    
    if data:
        return jsonify({'success': True, 'world': world, 'mobs': data.get('mobs', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

# ===================== ITEMS ENDPOINTS =====================

@config_bp.route('/items', methods=['GET'])
def get_items():
    """GET /api/config/items - Lista todos los items"""
    data = load_config('crafting_config.json')
    if data:
        return jsonify({'success': True, 'items': data.get('items', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

@config_bp.route('/items', methods=['POST'])
def create_item():
    """POST /api/config/items - Crear nuevo item"""
    try:
        item_data = request.json
        data = load_config('crafting_config.json') or {'items': [], 'recipes': []}
        
        if any(i.get('id') == item_data.get('id') for i in data.get('items', [])):
            return jsonify({'success': False, 'error': 'Item ID already exists'}), 400
        
        data.setdefault('items', []).append(item_data)
        save_config('crafting_config.json', data)
        return jsonify({'success': True, 'item': item_data}), 201
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/items/<item_id>', methods=['PUT'])
def update_item(item_id):
    """PUT /api/config/items/<id> - Actualizar item"""
    try:
        item_data = request.json
        data = load_config('crafting_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        for i, item in enumerate(data.get('items', [])):
            if item.get('id') == item_id:
                data['items'][i] = item_data
                save_config('crafting_config.json', data)
                return jsonify({'success': True, 'item': item_data})
        
        return jsonify({'success': False, 'error': 'Item not found'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/items/<item_id>', methods=['DELETE'])
def delete_item(item_id):
    """DELETE /api/config/items/<id> - Eliminar item"""
    try:
        data = load_config('crafting_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        original_count = len(data.get('items', []))
        data['items'] = [i for i in data.get('items', []) if i.get('id') != item_id]
        
        if len(data['items']) == original_count:
            return jsonify({'success': False, 'error': 'Item not found'}), 404
        
        save_config('crafting_config.json', data)
        return jsonify({'success': True, 'message': 'Item deleted'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/items/<world>', methods=['GET'])
def get_items_by_world(world):
    """GET /api/config/items/<world> - Items específicos de mundo"""
    world_config_path = f'worlds/{world}/crafting_config.json'
    data = load_config(world_config_path) or load_config('crafting_config.json')
    
    if data:
        return jsonify({'success': True, 'world': world, 'items': data.get('items', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

# ===================== NPCS ENDPOINTS =====================

@config_bp.route('/npcs', methods=['GET'])
def get_npcs():
    """GET /api/config/npcs - Lista todos los NPCs"""
    # Buscar en plugin data
    data = load_config('plugin-data/npcs.json')
    if data:
        return jsonify({'success': True, 'npcs': data.get('npcs', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

@config_bp.route('/npcs', methods=['POST'])
def create_npc():
    """POST /api/config/npcs - Crear nuevo NPC"""
    try:
        npc_data = request.json
        data = load_config('plugin-data/npcs.json') or {'npcs': []}
        
        if any(n.get('id') == npc_data.get('id') for n in data['npcs']):
            return jsonify({'success': False, 'error': 'NPC ID already exists'}), 400
        
        data['npcs'].append(npc_data)
        save_config('plugin-data/npcs.json', data)
        return jsonify({'success': True, 'npc': npc_data}), 201
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/npcs/<npc_id>', methods=['PUT'])
def update_npc(npc_id):
    """PUT /api/config/npcs/<id> - Actualizar NPC"""
    try:
        npc_data = request.json
        data = load_config('plugin-data/npcs.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        for i, npc in enumerate(data['npcs']):
            if npc.get('id') == npc_id:
                data['npcs'][i] = npc_data
                save_config('plugin-data/npcs.json', data)
                return jsonify({'success': True, 'npc': npc_data})
        
        return jsonify({'success': False, 'error': 'NPC not found'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/npcs/<npc_id>', methods=['DELETE'])
def delete_npc(npc_id):
    """DELETE /api/config/npcs/<id> - Eliminar NPC"""
    try:
        data = load_config('plugin-data/npcs.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        original_count = len(data['npcs'])
        data['npcs'] = [n for n in data['npcs'] if n.get('id') != npc_id]
        
        if len(data['npcs']) == original_count:
            return jsonify({'success': False, 'error': 'NPC not found'}), 404
        
        save_config('plugin-data/npcs.json', data)
        return jsonify({'success': True, 'message': 'NPC deleted'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/npcs/<world>', methods=['GET'])
def get_npcs_by_world(world):
    """GET /api/config/npcs/<world> - NPCs específicos de mundo"""
    world_config_path = f'worlds/{world}/plugin-data/npcs.json'
    data = load_config(world_config_path) or load_config('plugin-data/npcs.json')
    
    if data:
        return jsonify({'success': True, 'world': world, 'npcs': data.get('npcs', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

# ===================== PETS ENDPOINTS =====================

@config_bp.route('/pets', methods=['GET'])
def get_pets():
    """GET /api/config/pets - Lista todas las mascotas"""
    data = load_config('pets_config.json')
    if data:
        return jsonify({'success': True, 'pets': data.get('pets', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

@config_bp.route('/pets', methods=['POST'])
def create_pet():
    """POST /api/config/pets - Crear nueva mascota"""
    try:
        pet_data = request.json
        data = load_config('pets_config.json') or {'pets': []}
        
        if any(p.get('id') == pet_data.get('id') for p in data['pets']):
            return jsonify({'success': False, 'error': 'Pet ID already exists'}), 400
        
        data['pets'].append(pet_data)
        save_config('pets_config.json', data)
        return jsonify({'success': True, 'pet': pet_data}), 201
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/pets/<pet_id>', methods=['PUT'])
def update_pet(pet_id):
    """PUT /api/config/pets/<id> - Actualizar mascota"""
    try:
        pet_data = request.json
        data = load_config('pets_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        for i, pet in enumerate(data['pets']):
            if pet.get('id') == pet_id:
                data['pets'][i] = pet_data
                save_config('pets_config.json', data)
                return jsonify({'success': True, 'pet': pet_data})
        
        return jsonify({'success': False, 'error': 'Pet not found'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/pets/<pet_id>', methods=['DELETE'])
def delete_pet(pet_id):
    """DELETE /api/config/pets/<id> - Eliminar mascota"""
    try:
        data = load_config('pets_config.json')
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        original_count = len(data['pets'])
        data['pets'] = [p for p in data['pets'] if p.get('id') != pet_id]
        
        if len(data['pets']) == original_count:
            return jsonify({'success': False, 'error': 'Pet not found'}), 404
        
        save_config('pets_config.json', data)
        return jsonify({'success': True, 'message': 'Pet deleted'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@config_bp.route('/pets/<world>', methods=['GET'])
def get_pets_by_world(world):
    """GET /api/config/pets/<world> - Pets específicos de mundo"""
    world_config_path = f'worlds/{world}/pets_config.json'
    data = load_config(world_config_path) or load_config('pets_config.json')
    
    if data:
        return jsonify({'success': True, 'world': world, 'pets': data.get('pets', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404
