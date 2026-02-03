"""
Sprint 4: Crafting Configuration API
Endpoints para gestión de recetas de crafteo
"""

from flask import Blueprint, request, jsonify
import json
import os

crafting_bp = Blueprint('crafting', __name__, url_prefix='/api/config/crafting')

CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
CRAFTING_CONFIG = os.path.join(CONFIG_DIR, 'crafting_config.json')

def load_crafting_config():
    """Carga la configuración de crafting"""
    try:
        if os.path.exists(CRAFTING_CONFIG):
            with open(CRAFTING_CONFIG, 'r', encoding='utf-8') as f:
                return json.load(f)
        return {"recipes": []}
    except Exception as e:
        print(f"Error loading crafting config: {e}")
        return {"recipes": []}

def save_crafting_config(config):
    """Guarda la configuración de crafting"""
    try:
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(CRAFTING_CONFIG, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        print(f"Error saving crafting config: {e}")
        return False

@crafting_bp.route('', methods=['GET'])
def get_all_recipes():
    """GET /api/config/crafting - Obtiene todas las recetas"""
    try:
        config = load_crafting_config()
        return jsonify({'success': True, 'config': config.get('recipes', [])})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@crafting_bp.route('', methods=['POST'])
def create_recipe():
    """POST /api/config/crafting - Crea una nueva receta"""
    try:
        new_recipe = request.get_json()
        
        if not new_recipe or 'recipeId' not in new_recipe:
            return jsonify({'success': False, 'error': 'recipeId es requerido'}), 400
        
        config = load_crafting_config()
        recipes = config.get('recipes', [])
        
        existing = next((r for r in recipes if r.get('recipeId') == new_recipe['recipeId']), None)
        if existing:
            return jsonify({'success': False, 'error': 'Receta ya existe'}), 400
        
        recipes.append(new_recipe)
        config['recipes'] = recipes
        
        if save_crafting_config(config):
            return jsonify({'success': True, 'message': 'Receta creada', 'recipe': new_recipe}), 201
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@crafting_bp.route('/<recipe_id>', methods=['GET'])
def get_recipe(recipe_id):
    """GET /api/config/crafting/<id> - Obtiene una receta específica"""
    try:
        config = load_crafting_config()
        recipes = config.get('recipes', [])
        recipe = next((r for r in recipes if r.get('recipeId') == recipe_id), None)
        
        if recipe:
            return jsonify({'success': True, 'recipe': recipe})
        else:
            return jsonify({'success': False, 'error': 'Receta no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@crafting_bp.route('/<recipe_id>', methods=['PUT'])
def update_recipe(recipe_id):
    """PUT /api/config/crafting/<id> - Actualiza una receta"""
    try:
        updated_recipe = request.get_json()
        
        if not updated_recipe:
            return jsonify({'success': False, 'error': 'Datos requeridos'}), 400
        
        config = load_crafting_config()
        recipes = config.get('recipes', [])
        
        found = False
        for i, recipe in enumerate(recipes):
            if recipe.get('recipeId') == recipe_id:
                recipes[i] = updated_recipe
                found = True
                break
        
        if not found:
            return jsonify({'success': False, 'error': 'Receta no encontrada'}), 404
        
        config['recipes'] = recipes
        
        if save_crafting_config(config):
            return jsonify({'success': True, 'message': 'Receta actualizada', 'recipe': updated_recipe})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@crafting_bp.route('/<recipe_id>', methods=['DELETE'])
def delete_recipe(recipe_id):
    """DELETE /api/config/crafting/<id> - Elimina una receta"""
    try:
        config = load_crafting_config()
        recipes = config.get('recipes', [])
        
        original_length = len(recipes)
        recipes = [r for r in recipes if r.get('recipeId') != recipe_id]
        
        if len(recipes) == original_length:
            return jsonify({'success': False, 'error': 'Receta no encontrada'}), 404
        
        config['recipes'] = recipes
        
        if save_crafting_config(config):
            return jsonify({'success': True, 'message': 'Receta eliminada'})
        else:
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@crafting_bp.route('/category/<category>', methods=['GET'])
def get_recipes_by_category(category):
    """GET /api/config/crafting/category/<category> - Obtiene recetas por categoría"""
    try:
        config = load_crafting_config()
        recipes = config.get('recipes', [])
        filtered = [r for r in recipes if r.get('category') == category]
        
        return jsonify({'success': True, 'config': filtered, 'category': category})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500
