"""
Sprint 4: API Routes - Sistema de Eventos
Endpoints para gestión de eventos (CRUD, control, stats, currency)
"""
from flask import Blueprint, jsonify, request
import json
import os
import sqlite3
from datetime import datetime

events_bp = Blueprint('events_api', __name__, url_prefix='/api/events')

# Directorios
CONFIG_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'config')
DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'plugins', 'MMORPGPlugin', 'rpgdata.db')

def load_events_config():
    """Carga la configuración de eventos"""
    try:
        config_path = os.path.join(CONFIG_DIR, 'data', 'events_config.json')
        if os.path.exists(config_path):
            with open(config_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        return None
    except Exception as e:
        return None

def save_events_config(data):
    """Guarda la configuración de eventos"""
    try:
        config_path = os.path.join(CONFIG_DIR, 'data', 'events_config.json')
        os.makedirs(os.path.dirname(config_path), exist_ok=True)
        with open(config_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        return True
    except Exception as e:
        return False

def get_db_connection():
    """Obtiene conexión a la BD del plugin"""
    try:
        conn = sqlite3.connect(DB_PATH)
        conn.row_factory = sqlite3.Row
        return conn
    except Exception as e:
        return None

# ===================== EVENTOS CRUD =====================

@events_bp.route('', methods=['GET'])
def get_events():
    """GET /api/events - Lista todos los eventos"""
    data = load_events_config()
    if data:
        return jsonify({'success': True, 'events': data.get('events', [])})
    return jsonify({'success': False, 'error': 'Config not found'}), 404

@events_bp.route('/<event_id>', methods=['GET'])
def get_event_info(event_id):
    """GET /api/events/<id> - Info detallada de evento"""
    data = load_events_config()
    if not data:
        return jsonify({'success': False, 'error': 'Config not found'}), 404
    
    for event in data.get('events', []):
        if event.get('id') == event_id:
            return jsonify({'success': True, 'event': event})
    
    return jsonify({'success': False, 'error': 'Event not found'}), 404

@events_bp.route('', methods=['POST'])
def create_event():
    """POST /api/events - Crear nuevo evento"""
    try:
        event_data = request.json
        data = load_events_config() or {'events': []}
        
        # Verificar que no exista el ID
        if any(e.get('id') == event_data.get('id') for e in data['events']):
            return jsonify({'success': False, 'error': 'Event ID already exists'}), 400
        
        data['events'].append(event_data)
        save_events_config(data)
        return jsonify({'success': True, 'event': event_data}), 201
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/<event_id>', methods=['PUT'])
def update_event(event_id):
    """PUT /api/events/<id> - Actualizar evento"""
    try:
        event_data = request.json
        data = load_events_config()
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        for i, event in enumerate(data['events']):
            if event.get('id') == event_id:
                data['events'][i] = event_data
                save_events_config(data)
                return jsonify({'success': True, 'event': event_data})
        
        return jsonify({'success': False, 'error': 'Event not found'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/<event_id>', methods=['DELETE'])
def delete_event(event_id):
    """DELETE /api/events/<id> - Eliminar evento"""
    try:
        data = load_events_config()
        if not data:
            return jsonify({'success': False, 'error': 'Config not found'}), 404
        
        original_count = len(data['events'])
        data['events'] = [e for e in data['events'] if e.get('id') != event_id]
        
        if len(data['events']) == original_count:
            return jsonify({'success': False, 'error': 'Event not found'}), 404
        
        save_events_config(data)
        return jsonify({'success': True, 'message': 'Event deleted'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# ===================== CONTROL DE EVENTOS =====================

@events_bp.route('/<event_id>/start', methods=['POST'])
def start_event(event_id):
    """POST /api/events/<id>/start - Iniciar evento"""
    # Nota: Esto requiere comunicación con el servidor Minecraft via RCON
    # Por ahora retorna success simulado
    data = load_events_config()
    if not data:
        return jsonify({'success': False, 'error': 'Config not found'}), 404
    
    event = next((e for e in data['events'] if e.get('id') == event_id), None)
    if not event:
        return jsonify({'success': False, 'error': 'Event not found'}), 404
    
    # TODO: Ejecutar comando RCON: /event start <event_id>
    return jsonify({
        'success': True, 
        'message': f'Event {event_id} started',
        'note': 'RCON integration pending'
    })

@events_bp.route('/<event_id>/stop', methods=['POST'])
def stop_event(event_id):
    """POST /api/events/<id>/stop - Detener evento"""
    data = load_events_config()
    if not data:
        return jsonify({'success': False, 'error': 'Config not found'}), 404
    
    event = next((e for e in data['events'] if e.get('id') == event_id), None)
    if not event:
        return jsonify({'success': False, 'error': 'Event not found'}), 404
    
    # TODO: Ejecutar comando RCON: /event stop <event_id>
    return jsonify({
        'success': True, 
        'message': f'Event {event_id} stopped',
        'note': 'RCON integration pending'
    })

@events_bp.route('/active', methods=['GET'])
def get_active_events():
    """GET /api/events/active - Eventos activos"""
    conn = get_db_connection()
    if not conn:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    try:
        cursor = conn.cursor()
        # Buscar eventos activos en event_history
        cursor.execute("""
            SELECT event_id, start_time 
            FROM event_history 
            WHERE end_time IS NULL 
            ORDER BY start_time DESC
        """)
        
        active_events = []
        for row in cursor.fetchall():
            active_events.append({
                'event_id': row['event_id'],
                'start_time': row['start_time']
            })
        
        conn.close()
        return jsonify({'success': True, 'active_events': active_events})
    except Exception as e:
        conn.close()
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/reload', methods=['POST'])
def reload_events():
    """POST /api/events/reload - Recargar configuraciones"""
    # TODO: Ejecutar comando RCON: /event reload
    return jsonify({
        'success': True,
        'message': 'Events config reloaded',
        'note': 'RCON integration pending'
    })

# ===================== ESTADÍSTICAS Y CURRENCY =====================

@events_bp.route('/<event_id>/stats', methods=['GET'])
def get_event_stats(event_id):
    """GET /api/events/<id>/stats - Estadísticas del evento"""
    conn = get_db_connection()
    if not conn:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    try:
        cursor = conn.cursor()
        
        # Estadísticas de participación
        cursor.execute("""
            SELECT 
                COUNT(DISTINCT player_uuid) as total_players,
                SUM(kills) as total_kills
            FROM event_participants 
            WHERE event_id = ?
        """, (event_id,))
        
        stats_row = cursor.fetchone()
        
        # Top 10 jugadores
        cursor.execute("""
            SELECT player_uuid, kills
            FROM event_participants
            WHERE event_id = ?
            ORDER BY kills DESC
            LIMIT 10
        """, (event_id,))
        
        top_players = []
        for row in cursor.fetchall():
            top_players.append({
                'player_uuid': row['player_uuid'],
                'kills': row['kills']
            })
        
        conn.close()
        return jsonify({
            'success': True,
            'stats': {
                'total_players': stats_row['total_players'] if stats_row else 0,
                'total_kills': stats_row['total_kills'] if stats_row else 0,
                'top_players': top_players
            }
        })
    except Exception as e:
        conn.close()
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/currency/<player_uuid>', methods=['GET'])
def get_player_currency(player_uuid):
    """GET /api/events/currency/<player> - Balance de monedas"""
    conn = get_db_connection()
    if not conn:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    try:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT total_coins, coins_spent, coins_earned, last_updated
            FROM event_currency
            WHERE player_uuid = ?
        """, (player_uuid,))
        
        row = cursor.fetchone()
        conn.close()
        
        if row:
            return jsonify({
                'success': True,
                'player_uuid': player_uuid,
                'currency': {
                    'total_coins': row['total_coins'],
                    'coins_spent': row['coins_spent'],
                    'coins_earned': row['coins_earned'],
                    'last_updated': row['last_updated']
                }
            })
        else:
            return jsonify({
                'success': True,
                'player_uuid': player_uuid,
                'currency': {
                    'total_coins': 0,
                    'coins_spent': 0,
                    'coins_earned': 0
                }
            })
    except Exception as e:
        conn.close()
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/currency/<player_uuid>', methods=['POST'])
def modify_player_currency(player_uuid):
    """POST /api/events/currency/<player> - Modificar monedas (add/set)"""
    try:
        action = request.json.get('action')  # 'add' o 'set'
        amount = request.json.get('amount', 0)
        
        if action not in ['add', 'set']:
            return jsonify({'success': False, 'error': 'Invalid action'}), 400
        
        # TODO: Ejecutar comando RCON: /event currency <action> <player> <amount>
        return jsonify({
            'success': True,
            'message': f'Currency {action} {amount} for {player_uuid}',
            'note': 'RCON integration pending'
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@events_bp.route('/leaderboard', methods=['GET'])
def get_leaderboard():
    """GET /api/events/leaderboard - Top jugadores"""
    conn = get_db_connection()
    if not conn:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    try:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT player_uuid, total_coins, coins_earned
            FROM event_currency
            ORDER BY total_coins DESC
            LIMIT 20
        """)
        
        leaderboard = []
        for i, row in enumerate(cursor.fetchall(), 1):
            leaderboard.append({
                'rank': i,
                'player_uuid': row['player_uuid'],
                'total_coins': row['total_coins'],
                'coins_earned': row['coins_earned']
            })
        
        conn.close()
        return jsonify({'success': True, 'leaderboard': leaderboard})
    except Exception as e:
        conn.close()
        return jsonify({'success': False, 'error': str(e)}), 500

# ===================== VALIDACIÓN =====================

@events_bp.route('/<event_id>/validate', methods=['GET'])
def validate_event(event_id):
    """GET /api/events/<id>/validate - Validar mobs del evento"""
    # TODO: Ejecutar comando RCON: /event validate <event_id>
    # Por ahora retorna validación simulada
    return jsonify({
        'success': True,
        'event_id': event_id,
        'validation': {
            'mobs_configured': 5,
            'mobs_existing': 5,
            'missing_mobs': [],
            'valid': True
        },
        'note': 'RCON integration pending'
    })

@events_bp.route('/<event_id>/history', methods=['GET'])
def get_event_history(event_id):
    """GET /api/events/<id>/history - Historial de ejecuciones"""
    conn = get_db_connection()
    if not conn:
        return jsonify({'success': False, 'error': 'Database not available'}), 500
    
    try:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT event_id, start_time, end_time, total_mobs_killed, total_mobs_spawned
            FROM event_history
            WHERE event_id = ?
            ORDER BY start_time DESC
            LIMIT 10
        """, (event_id,))
        
        history = []
        for row in cursor.fetchall():
            history.append({
                'event_id': row['event_id'],
                'start_time': row['start_time'],
                'end_time': row['end_time'],
                'total_mobs_killed': row['total_mobs_killed'],
                'total_mobs_spawned': row['total_mobs_spawned']
            })
        
        conn.close()
        return jsonify({'success': True, 'history': history})
    except Exception as e:
        conn.close()
        return jsonify({'success': False, 'error': str(e)}), 500
