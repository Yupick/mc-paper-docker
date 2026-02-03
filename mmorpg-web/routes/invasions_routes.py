"""
Sprint 4: Invasions Configuration API
Endpoints para gestión de invasiones
"""

from flask import Blueprint, request, jsonify
import json
import os

invasions_bp = Blueprint('invasions', __name__, url_prefix='/api/config/invasions')

CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
INVASIONS_CONFIG = os.path.join(CONFIG_DIR, 'invasions_config.json')

def load_invasions_config():
    """Carga la configuración de invasiones"""
    try:
        if os.path.exists(INVASIONS_CONFIG):
            with open(INVASIONS_CONFIG, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {"invasions": []}
    except Exception as e:
        print(f"Error loading invasions config: {e}")
        return {"invasions": []}

def save_invasions_config(config):
    """Guarda la configuración de invasiones"""
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(INVASIONS_CONFIG, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving invasions config: {e}")
        return False

@invasions_bp.route('', methods=['GET'])
def get_all_invasions():
    """GET /api/config/invasions - Obtiene todas las invasiones"""
    try:
        config = load_invasions_config()
        return jsonify({
            'success': True,
            'config': config.get('invasions', [])
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@invasions_bp.route('', methods=['POST'])
def create_invasion():
    """POST /api/config/invasions - Crea una nueva invasión"""
    try:
        new_invasion = request.get_json()
        
        if not new_invasion or 'invasionId' not in new_invasion:
            return jsonify({'success': False, 'error': 'invasionId es requerido'}), 400
        
        config = load_invasions_config()
        invasions = config.get('invasions', [])
        
        existing = next((i for i in invasions if i.get('invasionId') == new_invasion['invasionId']), None)
        if existing:
            return jsonify({'success': False, 'error': 'Invasión con ese ID ya existe'}), 400
        
        invasions.append(new_invasion)
        config['invasions'] = invasions
        
        if save_invasions_config(config):
            return jsonify({'success': True, 'message': 'Invasión creada', 'invasion': new_invasion}), 201
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@invasions_bp.route('/<invasion_id>', methods=['GET'])
def get_invasion(invasion_id):
    """GET /api/config/invasions/<id> - Obtiene una invasión específica"""
    try:
        config = load_invasions_config()
        invasions = config.get('invasions', [])
        invasion = next((i for i in invasions if i.get('invasionId') == invasion_id), None)
        
        if invasion:
            return jsonify({'success': True, 'invasion': invasion})
        else:
            return jsonify({'success': False, 'error': 'Invasión no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@invasions_bp.route('/<invasion_id>', methods=['PUT'])
def update_invasion(invasion_id):
    """PUT /api/config/invasions/<id> - Actualiza una invasión"""
    try:
        updated_invasion = request.get_json()
        
        if not updated_invasion:
            return jsonify({'success': False, 'error': 'Datos requeridos'}), 400
        
        config = load_invasions_config()
        invasions = config.get('invasions', [])
        
        found = False
        for i, invasion in enumerate(invasions):
            if invasion.get('invasionId') == invasion_id:
                invasions[i] = updated_invasion
                found = True
                break
        
        if not found:
            return jsonify({'success': False, 'error': 'Invasión no encontrada'}), 404
        
        config['invasions'] = invasions
        
        if save_invasions_config(config):
            return jsonify({'success': True, 'message': 'Invasión actualizada', 'invasion': updated_invasion})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@invasions_bp.route('/<invasion_id>', methods=['DELETE'])
def delete_invasion(invasion_id):
    """DELETE /api/config/invasions/<id> - Elimina una invasión"""
    try:
        config = load_invasions_config()
        invasions = config.get('invasions', [])
        
        original_length = len(invasions)
        invasions = [i for i in invasions if i.get('invasionId') != invasion_id]
        
        if len(invasions) == original_length:
            return jsonify({'success': False, 'error': 'Invasión no encontrada'}), 404
        
        config['invasions'] = invasions
        
        if save_invasions_config(config):
            return jsonify({'success': True, 'message': 'Invasión eliminada'})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@invasions_bp.route('/world/<world_name>', methods=['GET'])
def get_invasions_by_world(world_name):
    """GET /api/config/invasions/world/<world> - Obtiene invasiones de un mundo"""
    try:
        config = load_invasions_config()
        invasions = config.get('invasions', [])
        world_invasions = [i for i in invasions if i.get('world') == world_name]
        
        return jsonify({'success': True, 'config': world_invasions, 'world': world_name})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
