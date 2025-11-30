import os
import json
import subprocess
import re
import shutil
from datetime import datetime, timedelta
from flask import Flask, render_template, request, jsonify, redirect, url_for, flash, send_file
from flask_login import LoginManager, UserMixin, login_user, logout_user, login_required, current_user
from werkzeug.security import check_password_hash, generate_password_hash
from werkzeug.utils import secure_filename
from dotenv import load_dotenv
import docker

# Cargar variables de entorno
load_dotenv()

app = Flask(__name__)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'default-secret-key')

# Configuración de rutas RELATIVAS - funciona en cualquier ubicación
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # /ruta/al/mc-paper
MINECRAFT_DIR = os.getenv('MINECRAFT_DIR', BASE_DIR)  # Si no hay .env, usa ruta relativa
PLUGINS_DIR = os.path.join(MINECRAFT_DIR, 'plugins')
CONFIG_DIR = os.path.join(MINECRAFT_DIR, 'config')
SERVER_PROPERTIES = os.path.join(CONFIG_DIR, 'server.properties')
WHITELIST_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'whitelist.json')
BLACKLIST_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'banned-players.json')
OPS_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'ops.json')
BACKUP_DIR = os.path.join(MINECRAFT_DIR, 'backups')
CONTAINER_NAME = os.getenv('DOCKER_CONTAINER_NAME', 'mc-paper')

app.config['UPLOAD_FOLDER'] = PLUGINS_DIR
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB max file size

# Login manager
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'

# Cliente Docker
try:
    docker_client = docker.from_env()
except Exception as e:
    docker_client = None
    print(f"Error conectando con Docker: {e}")

# Usuario simple (en producción usar base de datos)
class User(UserMixin):
    def __init__(self, id):
        self.id = id

@login_manager.user_loader
def load_user(user_id):
    return User(user_id)

# Función helper para ejecutar comandos RCON
def execute_rcon_command(container, command):
    """Ejecuta un comando RCON en el contenedor de Minecraft"""
    # mcrcon sintaxis: mcrcon -H localhost -P 25575 -p password "command"
    rcon_password = os.getenv('RCON_PASSWORD', 'minecraft123')
    exec_result = container.exec_run(f'mcrcon -H localhost -P 25575 -p {rcon_password} "{command}"')
    return exec_result

# Rutas de autenticación
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        
        # Verificar credenciales
        admin_username = os.getenv('ADMIN_USERNAME')
        admin_password_hash = os.getenv('ADMIN_PASSWORD_HASH')
        admin_password_plain = os.getenv('ADMIN_PASSWORD')  # Fallback para compatibilidad
        
        if username == admin_username:
            # Intentar con hash primero, luego con contraseña plana
            if admin_password_hash and check_password_hash(admin_password_hash, password):
                user = User(username)
                login_user(user)
                return redirect(url_for('dashboard'))
            elif admin_password_plain and password == admin_password_plain:
                user = User(username)
                login_user(user)
                # Redirigir al dashboard donde se mostrará el modal
                return redirect(url_for('dashboard'))
            else:
                flash('Credenciales incorrectas', 'error')
        else:
            flash('Credenciales incorrectas', 'error')
    
    return render_template('login.html')

@app.route('/api/auth/check-password-security')
@login_required
def check_password_security():
    """Verifica si la contraseña actual está sin hashear"""
    admin_password_hash = os.getenv('ADMIN_PASSWORD_HASH')
    admin_password_plain = os.getenv('ADMIN_PASSWORD')
    
    # Si hay contraseña plana y no hay hash, requiere cambio
    requires_change = bool(admin_password_plain and not admin_password_hash)
    
    return jsonify({
        'requires_change': requires_change,
        'has_email': bool(os.getenv('ADMIN_EMAIL'))
    })

@app.route('/api/auth/change-password', methods=['POST'])
@login_required
def change_password():
    """Cambiar contraseña y establecer email de recuperación"""
    try:
        data = request.json
        new_password = data.get('new_password')
        confirm_password = data.get('confirm_password')
        email = data.get('email')
        
        # Validaciones
        if not new_password or not confirm_password:
            return jsonify({'success': False, 'message': 'Faltan campos requeridos'}), 400
        
        if new_password != confirm_password:
            return jsonify({'success': False, 'message': 'Las contraseñas no coinciden'}), 400
        
        if len(new_password) < 8:
            return jsonify({'success': False, 'message': 'La contraseña debe tener al menos 8 caracteres'}), 400
        
        if email and '@' not in email:
            return jsonify({'success': False, 'message': 'Email inválido'}), 400
        
        # Generar hash de la nueva contraseña
        password_hash = generate_password_hash(new_password)
        
        # Leer archivo .env actual
        env_path = os.path.join(os.path.dirname(__file__), '.env')
        env_lines = []
        
        if os.path.exists(env_path):
            with open(env_path, 'r') as f:
                env_lines = f.readlines()
        
        # Actualizar o agregar variables
        updated = {'ADMIN_PASSWORD_HASH': False, 'ADMIN_EMAIL': False}
        removed_plain = False
        
        for i, line in enumerate(env_lines):
            if line.startswith('ADMIN_PASSWORD_HASH='):
                env_lines[i] = f'ADMIN_PASSWORD_HASH={password_hash}\n'
                updated['ADMIN_PASSWORD_HASH'] = True
            elif line.startswith('ADMIN_PASSWORD=') and not line.startswith('ADMIN_PASSWORD_HASH='):
                # Eliminar contraseña plana
                env_lines[i] = '# ADMIN_PASSWORD removed for security (now using ADMIN_PASSWORD_HASH)\n'
                removed_plain = True
            elif email and line.startswith('ADMIN_EMAIL='):
                env_lines[i] = f'ADMIN_EMAIL={email}\n'
                updated['ADMIN_EMAIL'] = True
        
        # Agregar si no existían
        if not updated['ADMIN_PASSWORD_HASH']:
            env_lines.append(f'ADMIN_PASSWORD_HASH={password_hash}\n')
        
        if email and not updated['ADMIN_EMAIL']:
            env_lines.append(f'ADMIN_EMAIL={email}\n')
        
        # Guardar archivo .env actualizado
        with open(env_path, 'w') as f:
            f.writelines(env_lines)
        
        # Recargar variables de entorno
        load_dotenv(override=True)
        
        return jsonify({
            'success': True,
            'message': 'Contraseña actualizada correctamente. Por favor, vuelve a iniciar sesión.',
            'email_set': bool(email)
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': f'Error al cambiar contraseña: {str(e)}'}), 500

@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('login'))

# Dashboard principal
@app.route('/')
@login_required
def dashboard():
    return render_template('dashboard_v2.html')

# Dashboard antiguo (legacy)
@app.route('/dashboard-old')
@login_required
def dashboard_old():
    return render_template('dashboard.html')

# API: Estado del servidor
@app.route('/api/server/status')
@login_required
def server_status():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            status = container.status
            stats = container.stats(stream=False)
            
            # Calcular uso de CPU y memoria
            cpu_delta = stats['cpu_stats']['cpu_usage']['total_usage'] - stats['precpu_stats']['cpu_usage']['total_usage']
            system_delta = stats['cpu_stats']['system_cpu_usage'] - stats['precpu_stats']['system_cpu_usage']
            cpu_percent = (cpu_delta / system_delta) * 100.0 if system_delta > 0 else 0.0
            
            memory_usage = stats['memory_stats']['usage']
            memory_limit = stats['memory_stats']['limit']
            memory_percent = (memory_usage / memory_limit) * 100.0 if memory_limit > 0 else 0.0
            
            return jsonify({
                'status': status,
                'running': status == 'running',
                'cpu_percent': round(cpu_percent, 2),
                'memory_usage_mb': round(memory_usage / (1024 * 1024), 2),
                'memory_limit_mb': round(memory_limit / (1024 * 1024), 2),
                'memory_percent': round(memory_percent, 2)
            })
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Logs del servidor
@app.route('/api/server/logs')
@login_required
def server_logs():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            logs = container.logs(tail=100).decode('utf-8')
            return jsonify({'logs': logs})
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Reiniciar servidor
@app.route('/api/server/restart', methods=['POST'])
@login_required
def restart_server():
    try:
        result = subprocess.run(
            ['sudo', 'docker-compose', 'restart'],
            cwd=MINECRAFT_DIR,
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            return jsonify({'success': True, 'message': 'Servidor reiniciado'})
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Detener servidor
@app.route('/api/server/stop', methods=['POST'])
@login_required
def stop_server():
    try:
        result = subprocess.run(
            ['sudo', 'docker-compose', 'stop'],
            cwd=MINECRAFT_DIR,
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            return jsonify({'success': True, 'message': 'Servidor detenido'})
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Iniciar servidor
@app.route('/api/server/start', methods=['POST'])
@login_required
def start_server():
    try:
        result = subprocess.run(
            ['sudo', 'docker-compose', 'start'],
            cwd=MINECRAFT_DIR,
            capture_output=True,
            text=True
        )
        if result.returncode == 0:
            return jsonify({'success': True, 'message': 'Servidor iniciado'})
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Listar plugins
@app.route('/api/plugins')
@login_required
def list_plugins():
    try:
        plugins = []
        if os.path.exists(PLUGINS_DIR):
            for file in os.listdir(PLUGINS_DIR):
                if file.endswith('.jar'):
                    file_path = os.path.join(PLUGINS_DIR, file)
                    size = os.path.getsize(file_path)
                    # Verificar si está activo (renombrados a .jar.disabled están desactivados)
                    active = True
                    plugins.append({
                        'name': file,
                        'size': size,
                        'size_mb': round(size / (1024 * 1024), 2),
                        'active': active
                    })
            
            # También buscar plugins desactivados
            for file in os.listdir(PLUGINS_DIR):
                if file.endswith('.jar.disabled'):
                    file_path = os.path.join(PLUGINS_DIR, file)
                    size = os.path.getsize(file_path)
                    plugins.append({
                        'name': file.replace('.disabled', ''),
                        'size': size,
                        'size_mb': round(size / (1024 * 1024), 2),
                        'active': False
                    })
        
        return jsonify({'plugins': plugins})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Activar/Desactivar plugin
@app.route('/api/plugins/toggle', methods=['POST'])
@login_required
def toggle_plugin():
    try:
        plugin_name = request.json.get('name')
        if not plugin_name:
            return jsonify({'success': False, 'error': 'Nombre de plugin requerido'}), 400
        
        active_path = os.path.join(PLUGINS_DIR, plugin_name)
        disabled_path = os.path.join(PLUGINS_DIR, plugin_name + '.disabled')
        
        if os.path.exists(active_path):
            # Desactivar
            os.rename(active_path, disabled_path)
            return jsonify({'success': True, 'message': f'{plugin_name} desactivado', 'active': False})
        elif os.path.exists(disabled_path):
            # Activar
            os.rename(disabled_path, active_path)
            return jsonify({'success': True, 'message': f'{plugin_name} activado', 'active': True})
        else:
            return jsonify({'success': False, 'error': 'Plugin no encontrado'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Subir plugin
@app.route('/api/plugins/upload', methods=['POST'])
@login_required
def upload_plugin():
    try:
        if 'file' not in request.files:
            return jsonify({'success': False, 'error': 'No se proporcionó archivo'}), 400
        
        file = request.files['file']
        if file.filename == '':
            return jsonify({'success': False, 'error': 'Nombre de archivo vacío'}), 400
        
        if file and file.filename.endswith('.jar'):
            filename = secure_filename(file.filename)
            file.save(os.path.join(PLUGINS_DIR, filename))
            return jsonify({'success': True, 'message': f'Plugin {filename} subido correctamente'})
        else:
            return jsonify({'success': False, 'error': 'Solo se permiten archivos .jar'}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Eliminar plugin
@app.route('/api/plugins/delete', methods=['POST'])
@login_required
def delete_plugin():
    try:
        plugin_name = request.json.get('name')
        if not plugin_name:
            return jsonify({'success': False, 'error': 'Nombre de plugin requerido'}), 400
        
        plugin_path = os.path.join(PLUGINS_DIR, plugin_name)
        disabled_path = os.path.join(PLUGINS_DIR, plugin_name + '.disabled')
        
        if os.path.exists(plugin_path):
            os.remove(plugin_path)
            return jsonify({'success': True, 'message': f'{plugin_name} eliminado'})
        elif os.path.exists(disabled_path):
            os.remove(disabled_path)
            return jsonify({'success': True, 'message': f'{plugin_name} eliminado'})
        else:
            return jsonify({'success': False, 'error': 'Plugin no encontrado'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Leer whitelist
@app.route('/api/whitelist')
@login_required
def get_whitelist():
    try:
        if os.path.exists(WHITELIST_FILE):
            with open(WHITELIST_FILE, 'r') as f:
                whitelist = json.load(f)
            return jsonify({'whitelist': whitelist})
        else:
            return jsonify({'whitelist': []})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Actualizar whitelist
@app.route('/api/whitelist', methods=['POST'])
@login_required
def update_whitelist():
    try:
        whitelist = request.json.get('whitelist', [])
        os.makedirs(os.path.dirname(WHITELIST_FILE), exist_ok=True)
        with open(WHITELIST_FILE, 'w') as f:
            json.dump(whitelist, f, indent=2)
        return jsonify({'success': True, 'message': 'Whitelist actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Leer blacklist
@app.route('/api/blacklist')
@login_required
def get_blacklist():
    try:
        if os.path.exists(BLACKLIST_FILE):
            with open(BLACKLIST_FILE, 'r') as f:
                blacklist = json.load(f)
            return jsonify({'blacklist': blacklist})
        else:
            return jsonify({'blacklist': []})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Actualizar blacklist
@app.route('/api/blacklist', methods=['POST'])
@login_required
def update_blacklist():
    try:
        blacklist = request.json.get('blacklist', [])
        os.makedirs(os.path.dirname(BLACKLIST_FILE), exist_ok=True)
        with open(BLACKLIST_FILE, 'w') as f:
            json.dump(blacklist, f, indent=2)
        return jsonify({'success': True, 'message': 'Blacklist actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Leer server.properties
@app.route('/api/config/server-properties')
@login_required
def get_server_properties():
    try:
        if os.path.exists(SERVER_PROPERTIES):
            with open(SERVER_PROPERTIES, 'r') as f:
                content = f.read()
            return jsonify({'content': content})
        else:
            return jsonify({'error': 'Archivo no encontrado'}), 404
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Actualizar server.properties
@app.route('/api/config/server-properties', methods=['POST'])
@login_required
def update_server_properties():
    try:
        content = request.json.get('content', '')
        with open(SERVER_PROPERTIES, 'w') as f:
            f.write(content)
        return jsonify({'success': True, 'message': 'Configuración actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Parsear server.properties a JSON
@app.route('/api/config/server-properties-parsed')
@login_required
def get_server_properties_parsed():
    try:
        if not os.path.exists(SERVER_PROPERTIES):
            return jsonify({'error': 'Archivo no encontrado'}), 404
        
        properties = {}
        with open(SERVER_PROPERTIES, 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    if '=' in line:
                        key, value = line.split('=', 1)
                        properties[key.strip()] = value.strip()
        
        return jsonify({'properties': properties})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Actualizar propiedades específicas del servidor
@app.route('/api/config/update-property', methods=['POST'])
@login_required
def update_property():
    try:
        key = request.json.get('key')
        value = request.json.get('value')
        
        if not key:
            return jsonify({'success': False, 'error': 'Se requiere la clave'}), 400
        
        if not os.path.exists(SERVER_PROPERTIES):
            return jsonify({'success': False, 'error': 'Archivo no encontrado'}), 404
        
        # Leer el archivo
        lines = []
        found = False
        with open(SERVER_PROPERTIES, 'r') as f:
            for line in f:
                if line.strip() and not line.startswith('#') and '=' in line:
                    prop_key = line.split('=', 1)[0].strip()
                    if prop_key == key:
                        lines.append(f'{key}={value}\n')
                        found = True
                    else:
                        lines.append(line)
                else:
                    lines.append(line)
        
        # Si no se encontró, agregar al final
        if not found:
            lines.append(f'{key}={value}\n')
        
        # Guardar
        with open(SERVER_PROPERTIES, 'w') as f:
            f.writelines(lines)
        
        return jsonify({'success': True, 'message': f'Propiedad {key} actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Obtener jugadores online
@app.route('/api/server/players')
@login_required
def get_players():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                # Ejecutar comando list en el servidor
                exec_result = execute_rcon_command(container, 'list')
                if exec_result.exit_code == 0:
                    output = exec_result.output.decode('utf-8')
                    # Parsear salida: "There are X of a max of Y players online: player1, player2"
                    match = re.search(r'There are (\d+) of a max of (\d+) players online:?\s*(.*)', output)
                    if match:
                        online_count = int(match.group(1))
                        max_players = int(match.group(2))
                        players_str = match.group(3).strip()
                        players = [p.strip() for p in players_str.split(',')] if players_str else []
                        return jsonify({
                            'online': online_count,
                            'max': max_players,
                            'players': players
                        })
                
                # Si RCON no está disponible, intentar leer del log
                logs = container.logs(tail=500).decode('utf-8')
                # Contar joins menos leaves
                joins = re.findall(r'(\w+) joined the game', logs)
                leaves = re.findall(r'(\w+) left the game', logs)
                online_players = list(set(joins) - set(leaves))
                
                return jsonify({
                    'online': len(online_players),
                    'max': 20,
                    'players': online_players
                })
            else:
                return jsonify({'online': 0, 'max': 20, 'players': []})
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'online': 0, 'max': 20, 'players': [], 'error': str(e)})

# API: Ejecutar comando de Minecraft
@app.route('/api/server/command', methods=['POST'])
@login_required
def execute_command():
    try:
        command = request.json.get('command', '').strip()
        if not command:
            return jsonify({'success': False, 'error': 'Comando vacío'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                # Ejecutar comando con mcrcon
                exec_result = execute_rcon_command(container, command)
                
                # Decodificar output
                if exec_result.output:
                    output = exec_result.output.decode('utf-8', errors='replace').strip()
                else:
                    output = 'Comando ejecutado (sin salida)'
                
                # Si hay error de exit code pero no hay output, es un error de rcon
                if exec_result.exit_code != 0 and not output:
                    output = 'Error: RCON no disponible o comando inválido'
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'output': output,
                    'command': command
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Acciones sobre jugadores
@app.route('/api/players/kick', methods=['POST'])
@login_required
def kick_player():
    try:
        player = request.json.get('player', '').strip()
        reason = request.json.get('reason', 'Kickeado por un administrador')
        
        if not player:
            return jsonify({'success': False, 'error': 'Nombre de jugador requerido'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'kick {player} {reason}'
                exec_result = execute_rcon_command(container, command)
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': f'Jugador {player} kickeado'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/players/ban', methods=['POST'])
@login_required
def ban_player():
    try:
        player = request.json.get('player', '').strip()
        reason = request.json.get('reason', 'Baneado por un administrador')
        
        if not player:
            return jsonify({'success': False, 'error': 'Nombre de jugador requerido'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'ban {player} {reason}'
                exec_result = execute_rcon_command(container, command)
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': f'Jugador {player} baneado'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/players/gamemode', methods=['POST'])
@login_required
def change_gamemode():
    try:
        player = request.json.get('player', '').strip()
        gamemode = request.json.get('gamemode', 'survival')
        
        if not player:
            return jsonify({'success': False, 'error': 'Nombre de jugador requerido'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'gamemode {gamemode} {player}'
                exec_result = execute_rcon_command(container, command)
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': f'Gamemode de {player} cambiado a {gamemode}'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Gestión de OPs
@app.route('/api/ops')
@login_required
def get_ops():
    try:
        if os.path.exists(OPS_FILE):
            with open(OPS_FILE, 'r') as f:
                ops = json.load(f)
            return jsonify({'ops': ops})
        else:
            return jsonify({'ops': []})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/ops/add', methods=['POST'])
@login_required
def add_op():
    try:
        player = request.json.get('player', '').strip()
        level = request.json.get('level', 4)
        
        if not player:
            return jsonify({'success': False, 'error': 'Nombre de jugador requerido'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'op {player}'
                exec_result = execute_rcon_command(container, command)
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': f'{player} es ahora operador'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/ops/remove', methods=['POST'])
@login_required
def remove_op():
    try:
        player = request.json.get('player', '').strip()
        
        if not player:
            return jsonify({'success': False, 'error': 'Nombre de jugador requerido'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'deop {player}'
                exec_result = execute_rcon_command(container, command)
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': f'{player} ya no es operador'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Obtener versión del servidor
@app.route('/api/server/version')
@login_required
def get_server_version():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            logs = container.logs(tail=100).decode('utf-8')
            # Buscar versión de Paper en los logs
            match = re.search(r'This server is running Paper version ([^\s]+)', logs)
            if match:
                return jsonify({'version': match.group(1)})
            else:
                return jsonify({'version': 'Desconocida'})
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'version': 'Error', 'error': str(e)})

# API: Actualizar servidor (ejecutar update.sh)
@app.route('/api/server/update', methods=['POST'])
@login_required
def update_server():
    try:
        result = subprocess.run(
            ['sudo', './update.sh'],
            cwd=MINECRAFT_DIR,
            capture_output=True,
            text=True,
            timeout=300
        )
        if result.returncode == 0:
            return jsonify({'success': True, 'message': 'Servidor actualizado correctamente', 'output': result.stdout})
        else:
            return jsonify({'success': False, 'error': result.stderr, 'output': result.stdout}), 500
    except subprocess.TimeoutExpired:
        return jsonify({'success': False, 'error': 'La actualización tardó demasiado (timeout)'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Actualizar plugins (ejecutar update.sh --plugins)
@app.route('/api/plugins/update-all', methods=['POST'])
@login_required
def update_all_plugins():
    try:
        result = subprocess.run(
            ['sudo', './update.sh', '--plugins'],
            cwd=MINECRAFT_DIR,
            capture_output=True,
            text=True,
            timeout=180
        )
        if result.returncode == 0:
            return jsonify({'success': True, 'message': 'Plugins actualizados correctamente', 'output': result.stdout})
        else:
            return jsonify({'success': False, 'error': result.stderr, 'output': result.stdout}), 500
    except subprocess.TimeoutExpired:
        return jsonify({'success': False, 'error': 'La actualización tardó demasiado (timeout)'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Crear backup del mundo
@app.route('/api/backup/create', methods=['POST'])
@login_required
def create_backup():
    try:
        os.makedirs(BACKUP_DIR, exist_ok=True)
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_name = f'backup_{timestamp}.tar.gz'
        backup_path = os.path.join(BACKUP_DIR, backup_name)
        
        # Crear backup del directorio worlds
        worlds_dir = os.path.join(MINECRAFT_DIR, 'worlds')
        
        result = subprocess.run(
            ['tar', '-czf', backup_path, '-C', MINECRAFT_DIR, 'worlds'],
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            size = os.path.getsize(backup_path)
            size_mb = round(size / (1024 * 1024), 2)
            return jsonify({
                'success': True, 
                'message': f'Backup creado: {backup_name}',
                'backup': {
                    'name': backup_name,
                    'size_mb': size_mb,
                    'date': timestamp
                }
            })
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Listar backups
@app.route('/api/backup/list')
@login_required
def list_backups():
    try:
        if not os.path.exists(BACKUP_DIR):
            return jsonify({'backups': []})
        
        backups = []
        for file in os.listdir(BACKUP_DIR):
            if file.endswith('.tar.gz'):
                file_path = os.path.join(BACKUP_DIR, file)
                size = os.path.getsize(file_path)
                mtime = os.path.getmtime(file_path)
                backups.append({
                    'name': file,
                    'size_mb': round(size / (1024 * 1024), 2),
                    'date': datetime.fromtimestamp(mtime).strftime('%Y-%m-%d %H:%M:%S')
                })
        
        backups.sort(key=lambda x: x['date'], reverse=True)
        return jsonify({'backups': backups})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Descargar backup
@app.route('/api/backup/download/<filename>')
@login_required
def download_backup(filename):
    try:
        backup_path = os.path.join(BACKUP_DIR, secure_filename(filename))
        if os.path.exists(backup_path) and filename.endswith('.tar.gz'):
            return send_file(backup_path, as_attachment=True)
        else:
            return jsonify({'error': 'Backup no encontrado'}), 404
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Eliminar backup
@app.route('/api/backup/delete', methods=['POST'])
@login_required
def delete_backup():
    try:
        filename = request.json.get('name')
        if not filename:
            return jsonify({'success': False, 'error': 'Nombre de backup requerido'}), 400
        
        backup_path = os.path.join(BACKUP_DIR, secure_filename(filename))
        if os.path.exists(backup_path) and filename.endswith('.tar.gz'):
            os.remove(backup_path)
            return jsonify({'success': True, 'message': f'Backup {filename} eliminado'})
        else:
            return jsonify({'success': False, 'error': 'Backup no encontrado'}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Restaurar desde backup
@app.route('/api/backup/restore', methods=['POST'])
@login_required
def restore_backup():
    try:
        filename = request.json.get('name')
        if not filename:
            return jsonify({'success': False, 'error': 'Nombre de backup requerido'}), 400
        
        backup_path = os.path.join(BACKUP_DIR, secure_filename(filename))
        if not os.path.exists(backup_path) or not filename.endswith('.tar.gz'):
            return jsonify({'success': False, 'error': 'Backup no encontrado'}), 404
        
        # Detener el servidor antes de restaurar
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                container.stop()
        
        # Hacer backup del mundo actual antes de restaurar
        worlds_dir = os.path.join(MINECRAFT_DIR, 'worlds')
        backup_current = os.path.join(BACKUP_DIR, f'pre-restore_{datetime.now().strftime("%Y%m%d_%H%M%S")}.tar.gz')
        
        subprocess.run(['tar', '-czf', backup_current, '-C', MINECRAFT_DIR, 'worlds'], check=True)
        
        # Eliminar mundos actuales
        if os.path.exists(worlds_dir):
            shutil.rmtree(worlds_dir)
        
        # Extraer backup
        result = subprocess.run(
            ['tar', '-xzf', backup_path, '-C', MINECRAFT_DIR],
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            # Reiniciar servidor
            if docker_client:
                container = docker_client.containers.get(CONTAINER_NAME)
                container.start()
            
            return jsonify({
                'success': True,
                'message': f'Mundo restaurado desde {filename}. Backup actual guardado como {os.path.basename(backup_current)}'
            })
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
            
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Reload de plugins
@app.route('/api/plugins/reload', methods=['POST'])
@login_required
def reload_plugins():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                # Ejecutar comando reload con PlugMan si está instalado, sino con reload estándar
                exec_result = execute_rcon_command(container, 'plugman reload all')
                
                if exec_result.exit_code != 0:
                    # Intentar con reload estándar de Bukkit
                    exec_result = execute_rcon_command(container, 'reload confirm')
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': 'Plugins recargados' if exec_result.exit_code == 0 else 'Error al recargar plugins',
                    'output': exec_result.output.decode('utf-8') if exec_result.output else ''
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Obtener información detallada de plugins desde logs
@app.route('/api/plugins/detailed')
@login_required
def get_plugins_detailed():
    try:
        plugins_info = []
        
        if os.path.exists(PLUGINS_DIR):
            for file in os.listdir(PLUGINS_DIR):
                if file.endswith('.jar'):
                    file_path = os.path.join(PLUGINS_DIR, file)
                    size = os.path.getsize(file_path)
                    mtime = os.path.getmtime(file_path)
                    
                    # Intentar obtener información del plugin desde el servidor
                    plugin_name = file.replace('.jar', '')
                    version = 'Desconocida'
                    author = 'Desconocido'
                    status = 'Activo'
                    
                    # Si el servidor está corriendo, obtener info de plugins
                    if docker_client:
                        try:
                            container = docker_client.containers.get(CONTAINER_NAME)
                            if container.status == 'running':
                                exec_result = execute_rcon_command(container, 'plugins')
                                if exec_result.exit_code == 0:
                                    output = exec_result.output.decode('utf-8')
                                    # Parsear output para encontrar el plugin
                                    if plugin_name.lower() in output.lower():
                                        status = 'Cargado'
                        except:
                            pass
                    
                    plugins_info.append({
                        'name': file,
                        'plugin_name': plugin_name,
                        'size': size,
                        'size_mb': round(size / (1024 * 1024), 2),
                        'modified': datetime.fromtimestamp(mtime).strftime('%Y-%m-%d %H:%M:%S'),
                        'version': version,
                        'author': author,
                        'status': status,
                        'active': True
                    })
            
            # Plugins desactivados
            for file in os.listdir(PLUGINS_DIR):
                if file.endswith('.jar.disabled'):
                    file_path = os.path.join(PLUGINS_DIR, file)
                    size = os.path.getsize(file_path)
                    mtime = os.path.getmtime(file_path)
                    
                    plugins_info.append({
                        'name': file.replace('.disabled', ''),
                        'plugin_name': file.replace('.jar.disabled', ''),
                        'size': size,
                        'size_mb': round(size / (1024 * 1024), 2),
                        'modified': datetime.fromtimestamp(mtime).strftime('%Y-%m-%d %H:%M:%S'),
                        'version': 'Desconocida',
                        'author': 'Desconocido',
                        'status': 'Desactivado',
                        'active': False
                    })
        
        return jsonify({'plugins': plugins_info})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Obtener uptime del servidor
@app.route('/api/server/uptime')
@login_required
def get_uptime():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                started_at = container.attrs['State']['StartedAt']
                start_time = datetime.fromisoformat(started_at.replace('Z', '+00:00'))
                uptime_seconds = (datetime.now(start_time.tzinfo) - start_time).total_seconds()
                
                days = int(uptime_seconds // 86400)
                hours = int((uptime_seconds % 86400) // 3600)
                minutes = int((uptime_seconds % 3600) // 60)
                
                uptime_str = f'{days}d {hours}h {minutes}m'
                return jsonify({
                    'uptime': uptime_str,
                    'uptime_seconds': int(uptime_seconds),
                    'started_at': started_at
                })
            else:
                return jsonify({'uptime': 'Servidor apagado', 'uptime_seconds': 0})
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Obtener estadísticas históricas (últimas 24h)
@app.route('/api/stats/history')
@login_required
def get_stats_history():
    try:
        stats_file = os.path.join(BASE_DIR, 'web', 'stats_history.json')
        
        if os.path.exists(stats_file):
            with open(stats_file, 'r') as f:
                history = json.load(f)
                
            # Filtrar últimas 24 horas
            cutoff = (datetime.now() - timedelta(hours=24)).timestamp()
            recent = [s for s in history if s.get('timestamp', 0) > cutoff]
            
            return jsonify({'history': recent[-288:]})  # Máximo 288 puntos (cada 5 min)
        else:
            return jsonify({'history': []})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Guardar estadística actual
@app.route('/api/stats/save', methods=['POST'])
@login_required
def save_stat():
    try:
        stats_file = os.path.join(BASE_DIR, 'web', 'stats_history.json')
        
        # Leer historial existente
        history = []
        if os.path.exists(stats_file):
            with open(stats_file, 'r') as f:
                history = json.load(f)
        
        # Agregar nueva estadística
        new_stat = {
            'timestamp': datetime.now().timestamp(),
            'cpu': request.json.get('cpu', 0),
            'memory': request.json.get('memory', 0),
            'players': request.json.get('players', 0)
        }
        
        history.append(new_stat)
        
        # Mantener solo últimas 24 horas
        cutoff = (datetime.now() - timedelta(hours=24)).timestamp()
        history = [s for s in history if s.get('timestamp', 0) > cutoff]
        
        # Guardar
        with open(stats_file, 'w') as f:
            json.dump(history, f)
        
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Obtener TPS (Ticks Per Second)
@app.route('/api/server/tps')
@login_required
def get_tps():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                # Ejecutar comando tps
                exec_result = execute_rcon_command(container, 'tps')
                
                if exec_result.exit_code == 0:
                    output = exec_result.output.decode('utf-8')
                    # Parsear TPS del output
                    # Formato típico: "TPS from last 1m, 5m, 15m: 20.0, 20.0, 20.0"
                    match = re.search(r'(\d+\.?\d*),?\s*(\d+\.?\d*),?\s*(\d+\.?\d*)', output)
                    if match:
                        return jsonify({
                            'tps_1m': float(match.group(1)),
                            'tps_5m': float(match.group(2)),
                            'tps_15m': float(match.group(3)),
                            'raw': output
                        })
                
                return jsonify({'tps_1m': 20.0, 'tps_5m': 20.0, 'tps_15m': 20.0, 'raw': 'TPS no disponible'})
            else:
                return jsonify({'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Obtener chat reciente
@app.route('/api/server/chat')
@login_required
def get_chat():
    try:
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            logs = container.logs(tail=200).decode('utf-8', errors='ignore')
            
            # Filtrar solo mensajes de chat
            chat_pattern = r'\[.*?\]: <(\w+)> (.+)'
            chat_messages = []
            
            for line in logs.split('\n'):
                match = re.search(chat_pattern, line)
                if match:
                    chat_messages.append({
                        'player': match.group(1),
                        'message': match.group(2),
                        'timestamp': datetime.now().isoformat()
                    })
            
            return jsonify({'messages': chat_messages[-50:]})  # Últimos 50 mensajes
        else:
            return jsonify({'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Enviar mensaje de chat
@app.route('/api/server/say', methods=['POST'])
@login_required
def say_message():
    try:
        message = request.json.get('message', '').strip()
        if not message:
            return jsonify({'success': False, 'error': 'Mensaje vacío'}), 400
        
        if docker_client:
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'say {message}'
                exec_result = container.exec_run(f'rcon-cli {command}')
                
                return jsonify({
                    'success': exec_result.exit_code == 0,
                    'message': 'Mensaje enviado'
                })
            else:
                return jsonify({'success': False, 'error': 'Servidor no está corriendo'}), 400
        else:
            return jsonify({'success': False, 'error': 'Docker no disponible'}), 500
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Listar archivos de configuración editables
@app.route('/api/files/list')
@login_required
def list_config_files():
    try:
        files = []
        
        # Archivos en config/
        if os.path.exists(CONFIG_DIR):
            for file in os.listdir(CONFIG_DIR):
                if file.endswith(('.yml', '.yaml', '.properties', '.json', '.txt')):
                    file_path = os.path.join(CONFIG_DIR, file)
                    files.append({
                        'name': file,
                        'path': f'config/{file}',
                        'size': os.path.getsize(file_path),
                        'type': file.split('.')[-1]
                    })
        
        # Archivos en plugins/*/
        plugins_config_dir = os.path.join(MINECRAFT_DIR, 'worlds', 'plugins')
        if os.path.exists(plugins_config_dir):
            for plugin_dir in os.listdir(plugins_config_dir):
                plugin_path = os.path.join(plugins_config_dir, plugin_dir)
                if os.path.isdir(plugin_path):
                    for file in os.listdir(plugin_path):
                        if file.endswith(('.yml', '.yaml', '.json')):
                            file_path = os.path.join(plugin_path, file)
                            files.append({
                                'name': file,
                                'path': f'plugins/{plugin_dir}/{file}',
                                'size': os.path.getsize(file_path),
                                'type': file.split('.')[-1]
                            })
        
        return jsonify({'files': files})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Leer archivo de configuración
@app.route('/api/files/read')
@login_required
def read_config_file():
    try:
        file_path = request.args.get('path', '')
        if not file_path:
            return jsonify({'error': 'Path requerido'}), 400
        
        # Construir ruta segura
        if file_path.startswith('config/'):
            full_path = os.path.join(MINECRAFT_DIR, file_path)
        elif file_path.startswith('plugins/'):
            full_path = os.path.join(MINECRAFT_DIR, 'worlds', file_path)
        else:
            return jsonify({'error': 'Path no válido'}), 400
        
        # Verificar que el archivo existe y está dentro del directorio permitido
        full_path = os.path.abspath(full_path)
        if not full_path.startswith(MINECRAFT_DIR):
            return jsonify({'error': 'Acceso denegado'}), 403
        
        if os.path.exists(full_path):
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            return jsonify({
                'content': content,
                'path': file_path,
                'size': os.path.getsize(full_path)
            })
        else:
            return jsonify({'error': 'Archivo no encontrado'}), 404
            
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Guardar archivo de configuración
@app.route('/api/files/save', methods=['POST'])
@login_required
def save_config_file():
    try:
        file_path = request.json.get('path', '')
        content = request.json.get('content', '')
        
        if not file_path:
            return jsonify({'success': False, 'error': 'Path requerido'}), 400
        
        # Construir ruta segura
        if file_path.startswith('config/'):
            full_path = os.path.join(MINECRAFT_DIR, file_path)
        elif file_path.startswith('plugins/'):
            full_path = os.path.join(MINECRAFT_DIR, 'worlds', file_path)
        else:
            return jsonify({'success': False, 'error': 'Path no válido'}), 400
        
        # Verificar seguridad
        full_path = os.path.abspath(full_path)
        if not full_path.startswith(MINECRAFT_DIR):
            return jsonify({'success': False, 'error': 'Acceso denegado'}), 403
        
        # Crear backup antes de guardar
        if os.path.exists(full_path):
            backup_path = full_path + '.backup'
            shutil.copy2(full_path, backup_path)
        
        # Guardar
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        return jsonify({'success': True, 'message': 'Archivo guardado correctamente'})
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Listar mundos
@app.route('/api/worlds/list')
@login_required
def list_worlds():
    try:
        worlds_dir = os.path.join(MINECRAFT_DIR, 'worlds')
        worlds = []
        
        if os.path.exists(worlds_dir):
            for item in os.listdir(worlds_dir):
                item_path = os.path.join(worlds_dir, item)
                if os.path.isdir(item_path):
                    # Verificar si es un mundo válido (tiene level.dat)
                    level_dat = os.path.join(item_path, 'level.dat')
                    if os.path.exists(level_dat):
                        # Calcular tamaño
                        total_size = 0
                        for dirpath, dirnames, filenames in os.walk(item_path):
                            for filename in filenames:
                                filepath = os.path.join(dirpath, filename)
                                total_size += os.path.getsize(filepath)
                        
                        worlds.append({
                            'name': item,
                            'size_mb': round(total_size / (1024 * 1024), 2),
                            'path': item
                        })
        
        return jsonify({'worlds': worlds})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Crear backup de mundo específico
@app.route('/api/worlds/backup', methods=['POST'])
@login_required
def backup_world():
    try:
        world_name = request.json.get('world', '')
        if not world_name:
            return jsonify({'success': False, 'error': 'Nombre de mundo requerido'}), 400
        
        world_path = os.path.join(MINECRAFT_DIR, 'worlds', world_name)
        if not os.path.exists(world_path):
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        os.makedirs(BACKUP_DIR, exist_ok=True)
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_name = f'world_{world_name}_{timestamp}.tar.gz'
        backup_path = os.path.join(BACKUP_DIR, backup_name)
        
        result = subprocess.run(
            ['tar', '-czf', backup_path, '-C', os.path.join(MINECRAFT_DIR, 'worlds'), world_name],
            capture_output=True,
            text=True
        )
        
        if result.returncode == 0:
            size = os.path.getsize(backup_path)
            return jsonify({
                'success': True,
                'message': f'Backup del mundo {world_name} creado',
                'backup': {
                    'name': backup_name,
                    'size_mb': round(size / (1024 * 1024), 2)
                }
            })
        else:
            return jsonify({'success': False, 'error': result.stderr}), 500
            
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
