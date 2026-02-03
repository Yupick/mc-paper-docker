"""
Sprint 4: Dungeons Configuration API
Endpoints para gestión de dungeons (mazmorras)
"""

from flask import Blueprint, request, jsonify
import json
import os

dungeons_bp = Blueprint('dungeons', __name__, url_prefix='/api/config/dungeons')

# Configuración de rutas
CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
DUNGEONS_CONFIG = os.path.join(CONFIG_DIR, 'dungeons_config.json')

def load_dungeons_config():
    """Carga la configuración de dungeons"""
    try:
        if os.path.exists(DUNGEONS_CONFIG):
            with open(DUNGEONS_CONFIG, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {"dungeons": []}
    except Exception as e:
        print(f"Error loading dungeons config: {e}")
        return {"dungeons": []}

def save_dungeons_config(config):
    """Guarda la configuración de dungeons"""
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(DUNGEONS_CONFIG, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving dungeons config: {e}")
        return False

@dungeons_bp.route('', methods=['GET'])
def get_all_dungeons():
    """GET /api/config/dungeons - Obtiene todas las dungeons"""
    try:
        config = load_dungeons_config()
        return jsonify({
            'success': True,
            'config': config.get('dungeons', [])
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@dungeons_bp.route('', methods=['POST'])
def create_dungeon():
    """POST /api/config/dungeons - Crea una nueva dungeon"""
    try:
        new_dungeon = request.get_json()
        
        if not new_dungeon or 'dungeonId' not in new_dungeon:
            return jsonify({
                'success': False,
                'error': 'dungeonId es requerido'
            }), 400
        
        config = load_dungeons_config()
        dungeons = config.get('dungeons', [])
        
        # Verificar si ya existe
        existing = next((d for d in dungeons if d.get('dungeonId') == new_dungeon['dungeonId']), None)
        if existing:
            return jsonify({
                'success': False,
                'error': 'Dungeon con ese ID ya existe'
            }), 400
        
        dungeons.append(new_dungeon)
        config['dungeons'] = dungeons
        
        if save_dungeons_config(config):
            return jsonify({
                'success': True,
                'message': 'Dungeon creada exitosamente',
                'dungeon': new_dungeon
            }), 201
        else:
            return jsonify({
                'success': False,
                'error': 'Error al guardar configuración'
            }), 500
            
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@dungeons_bp.route('/<dungeon_id>', methods=['GET'])
def get_dungeon(dungeon_id):
    """GET /api/config/dungeons/<id> - Obtiene una dungeon específica"""
    try:
        config = load_dungeons_config()
        dungeons = config.get('dungeons', [])
        
        dungeon = next((d for d in dungeons if d.get('dungeonId') == dungeon_id), None)
        
        if dungeon:
            return jsonify({
                'success': True,
                'dungeon': dungeon
            })
        else:
            return jsonify({
                'success': False,
                'error': 'Dungeon no encontrada'
            }), 404
            
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@dungeons_bp.route('/<dungeon_id>', methods=['PUT'])
def update_dungeon(dungeon_id):
    """PUT /api/config/dungeons/<id> - Actualiza una dungeon"""
    try:
        updated_dungeon = request.get_json()
        
        if not updated_dungeon:
            return jsonify({
                'success': False,
                'error': 'Datos de dungeon requeridos'
            }), 400
        
        config = load_dungeons_config()
        dungeons = config.get('dungeons', [])
        
        # Buscar y actualizar
        found = False
        for i, dungeon in enumerate(dungeons):
            if dungeon.get('dungeonId') == dungeon_id:
                dungeons[i] = updated_dungeon
                found = True
                break
        
        if not found:
            return jsonify({
                'success': False,
                'error': 'Dungeon no encontrada'
            }), 404
        
        config['dungeons'] = dungeons
        
        if save_dungeons_config(config):
            return jsonify({
                'success': True,
                'message': 'Dungeon actualizada exitosamente',
                'dungeon': updated_dungeon
            })
        else:
            return jsonify({
                'success': False,
                'error': 'Error al guardar configuración'
            }), 500
            
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@dungeons_bp.route('/<dungeon_id>', methods=['DELETE'])
def delete_dungeon(dungeon_id):
    """DELETE /api/config/dungeons/<id> - Elimina una dungeon"""
    try:
        config = load_dungeons_config()
        dungeons = config.get('dungeons', [])
        
        # Filtrar la dungeon a eliminar
        original_length = len(dungeons)
        dungeons = [d for d in dungeons if d.get('dungeonId') != dungeon_id]
        
        if len(dungeons) == original_length:
            return jsonify({
                'success': False,
                'error': 'Dungeon no encontrada'
            }), 404
        
        config['dungeons'] = dungeons
        
        if save_dungeons_config(config):
            return jsonify({
                'success': True,
                'message': 'Dungeon eliminada exitosamente'
            })
        else:
            return jsonify({
                'success': False,
                'error': 'Error al guardar configuración'
            }), 500
            
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@dungeons_bp.route('/world/<world_name>', methods=['GET'])
def get_dungeons_by_world(world_name):
    """GET /api/config/dungeons/world/<world> - Obtiene dungeons de un mundo específico"""
    try:
        config = load_dungeons_config()
        dungeons = config.get('dungeons', [])
        
        # Filtrar por mundo
        world_dungeons = [d for d in dungeons if d.get('world') == world_name]
        
        return jsonify({
            'success': True,
            'config': world_dungeons,
            'world': world_name
        })
        
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500
