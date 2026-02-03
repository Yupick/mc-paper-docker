"""
Sprint 4: Respawn Configuration API
Endpoints para gestión de zonas de respawn
"""

from flask import Blueprint, request, jsonify
import json
import os

respawn_bp = Blueprint('respawn', __name__, url_prefix='/api/config/respawn')

CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
RESPAWN_CONFIG = os.path.join(CONFIG_DIR, 'respawn_config.json')

def load_respawn_config():
    """Carga la configuración de respawn"""
    try:
        if os.path.exists(RESPAWN_CONFIG):
            with open(RESPAWN_CONFIG, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {"zones": [], "globalSettings": {}}
    except Exception as e:
        print(f"Error loading respawn config: {e}")
        return {"zones": [], "globalSettings": {}}

def save_respawn_config(config):
    """Guarda la configuración de respawn"""
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(RESPAWN_CONFIG, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving respawn config: {e}")
        return False

@respawn_bp.route('', methods=['GET'])
def get_all_zones():
    """GET /api/config/respawn - Obtiene todas las zonas de respawn"""
    try:
        config = load_respawn_config()
        return jsonify({'success': True, 'config': config.get('zones', []), 'globalSettings': config.get('globalSettings', {})})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('', methods=['POST'])
def create_zone():
    """POST /api/config/respawn - Crea una nueva zona de respawn"""
    try:
        new_zone = request.get_json()
        
        if not new_zone or 'zoneId' not in new_zone:
            return jsonify({'success': False, 'error': 'zoneId es requerido'}), 400
        
        config = load_respawn_config()
        zones = config.get('zones', [])
        
        existing = next((z for z in zones if z.get('zoneId') == new_zone['zoneId']), None)
        if existing:
            return jsonify({'success': False, 'error': 'Zona ya existe'}), 400
        
        zones.append(new_zone)
        config['zones'] = zones
        
        if save_respawn_config(config):
            return jsonify({'success': True, 'message': 'Zona creada', 'zone': new_zone}), 201
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('/<zone_id>', methods=['GET'])
def get_zone(zone_id):
    """GET /api/config/respawn/<id> - Obtiene una zona específica"""
    try:
        config = load_respawn_config()
        zones = config.get('zones', [])
        zone = next((z for z in zones if z.get('zoneId') == zone_id), None)
        
        if zone:
            return jsonify({'success': True, 'zone': zone})
        else:
            return jsonify({'success': False, 'error': 'Zona no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('/<zone_id>', methods=['PUT'])
def update_zone(zone_id):
    """PUT /api/config/respawn/<id> - Actualiza una zona"""
    try:
        updated_zone = request.get_json()
        
        if not updated_zone:
            return jsonify({'success': False, 'error': 'Datos requeridos'}), 400
        
        config = load_respawn_config()
        zones = config.get('zones', [])
        
        found = False
        for i, zone in enumerate(zones):
            if zone.get('zoneId') == zone_id:
                zones[i] = updated_zone
                found = True
                break
        
        if not found:
            return jsonify({'success': False, 'error': 'Zona no encontrada'}), 404
        
        config['zones'] = zones
        
        if save_respawn_config(config):
            return jsonify({'success': True, 'message': 'Zona actualizada', 'zone': updated_zone})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('/<zone_id>', methods=['DELETE'])
def delete_zone(zone_id):
    """DELETE /api/config/respawn/<id> - Elimina una zona"""
    try:
        config = load_respawn_config()
        zones = config.get('zones', [])
        
        original_length = len(zones)
        zones = [z for z in zones if z.get('zoneId') != zone_id]
        
        if len(zones) == original_length:
            return jsonify({'success': False, 'error': 'Zona no encontrada'}), 404
        
        config['zones'] = zones
        
        if save_respawn_config(config):
            return jsonify({'success': True, 'message': 'Zona eliminada'})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('/world/<world_name>', methods=['GET'])
def get_zones_by_world(world_name):
    """GET /api/config/respawn/world/<world> - Obtiene zonas por mundo"""
    try:
        config = load_respawn_config()
        zones = config.get('zones', [])
        world_zones = [z for z in zones if z.get('world') == world_name]
        
        return jsonify({'success': True, 'config': world_zones, 'world': world_name})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@respawn_bp.route('/settings', methods=['GET', 'PUT'])
def manage_global_settings():
    """GET/PUT /api/config/respawn/settings - Gestiona configuración global"""
    try:
        if request.method == 'GET':
            config = load_respawn_config()
            return jsonify({'success': True, 'settings': config.get('globalSettings', {})})
        else:  # PUT
            new_settings = request.get_json()
            config = load_respawn_config()
            config['globalSettings'] = new_settings
            
            if save_respawn_config(config):
                return jsonify({'success': True, 'message': 'Configuración actualizada', 'settings': new_settings})
            else:
                return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
