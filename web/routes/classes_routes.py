"""
Sprint 4: Classes Configuration API
Endpoints para gestión de clases RPG
"""

from flask import Blueprint, request, jsonify
import json
import os

classes_bp = Blueprint('classes', __name__, url_prefix='/api/config/classes')

CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
CLASSES_DIR = os.path.join(CONFIG_DIR, 'classes')

def load_classes_config():
    """Carga todas las clases"""
    try:
        if not os.path.exists(CLASSES_DIR):
            return []
        
        classes = []
        for filename in os.listdir(CLASSES_DIR):
            if filename.endswith('.json'):
                filepath = os.path.join(CLASSES_DIR, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    class_data = json.load(f)
                    classes.append(class_data)
        return classes
    except Exception as e:
        print(f"Error loading classes: {e}")
        return []

def save_class_config(class_data):
    """Guarda una clase individual"""
    try:
        os.makedirs(CLASSES_DIR, exist_ok=True)
        class_id = class_data.get('classId', 'unknown')
        filepath = os.path.join(CLASSES_DIR, f"{class_id}.json")
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(class_data, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving class: {e}")
        return False

def delete_class_file(class_id):
    """Elimina archivo de clase"""
    try:
        filepath = os.path.join(CLASSES_DIR, f"{class_id}.json")
        if os.path.exists(filepath):
            os.remove(filepath)
            return True
        return False
    except Exception as e:
        print(f"Error deleting class: {e}")
        return False

@classes_bp.route('', methods=['GET'])
def get_all_classes():
    """GET /api/config/classes - Obtiene todas las clases"""
    try:
        classes = load_classes_config()
        return jsonify({'success': True, 'config': classes})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@classes_bp.route('', methods=['POST'])
def create_class():
    """POST /api/config/classes - Crea una nueva clase"""
    try:
        new_class = request.get_json()
        
        if not new_class or 'classId' not in new_class:
            return jsonify({'success': False, 'error': 'classId es requerido'}), 400
        
        # Verificar si existe
        classes = load_classes_config()
        existing = next((c for c in classes if c.get('classId') == new_class['classId']), None)
        if existing:
            return jsonify({'success': False, 'error': 'Clase ya existe'}), 400
        
        if save_class_config(new_class):
            return jsonify({'success': True, 'message': 'Clase creada', 'class': new_class}), 201
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@classes_bp.route('/<class_id>', methods=['GET'])
def get_class(class_id):
    """GET /api/config/classes/<id> - Obtiene una clase específica"""
    try:
        classes = load_classes_config()
        class_data = next((c for c in classes if c.get('classId') == class_id), None)
        
        if class_data:
            return jsonify({'success': True, 'class': class_data})
        else:
            return jsonify({'success': False, 'error': 'Clase no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@classes_bp.route('/<class_id>', methods=['PUT'])
def update_class(class_id):
    """PUT /api/config/classes/<id> - Actualiza una clase"""
    try:
        updated_class = request.get_json()
        
        if not updated_class:
            return jsonify({'success': False, 'error': 'Datos requeridos'}), 400
        
        # Verificar que existe
        classes = load_classes_config()
        existing = next((c for c in classes if c.get('classId') == class_id), None)
        
        if not existing:
            return jsonify({'success': False, 'error': 'Clase no encontrada'}), 404
        
        if save_class_config(updated_class):
            return jsonify({'success': True, 'message': 'Clase actualizada', 'class': updated_class})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@classes_bp.route('/<class_id>', methods=['DELETE'])
def delete_class(class_id):
    """DELETE /api/config/classes/<id> - Elimina una clase"""
    try:
        if delete_class_file(class_id):
            return jsonify({'success': True, 'message': 'Clase eliminada'})
        else:
            return jsonify({'success': False, 'error': 'Clase no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@classes_bp.route('/type/<class_type>', methods=['GET'])
def get_classes_by_type(class_type):
    """GET /api/config/classes/type/<type> - Obtiene clases por tipo"""
    try:
        classes = load_classes_config()
        filtered = [c for c in classes if c.get('type') == class_type]
        
        return jsonify({'success': True, 'config': filtered, 'type': class_type})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
