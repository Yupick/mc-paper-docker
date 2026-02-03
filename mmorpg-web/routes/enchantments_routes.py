"""
Sprint 4: Enchantments Configuration API
Endpoints para gestión de encantamientos
"""

from flask import Blueprint, request, jsonify
import json
import os

enchantments_bp = Blueprint('enchantments', __name__, url_prefix='/api/config/enchantments')

CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
ENCHANTMENTS_CONFIG = os.path.join(CONFIG_DIR, 'enchantments_config.json')

def load_enchantments_config():
    """Carga la configuración de encantamientos"""
    try:
        if os.path.exists(ENCHANTMENTS_CONFIG):
            with open(ENCHANTMENTS_CONFIG, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {"enchantments": []}
    except Exception as e:
        print(f"Error loading enchantments: {e}")
        return {"enchantments": []}

def save_enchantments_config(config):
    """Guarda la configuración de encantamientos"""
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(ENCHANTMENTS_CONFIG, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving enchantments: {e}")
        return False

@enchantments_bp.route('', methods=['GET'])
def get_all_enchantments():
    """GET /api/config/enchantments - Obtiene todos los encantamientos"""
    try:
        config = load_enchantments_config()
        return jsonify({'success': True, 'config': config.get('enchantments', [])})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@enchantments_bp.route('', methods=['POST'])
def create_enchantment():
    """POST /api/config/enchantments - Crea un nuevo encantamiento"""
    try:
        new_enchantment = request.get_json()
        
        if not new_enchantment or 'enchantmentId' not in new_enchantment:
            return jsonify({'success': False, 'error': 'enchantmentId es requerido'}), 400
        
        config = load_enchantments_config()
        enchantments = config.get('enchantments', [])
        
        existing = next((e for e in enchantments if e.get('enchantmentId') == new_enchantment['enchantmentId']), None)
        if existing:
            return jsonify({'success': False, 'error': 'Encantamiento ya existe'}), 400
        
        enchantments.append(new_enchantment)
        config['enchantments'] = enchantments
        
        if save_enchantments_config(config):
            return jsonify({'success': True, 'message': 'Encantamiento creado', 'enchantment': new_enchantment}), 201
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@enchantments_bp.route('/<enchantment_id>', methods=['GET'])
def get_enchantment(enchantment_id):
    """GET /api/config/enchantments/<id> - Obtiene un encantamiento específico"""
    try:
        config = load_enchantments_config()
        enchantments = config.get('enchantments', [])
        enchantment = next((e for e in enchantments if e.get('enchantmentId') == enchantment_id), None)
        
        if enchantment:
            return jsonify({'success': True, 'enchantment': enchantment})
        else:
            return jsonify({'success': False, 'error': 'Encantamiento no encontrado'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@enchantments_bp.route('/<enchantment_id>', methods=['PUT'])
def update_enchantment(enchantment_id):
    """PUT /api/config/enchantments/<id> - Actualiza un encantamiento"""
    try:
        updated_enchantment = request.get_json()
        
        if not updated_enchantment:
            return jsonify({'success': False, 'error': 'Datos requeridos'}), 400
        
        config = load_enchantments_config()
        enchantments = config.get('enchantments', [])
        
        found = False
        for i, enchantment in enumerate(enchantments):
            if enchantment.get('enchantmentId') == enchantment_id:
                enchantments[i] = updated_enchantment
                found = True
                break
        
        if not found:
            return jsonify({'success': False, 'error': 'Encantamiento no encontrado'}), 404
        
        config['enchantments'] = enchantments
        
        if save_enchantments_config(config):
            return jsonify({'success': True, 'message': 'Encantamiento actualizado', 'enchantment': updated_enchantment})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@enchantments_bp.route('/<enchantment_id>', methods=['DELETE'])
def delete_enchantment(enchantment_id):
    """DELETE /api/config/enchantments/<id> - Elimina un encantamiento"""
    try:
        config = load_enchantments_config()
        enchantments = config.get('enchantments', [])
        
        original_length = len(enchantments)
        enchantments = [e for e in enchantments if e.get('enchantmentId') != enchantment_id]
        
        if len(enchantments) == original_length:
            return jsonify({'success': False, 'error': 'Encantamiento no encontrado'}), 404
        
        config['enchantments'] = enchantments
        
        if save_enchantments_config(config):
            return jsonify({'success': True, 'message': 'Encantamiento eliminado'})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@enchantments_bp.route('/category/<category>', methods=['GET'])
def get_enchantments_by_category(category):
    """GET /api/config/enchantments/category/<category> - Obtiene encantamientos por categoría"""
    try:
        config = load_enchantments_config()
        enchantments = config.get('enchantments', [])
        filtered = [e for e in enchantments if e.get('category') == category]
        
        return jsonify({'success': True, 'config': filtered, 'category': category})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
