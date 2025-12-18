import os
import json
import subprocess
import re
import shutil
import sqlite3
from datetime import datetime, timedelta
from flask import Flask, render_template, request, jsonify, redirect, url_for, flash, send_file
from flask_login import LoginManager, UserMixin, login_user, logout_user, login_required, current_user
from werkzeug.security import check_password_hash, generate_password_hash
from werkzeug.utils import secure_filename
from dotenv import load_dotenv
from threading import Lock
from time import time
import docker
from models.world_manager import WorldManager
from models.rpg_manager import RPGManager
from models.resource_pack_manager import ResourcePackManager
from services.backup_service import BackupService
def _load_panel_config():
    try:
        config_file = os.path.join(CONFIG_DIR, 'panel_config.json')
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                return json.load(f)
    except Exception:
        pass
    # Valores por defecto
    return {
        'refresh_interval': 10000,
        'logs_interval': 15000,
        'tps_interval': 30000,
        'pause_when_hidden': True,
        'enable_cache': True,
        'cache_ttl': 5000,
        'rcon_enabled': True,
        'rcon_polling_enabled': False,
    }

# Cargar variables de entorno
load_dotenv()

app = Flask(__name__)
app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'default-secret-key')

# Configuración de rutas RELATIVAS - funciona en cualquier ubicación
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # /ruta/al/mc-paper
MINECRAFT_DIR = os.getenv('MINECRAFT_DIR', BASE_DIR)  # Si no hay .env, usa ruta relativa
PLUGINS_DIR = os.path.join(MINECRAFT_DIR, 'plugins')
CONFIG_DIR = os.path.join(MINECRAFT_DIR, 'config')
# SERVER_PROPERTIES apunta al mundo activo (worlds/active/server.properties)
SERVER_PROPERTIES = os.path.join(MINECRAFT_DIR, 'worlds', 'active', 'server.properties')
WHITELIST_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'whitelist.json')
BLACKLIST_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'banned-players.json')
OPS_FILE = os.path.join(MINECRAFT_DIR, 'worlds', 'ops.json')
BACKUP_DIR = os.path.join(MINECRAFT_DIR, 'backups')
WORLDS_DIR = os.path.join(MINECRAFT_DIR, 'worlds')
BACKUP_WORLDS_DIR = os.path.join(BACKUP_DIR, 'worlds')
CONTAINER_NAME = os.getenv('DOCKER_CONTAINER_NAME', 'minecraft-paper')

app.config['UPLOAD_FOLDER'] = PLUGINS_DIR
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB max file size

# Inicializar RPGManager primero
rpg_manager = RPGManager()

# Inicializar ResourcePackManager
resource_pack_manager = ResourcePackManager(BASE_DIR)

# Inicializar WorldManager con rpg_manager y BackupService
world_manager = WorldManager(WORLDS_DIR, rpg_manager=rpg_manager)
backup_service = BackupService(WORLDS_DIR, BACKUP_WORLDS_DIR)

# Login manager
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'

# Cliente Docker
try:
    # Intentar primero con configuración por defecto
    docker_client = docker.from_env()
    # Verificar que funciona
    docker_client.ping()
except Exception as e:
    try:
        # Intentar con socket Unix explícito (triple barra)
        docker_client = docker.DockerClient(base_url='unix:///var/run/docker.sock')
        docker_client.ping()
    except Exception as e2:
        docker_client = None
        print(f"Error conectando con Docker:")
        print(f"  - docker.from_env(): {e}")
        print(f"  - unix socket: {e2}")
        print(f"  Ejecuta: sudo usermod -aG docker $USER && newgrp docker")

# Sistema de caché para reducir llamadas RCON
class RCONCache:
    def __init__(self):
        self.cache = {}
        self.lock = Lock()
        self.default_ttl = 5  # 5 segundos por defecto
    
    def get(self, key, ttl=None):
        """Obtiene valor de caché si no ha expirado"""
        with self.lock:
            if key in self.cache:
                value, timestamp, cache_ttl = self.cache[key]
                if time() - timestamp < (ttl or cache_ttl):
                    return value
        return None
    
    def set(self, key, value, ttl=None):
        """Guarda valor en caché"""
        with self.lock:
            self.cache[key] = (value, time(), ttl or self.default_ttl)
    
    def clear(self):
        """Limpia toda la caché"""
        with self.lock:
            self.cache.clear()

rcon_cache = RCONCache()

# Usuario simple (en producción usar base de datos)
class User(UserMixin):
    def __init__(self, id):
        self.id = id

@login_manager.user_loader
def load_user(user_id):
    return User(user_id)

# Función helper para ejecutar comandos RCON
def execute_rcon_command(container, command, use_cache=False, cache_ttl=5):
    """Ejecuta un comando RCON en el contenedor de Minecraft"""
    # Usar caché si está habilitada
    if use_cache:
        cache_key = f"rcon:{command}"
        cached = rcon_cache.get(cache_key, cache_ttl)
        if cached is not None:
            return cached
    
    # mcrcon sintaxis: mcrcon -H localhost -P 25575 -p password "command"
    rcon_password = os.getenv('RCON_PASSWORD', 'minecraft123')
    exec_result = container.exec_run(f'mcrcon -H localhost -P 25575 -p {rcon_password} "{command}"')
    
    # Guardar en caché si está habilitada
    if use_cache:
        rcon_cache.set(cache_key, exec_result, cache_ttl)
    
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

# Dashboard de Kills Tracking
@app.route('/kills')
@login_required
def kills_dashboard():
    return render_template('kills_dashboard.html')

# Dashboard de Respawn
@app.route('/respawn')
@login_required
def respawn_panel():
    return render_template('respawn_panel.html')

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
        
        # Obtener mundo activo
        active_world = world_manager.get_active_world()
        world_name = active_world.metadata.get('name', 'Desconocido') if active_world else 'Desconocido'
        
        return jsonify({
            'properties': properties,
            'active_world': world_name
        })
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
                cfg = _load_panel_config()
                rcon_enabled = cfg.get('rcon_enabled', True)
                rcon_polling = cfg.get('rcon_polling_enabled', True)

                if rcon_enabled and rcon_polling:
                    # Ejecutar comando list con caché de 10 segundos
                    exec_result = execute_rcon_command(container, 'list', use_cache=True, cache_ttl=10)
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
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
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
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
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
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
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
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
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

# ========== FUNCIONES OBSOLETAS ELIMINADAS ==========
# Las siguientes funciones fueron reemplazadas por el sistema multi-mundo:
# - /api/backup/create -> /api/worlds/<slug>/backup
# - /api/backup/list -> /api/worlds/<slug>/backups
# - /api/backup/download/<filename> -> (mantener si es necesario)
# - /api/backup/delete -> /api/backups/<backup_filename>
# - /api/backup/restore -> /api/worlds/<slug>/restore

# ==== Endpoints legacy de Backups para compatibilidad con UI ====

@app.route('/api/backup/list')
@login_required
def legacy_backup_list():
    """Listar todos los backups (sin filtro por mundo) para compatibilidad con UI antigua."""
    try:
        backups = backup_service.list_backups()
        # Adaptar al formato esperado por dashboard.js (backups: name, date, size_mb)
        items = []
        for b in backups:
            items.append({
                'name': b.get('filename'),
                'date': b.get('created_at'),
                'size_mb': b.get('size_mb', 0)
            })
        return jsonify({'backups': items})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/backup/create', methods=['POST'])
@login_required
def legacy_backup_create():
    """Crear backup completo del servidor (todos los mundos)."""
    try:
        # Obtener lista de mundos y crear un backup por cada uno
        summary = world_manager.get_worlds_summary()
        created = []
        for w in summary.get('worlds', []):
            info = backup_service.create_backup(w.get('slug'), auto=False, description='Backup completo (legacy)')
            created.append(info.get('filename'))
        msg = f'Backups creados correctamente: {len(created)} mundos respaldados'
        return jsonify({'success': True, 'message': msg, 'created': created})
    except Exception as e:
        return jsonify({'success': False, 'message': f'Error al crear backup: {str(e)}'}), 500

@app.route('/api/backup/delete', methods=['POST'])
@login_required
def legacy_backup_delete():
    """Eliminar backup por nombre (compatibilidad)."""
    try:
        name = request.json.get('name')
        if not name:
            return jsonify({'success': False, 'error': 'Nombre de backup requerido'}), 400
        backup_service.delete_backup(name)
        return jsonify({'success': True, 'message': 'Backup eliminado'})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/backup/restore', methods=['POST'])
@login_required
def legacy_backup_restore():
    """Restaurar backup (requiere especificar mundo destino)."""
    try:
        data = request.get_json() or {}
        name = data.get('name')
        target_world = data.get('target_world')
        if not name or not target_world:
            return jsonify({'success': False, 'message': 'name y target_world requeridos'}), 400
        # Validar que el mundo destino existe y NO esté activo
        active_world = world_manager.get_active_world()
        if active_world and active_world.slug == target_world:
            return jsonify({'success': False, 'message': 'No se puede restaurar sobre el mundo activo'}), 400
        backup_service.restore_backup(name, target_world_slug=target_world)
        return jsonify({'success': True, 'message': 'Backup restaurado correctamente'})
    except Exception as e:
        return jsonify({'success': False, 'message': f'Error al restaurar backup: {str(e)}'}), 500

@app.route('/api/backup/download/<filename>')
@login_required
def legacy_backup_download(filename):
    """Descargar archivo de backup."""
    try:
        file_path = os.path.join(BACKUP_WORLDS_DIR, filename)
        if not os.path.exists(file_path):
            return jsonify({'error': 'Backup no encontrado'}), 404
        return send_file(file_path, as_attachment=True)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# API: Reload de plugins
@app.route('/api/plugins/reload', methods=['POST'])
@login_required
def reload_plugins():
    try:
        if docker_client:
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
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
                    
                    # Si el servidor está corriendo y RCON habilitado, obtener info de plugins
                    if docker_client and _load_panel_config().get('rcon_enabled', True):
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
                cfg = _load_panel_config()
                if (not cfg.get('rcon_enabled', True)) or (not cfg.get('rcon_polling_enabled', True)):
                    # Si el polling RCON está deshabilitado, devolver valor en caché si existe, o por defecto
                    cached = rcon_cache.get('rcon:tps', ttl=60)
                    if cached is not None and hasattr(cached, 'exit_code'):
                        try:
                            output = cached.output.decode('utf-8')
                            match = re.search(r'(\d+\.?\d*),?\s*(\d+\.?\d*),?\s*(\d+\.?\d*)', output)
                            if match:
                                return jsonify({
                                    'tps_1m': float(match.group(1)),
                                    'tps_5m': float(match.group(2)),
                                    'tps_15m': float(match.group(3)),
                                    'raw': output,
                                    'cached': True
                                })
                        except Exception:
                            pass
                    return jsonify({'tps_1m': 20.0, 'tps_5m': 20.0, 'tps_15m': 20.0, 'raw': 'RCON polling disabled', 'cached': True})
                # Ejecutar comando tps con caché de 30 segundos
                # Esto reduce DRÁSTICAMENTE las llamadas RCON
                exec_result = execute_rcon_command(container, 'tps', use_cache=True, cache_ttl=30)
                # Guardar bajo clave fija para posible uso offline
                rcon_cache.set('rcon:tps', exec_result, ttl=60)
                
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
            cfg = _load_panel_config()
            if not cfg.get('rcon_enabled', True):
                return jsonify({'success': False, 'error': 'RCON deshabilitado en el panel'}), 400
            container = docker_client.containers.get(CONTAINER_NAME)
            if container.status == 'running':
                command = f'say {message}'
                exec_result = execute_rcon_command(container, command)
                
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

# ========== GESTIÓN DE MUNDOS ==========

@app.route('/api/worlds', methods=['GET'])
@login_required
def list_worlds():
    """Listar todos los mundos disponibles"""
    try:
        summary = world_manager.get_worlds_summary()
        return jsonify({
            'success': True,
            'worlds': summary['worlds'],
            'active_world': summary['active_world'],
            'total_worlds': summary['total_worlds'],
            'max_worlds': summary['max_worlds']
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>', methods=['GET'])
@login_required
def get_world(slug):
    """Obtener detalles de un mundo específico"""
    try:
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        return jsonify({
            'success': True,
            'world': world.to_dict()
        })
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds', methods=['POST'])
@login_required
def create_world():
    """Crear nuevo mundo"""
    try:
        data = request.get_json()
        
        # Validar campos requeridos
        name = data.get('name')
        if not name:
            return jsonify({'success': False, 'error': 'Nombre del mundo requerido'}), 400
        
        # Parámetros opcionales
        template = data.get('template', 'vanilla')
        seed = data.get('seed', '')
        gamemode = data.get('gamemode', 'survival')
        difficulty = data.get('difficulty', 'normal')
        description = data.get('description', '')
        tags = data.get('tags', [])
        motd = data.get('motd', '')
        
        # Parámetros RPG
        is_rpg = data.get('isRPG', False)
        rpg_config = data.get('rpgConfig', None)
        
        # Crear mundo
        world = world_manager.create_world(
            name=name,
            template=template,
            seed=seed,
            gamemode=gamemode,
            difficulty=difficulty,
            description=description,
            tags=tags,
            motd=motd,
            is_rpg=is_rpg,
            rpg_config=rpg_config
        )
        
        return jsonify({
            'success': True,
            'message': f'Mundo "{name}" creado correctamente',
            'world': world.to_dict()
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': str(e)}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/activate', methods=['POST'])
@login_required
def activate_world(slug):
    """Activar mundo (cambiar symlink y reiniciar servidor)"""
    try:
        data = request.get_json() or {}
        
        # Leer configuración de backups
        config_file = os.path.join(CONFIG_DIR, 'backup_config.json')
        backup_config = {'auto_backup_enabled': True}
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                backup_config = json.load(f)
        
        # Usar configuración de backup automático o el valor enviado
        create_backup = data.get('create_backup', backup_config.get('auto_backup_enabled', True))
        
        # Verificar que el mundo existe
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        # Obtener contenedor
        container = docker_client.containers.get(CONTAINER_NAME)
        
        # Verificar si el servidor está corriendo
        server_was_running = container.status == 'running'
        
        # Detener servidor si está corriendo
        if server_was_running:
            try:
                # Enviar comando de guardado y apagado
                try:
                    execute_rcon_command(container, 'save-all')
                    execute_rcon_command(container, 'stop')
                except Exception:
                    # Si RCON está deshabilitado o falla, intentaremos detener vía Docker
                    pass
                
                # Esperar a que se detenga (máximo 60 segundos)
                import time
                for _ in range(60):
                    container.reload()
                    if container.status != 'running':
                        break
                    time.sleep(1)
                # Si aún sigue corriendo, forzar stop por Docker
                container.reload()
                if container.status == 'running':
                    container.stop(timeout=30)
                
            except Exception as e:
                print(f"Error al detener servidor: {e}")
        
        # Crear backup automático del mundo actual si se solicita
        if create_backup:
            try:
                current_world = world_manager.get_active_world()
                if current_world:
                    backup_service.create_backup(
                        current_world.slug,
                        auto=True,
                        description=f"Backup automático antes de cambiar a {world.metadata['name']}"
                    )
            except Exception as e:
                print(f"Error al crear backup automático: {e}")
        
        # Cambiar mundo activo
        world_manager.switch_world(slug)
        
        # Reiniciar servidor si estaba corriendo
        if server_was_running:
            container.start()
        
        return jsonify({
            'success': True,
            'message': f'Mundo "{world.metadata["name"]}" activado correctamente',
            'active_world': slug
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': str(e)}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>', methods=['DELETE'])
@login_required
def delete_world(slug):
    """Eliminar mundo"""
    try:
        # Obtener parámetro de backup
        create_backup = request.args.get('backup', 'false').lower() == 'true'
        
        # Eliminar mundo
        world_manager.delete_world(slug, create_backup=create_backup)
        
        return jsonify({
            'success': True,
            'message': f'Mundo "{slug}" eliminado correctamente'
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': str(e)}), 400
    except FileNotFoundError as e:
        return jsonify({'success': False, 'error': str(e)}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/duplicate', methods=['POST'])
@login_required
def duplicate_world(slug):
    """Duplicar mundo existente"""
    try:
        data = request.get_json()
        new_name = data.get('new_name')
        
        if not new_name:
            return jsonify({'success': False, 'error': 'Nombre del nuevo mundo requerido'}), 400
        
        # Duplicar mundo
        new_world = world_manager.duplicate_world(slug, new_name)
        
        return jsonify({
            'success': True,
            'message': f'Mundo duplicado como "{new_name}"',
            'world': new_world.to_dict()
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': str(e)}), 400
    except FileNotFoundError as e:
        return jsonify({'success': False, 'error': str(e)}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/config', methods=['GET'])
@login_required
def get_world_config(slug):
    """Obtener configuración de server.properties del mundo"""
    try:
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        properties = world.get_server_properties()
        
        return jsonify({
            'success': True,
            'properties': properties
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/config', methods=['PUT'])
@login_required
def update_world_config(slug):
    """Actualizar configuración de server.properties del mundo"""
    try:
        data = request.get_json()
        properties = data.get('properties')
        
        if not properties:
            return jsonify({'success': False, 'error': 'Propiedades requeridas'}), 400
        
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        # Actualizar propiedades
        world.update_server_properties(properties)
        
        return jsonify({
            'success': True,
            'message': 'Configuración actualizada correctamente'
        })
        
    except FileNotFoundError as e:
        return jsonify({'success': False, 'error': str(e)}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/backups', methods=['GET'])
@login_required
def list_world_backups(slug):
    """Listar backups de un mundo específico"""
    try:
        # Verificar que el mundo existe
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        backups = backup_service.list_backups(slug)
        total_size = backup_service.get_total_backup_size(slug)
        
        return jsonify({
            'success': True,
            'backups': backups,
            'total_size_mb': total_size['total_mb'],
            'total_count': total_size['count']
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/backup', methods=['POST'])
@login_required
def create_world_backup(slug):
    """Crear backup manual de un mundo"""
    try:
        data = request.get_json() or {}
        description = data.get('description', '')
        
        # Verificar que el mundo existe
        world = world_manager.get_world(slug)
        if world is None:
            return jsonify({'success': False, 'error': 'Mundo no encontrado'}), 404
        
        # Crear backup
        backup_info = backup_service.create_backup(
            world_slug=slug,
            auto=False,
            description=description
        )
        
        return jsonify({
            'success': True,
            'message': 'Backup creado correctamente',
            'backup': backup_info
        })
        
    except FileNotFoundError as e:
        return jsonify({'success': False, 'error': str(e)}), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/worlds/<slug>/restore', methods=['POST'])
@login_required
def restore_world_backup(slug):
    """Restaurar un mundo desde un backup"""
    try:
        data = request.get_json()
        backup_filename = data.get('backup_filename')
        
        if not backup_filename:
            return jsonify({'success': False, 'error': 'Nombre de backup requerido'}), 400
        
        # Verificar que el mundo no está activo
        active_world = world_manager.get_active_world()
        if active_world and active_world.slug == slug:
            return jsonify({
                'success': False,
                'error': 'No se puede restaurar el mundo activo. Cambia a otro mundo primero.'
            }), 400
        
        # Restaurar backup
        backup_service.restore_backup(backup_filename, slug)
        
        # Actualizar metadata del mundo
        world = world_manager.get_world(slug)
        if world:
            world.metadata['last_played'] = datetime.now().isoformat() + "Z"
            world.save_metadata()
        
        return jsonify({
            'success': True,
            'message': 'Mundo restaurado correctamente'
        })
        
    except FileNotFoundError as e:
        return jsonify({'success': False, 'error': str(e)}), 404
    except ValueError as e:
        return jsonify({'success': False, 'error': str(e)}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/backups/<backup_filename>', methods=['DELETE'])
@login_required
def delete_backup(backup_filename):
    """Eliminar un backup específico"""
    try:
        # Verificar que el backup existe
        backup_info = backup_service.get_backup_info(backup_filename)
        if not backup_info:
            return jsonify({'success': False, 'error': 'Backup no encontrado'}), 404
        
        # Eliminar backup
        backup_service.delete_backup(backup_filename)
        
        return jsonify({
            'success': True,
            'message': 'Backup eliminado correctamente'
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/backup-config', methods=['GET'])
@login_required
def get_backup_config():
    """Obtener configuración de backups"""
    try:
        config_file = os.path.join(CONFIG_DIR, 'backup_config.json')
        
        # Configuración por defecto
        default_config = {
            'auto_backup_enabled': True,
            'retention_count': 5
        }
        
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
        else:
            config = default_config
            # Crear archivo con configuración por defecto
            os.makedirs(CONFIG_DIR, exist_ok=True)
            with open(config_file, 'w') as f:
                json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'config': config
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/backup-config', methods=['PUT'])
@login_required
def update_backup_config():
    """Actualizar configuración de backups"""
    try:
        data = request.get_json()
        config_file = os.path.join(CONFIG_DIR, 'backup_config.json')
        
        # Leer configuración actual
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
        else:
            config = {}
        
        # Actualizar valores
        if 'auto_backup_enabled' in data:
            config['auto_backup_enabled'] = bool(data['auto_backup_enabled'])
        
        if 'retention_count' in data:
            retention = int(data['retention_count'])
            if retention < 1 or retention > 50:
                return jsonify({'success': False, 'error': 'retention_count debe estar entre 1 y 50'}), 400
            config['retention_count'] = retention
        
        # Guardar configuración
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Configuración actualizada',
            'config': config
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': 'Valor inválido'}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/panel-config', methods=['GET'])
@login_required
def get_panel_config():
    """Obtener configuración del panel"""
    try:
        config_file = os.path.join(CONFIG_DIR, 'panel_config.json')
        
        # Configuración por defecto
        default_config = {
            'refresh_interval': 5000,
            'logs_interval': 10000,
            'tps_interval': 10000,
            'pause_when_hidden': True,
            'enable_cache': True,
            'cache_ttl': 3000,
            'rcon_enabled': True,
            'rcon_polling_enabled': False
        }
        
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
        else:
            config = default_config
            os.makedirs(CONFIG_DIR, exist_ok=True)
            with open(config_file, 'w') as f:
                json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'config': config
        })
        
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/panel-config', methods=['PUT'])
@login_required
def update_panel_config():
    """Actualizar configuración del panel"""
    try:
        data = request.get_json()
        config_file = os.path.join(CONFIG_DIR, 'panel_config.json')
        
        # Leer configuración actual
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
        else:
            config = {}
        
        # Validar y actualizar intervalos
        if 'refresh_interval' in data:
            interval = int(data['refresh_interval'])
            if interval < 1000 or interval > 60000:
                return jsonify({'success': False, 'error': 'refresh_interval debe estar entre 1000 y 60000 ms'}), 400
            config['refresh_interval'] = interval
        
        if 'logs_interval' in data:
            interval = int(data['logs_interval'])
            if interval < 5000 or interval > 120000:
                return jsonify({'success': False, 'error': 'logs_interval debe estar entre 5000 y 120000 ms'}), 400
            config['logs_interval'] = interval
        
        if 'tps_interval' in data:
            interval = int(data['tps_interval'])
            if interval < 5000 or interval > 120000:
                return jsonify({'success': False, 'error': 'tps_interval debe estar entre 5000 y 120000 ms'}), 400
            config['tps_interval'] = interval
        
        if 'pause_when_hidden' in data:
            config['pause_when_hidden'] = bool(data['pause_when_hidden'])
        
        if 'enable_cache' in data:
            config['enable_cache'] = bool(data['enable_cache'])
        
        if 'cache_ttl' in data:
            ttl = int(data['cache_ttl'])
            if ttl < 1000 or ttl > 30000:
                return jsonify({'success': False, 'error': 'cache_ttl debe estar entre 1000 y 30000 ms'}), 400
            config['cache_ttl'] = ttl
        
        # Flags RCON
        if 'rcon_enabled' in data:
            config['rcon_enabled'] = bool(data['rcon_enabled'])
        if 'rcon_polling_enabled' in data:
            config['rcon_polling_enabled'] = bool(data['rcon_polling_enabled'])
        
        # Guardar configuración
        os.makedirs(CONFIG_DIR, exist_ok=True)
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Configuración del panel actualizada',
            'config': config
        })
        
    except ValueError as e:
        return jsonify({'success': False, 'error': 'Valor inválido'}), 400
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# API: Obtener mundo activo
@app.route('/api/worlds/active')
@login_required
def get_active_world_info():
    """Obtener información del mundo activo"""
    try:
        active_world = world_manager.get_active_world()
        if active_world:
            return jsonify({
                'success': True,
                'world': active_world.to_dict()
            })
        else:
            return jsonify({
                'success': False,
                'error': 'No hay mundo activo'
            }), 404
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# ============================================================================
# API RPG - Endpoints para integración con MMORPG Plugin
# ============================================================================

@app.route('/api/worlds/<slug>/rpg/status')
@login_required
def get_world_rpg_status(slug):
    """Obtiene el estado RPG de un mundo específico"""
    try:
        status = rpg_manager.get_rpg_status(slug)
        
        if status is None:
            return jsonify({
                'success': False,
                'message': 'El mundo no tiene modo RPG activado o no se encontraron datos'
            }), 404
        
        return jsonify({
            'success': True,
            'status': status
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/players')
@login_required
def get_world_rpg_players(slug):
    """Obtiene los datos de jugadores RPG de un mundo"""
    try:
        players = rpg_manager.get_players_data(slug)
        
        if players is None:
            return jsonify({
                'success': False,
                'message': 'No se encontraron datos de jugadores RPG'
            }), 404
        
        return jsonify({
            'success': True,
            'players': players
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/summary')
@login_required
def get_world_rpg_summary(slug):
    """Obtiene un resumen completo del estado RPG de un mundo"""
    try:
        summary = rpg_manager.get_rpg_summary(slug)
        
        if summary is None:
            return jsonify({
                'success': False,
                'message': 'El mundo no tiene modo RPG activado'
            }), 404
        
        return jsonify({
            'success': True,
            'summary': summary
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/worlds')
@login_required
def list_rpg_worlds():
    """Lista todos los mundos con modo RPG activado"""
    try:
        rpg_worlds = rpg_manager.list_rpg_worlds()
        
        return jsonify({
            'success': True,
            'worlds': rpg_worlds,
            'count': len(rpg_worlds)
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================================================
# ENDPOINTS PARA GESTIÓN DE SPAWNS
# ============================================================================

@app.route('/api/worlds/<slug>/rpg/spawns', methods=['GET'])
@login_required
def get_world_spawns(slug):
    """Obtiene todos los spawns configurados de un mundo RPG"""
    try:
        spawns_data = rpg_manager.read_file(slug, 'spawns.json', scope='local')
        
        if spawns_data is None:
            spawns_data = {"spawns": []}
        
        return jsonify({
            'success': True,
            'spawns': spawns_data.get('spawns', [])
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/spawns', methods=['POST'])
@login_required
def create_world_spawn(slug):
    """Crea un nuevo spawn en un mundo RPG"""
    try:
        data = request.get_json()
        
        # Validar datos requeridos
        required = ['id', 'type', 'x', 'y', 'z']
        for field in required:
            if field not in data:
                return jsonify({'success': False, 'error': f'Campo requerido: {field}'}), 400
        
        # Cargar spawns existentes
        spawns_data = rpg_manager.read_file(slug, 'spawns.json', scope='local')
        if spawns_data is None:
            spawns_data = {"spawns": []}
        
        # Verificar que no exista el ID
        if any(s['id'] == data['id'] for s in spawns_data['spawns']):
            return jsonify({'success': False, 'error': 'Ya existe un spawn con ese ID'}), 400
        
        # Crear nuevo spawn
        new_spawn = {
            'id': data['id'],
            'type': data['type'],  # 'item', 'mob', 'npc'
            'item': data.get('item', ''),
            'entity_type': data.get('entity_type', ''),
            'x': data['x'],
            'y': data['y'],
            'z': data['z'],
            'respawn_enabled': data.get('respawn_enabled', True),
            'respawn_time_seconds': data.get('respawn_time_seconds', 300),
            'respawn_on_death': data.get('respawn_on_death', True),
            'respawn_on_use': data.get('respawn_on_use', False),
            'enabled': data.get('enabled', True)
        }
        
        spawns_data['spawns'].append(new_spawn)
        
        # Guardar
        if not rpg_manager.write_file(slug, 'spawns.json', spawns_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Spawn creado correctamente',
            'spawn': new_spawn
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/spawns/<spawn_id>', methods=['PUT'])
@login_required
def update_world_spawn(slug, spawn_id):
    """Actualiza un spawn existente"""
    try:
        data = request.get_json()
        
        # Cargar spawns existentes
        spawns_data = rpg_manager.read_file(slug, 'spawns.json', scope='local')
        if spawns_data is None:
            return jsonify({'success': False, 'error': 'No se encontraron spawns'}), 404
        
        # Buscar spawn
        spawn_found = False
        for i, spawn in enumerate(spawns_data['spawns']):
            if spawn['id'] == spawn_id:
                # Actualizar campos
                spawns_data['spawns'][i].update(data)
                spawn_found = True
                break
        
        if not spawn_found:
            return jsonify({'success': False, 'error': 'Spawn no encontrado'}), 404
        
        # Guardar
        if not rpg_manager.write_file(slug, 'spawns.json', spawns_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Spawn actualizado correctamente'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/spawns/<spawn_id>', methods=['DELETE'])
@login_required
def delete_world_spawn(slug, spawn_id):
    """Elimina un spawn"""
    try:
        # Cargar spawns existentes
        spawns_data = rpg_manager.read_file(slug, 'spawns.json', scope='local')
        if spawns_data is None:
            return jsonify({'success': False, 'error': 'No se encontraron spawns'}), 404
        
        # Filtrar spawn
        original_count = len(spawns_data['spawns'])
        spawns_data['spawns'] = [s for s in spawns_data['spawns'] if s['id'] != spawn_id]
        
        if len(spawns_data['spawns']) == original_count:
            return jsonify({'success': False, 'error': 'Spawn no encontrado'}), 404
        
        # Guardar
        if not rpg_manager.write_file(slug, 'spawns.json', spawns_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Spawn eliminado correctamente'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================================================
# ENDPOINTS PARA GESTIÓN DE DUNGEONS
# ============================================================================

@app.route('/api/worlds/<slug>/rpg/dungeons', methods=['GET'])
@login_required
def get_world_dungeons(slug):
    """Obtiene todas las dungeons configuradas de un mundo RPG"""
    try:
        dungeons_data = rpg_manager.read_file(slug, 'dungeons.json', scope='local')
        
        if dungeons_data is None:
            dungeons_data = {"dungeons": []}
        
        return jsonify({
            'success': True,
            'dungeons': dungeons_data.get('dungeons', [])
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/dungeons', methods=['POST'])
@login_required
def create_world_dungeon(slug):
    """Crea una nueva dungeon en un mundo RPG"""
    try:
        data = request.get_json()
        
        # Validar datos requeridos
        required = ['id', 'name']
        for field in required:
            if field not in data:
                return jsonify({'success': False, 'error': f'Campo requerido: {field}'}), 400
        
        # Cargar dungeons existentes
        dungeons_data = rpg_manager.read_file(slug, 'dungeons.json', scope='local')
        if dungeons_data is None:
            dungeons_data = {"dungeons": []}
        
        # Verificar que no exista el ID
        if any(d['id'] == data['id'] for d in dungeons_data['dungeons']):
            return jsonify({'success': False, 'error': 'Ya existe una dungeon con ese ID'}), 400
        
        # Crear nueva dungeon
        new_dungeon = {
            'id': data['id'],
            'name': data['name'],
            'description': data.get('description', ''),
            'location': data.get('location', {}),
            'min_level': data.get('min_level', 1),
            'max_level': data.get('max_level', 100),
            'difficulty': data.get('difficulty', 'normal'),
            'rooms': data.get('rooms', []),
            'boss': data.get('boss', {}),
            'rewards': data.get('rewards', []),
            'enabled': data.get('enabled', True)
        }
        
        dungeons_data['dungeons'].append(new_dungeon)
        
        # Guardar
        if not rpg_manager.write_file(slug, 'dungeons.json', dungeons_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Dungeon creada correctamente',
            'dungeon': new_dungeon
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/dungeons/<dungeon_id>', methods=['PUT'])
@login_required
def update_world_dungeon(slug, dungeon_id):
    """Actualiza una dungeon existente"""
    try:
        data = request.get_json()
        
        # Cargar dungeons existentes
        dungeons_data = rpg_manager.read_file(slug, 'dungeons.json', scope='local')
        if dungeons_data is None:
            return jsonify({'success': False, 'error': 'No se encontraron dungeons'}), 404
        
        # Buscar dungeon
        dungeon_found = False
        for i, dungeon in enumerate(dungeons_data['dungeons']):
            if dungeon['id'] == dungeon_id:
                # Actualizar campos
                dungeons_data['dungeons'][i].update(data)
                dungeon_found = True
                break
        
        if not dungeon_found:
            return jsonify({'success': False, 'error': 'Dungeon no encontrada'}), 404
        
        # Guardar
        if not rpg_manager.write_file(slug, 'dungeons.json', dungeons_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Dungeon actualizada correctamente'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/worlds/<slug>/rpg/dungeons/<dungeon_id>', methods=['DELETE'])
@login_required
def delete_world_dungeon(slug, dungeon_id):
    """Elimina una dungeon"""
    try:
        # Cargar dungeons existentes
        dungeons_data = rpg_manager.read_file(slug, 'dungeons.json', scope='local')
        if dungeons_data is None:
            return jsonify({'success': False, 'error': 'No se encontraron dungeons'}), 404
        
        # Filtrar dungeon
        original_count = len(dungeons_data['dungeons'])
        dungeons_data['dungeons'] = [d for d in dungeons_data['dungeons'] if d['id'] != dungeon_id]
        
        if len(dungeons_data['dungeons']) == original_count:
            return jsonify({'success': False, 'error': 'Dungeon no encontrada'}), 404
        
        # Guardar
        if not rpg_manager.write_file(slug, 'dungeons.json', dungeons_data, scope='local'):
            return jsonify({'success': False, 'error': 'Error al guardar'}), 500
        
        return jsonify({
            'success': True,
            'message': 'Dungeon eliminada correctamente'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/debug')
def debug_rpg():
    """DEBUG: Endpoint sin protección para verificar rutas"""
    debug_info = {
        'function': '_resolve_rpg_file_path',
        'status': 'Testing',
        'files': {}
    }
    
    try:
        for filename in ['npcs.json', 'quests.json', 'mobs.json']:
            path = _resolve_rpg_file_path(filename)
            exists = os.path.exists(path)
            
            debug_info['files'][filename] = {
                'path': path,
                'exists': exists,
                'size': os.path.getsize(path) if exists else 0
            }
            
            # Intentar leer el contenido
            if exists:
                try:
                    with open(path) as f:
                        data = json.load(f)
                        if 'npcs' in data:
                            debug_info['files'][filename]['count'] = len(data['npcs'])
                        elif 'quests' in data:
                            debug_info['files'][filename]['count'] = len(data['quests'])
                        else:
                            debug_info['files'][filename]['count'] = len(data)
                except Exception as e:
                    debug_info['files'][filename]['error'] = str(e)
    except Exception as e:
        debug_info['status'] = f'Error: {str(e)}'
    
    return jsonify(debug_info)

def _get_active_world_slug():
    """
    Obtiene el slug del mundo activo
    """
    try:
        world_manager = WorldManager(os.path.join(MINECRAFT_DIR, 'worlds'))
        active_world = world_manager.get_active_world()
        if active_world and active_world.slug:
            return active_world.slug
    except Exception as e:
        print(f"Error obteniendo mundo activo: {e}")
    return None

def _get_data_location(world_slug, data_type, scope='local'):
    """
    Resuelve la ruta de datos según el scope (local, universal, exclusive-local)
    
    Args:
        world_slug: slug del mundo (ej: 'mmorpg', 'survival')
        data_type: tipo de dato (npcs, quests, mobs, items, pets, etc.)
        scope: 'local' (per-world), 'universal' (global), o 'exclusive-local' (solo local)
    
    Returns:
        Ruta absoluta al archivo JSON
    
    Clasificación de datos:
        UNIVERSAL (global):
            - items.json, mobs.json, npcs.json, quests.json, enchantments.json
            Ubicación: plugins/MMORPGPlugin/data/
        
        LOCAL (per-mundo):
            - npcs.json, quests.json, mobs.json, pets.json, enchantments.json
            Ubicación: plugins/MMORPGPlugin/data/{level-name}/
        
        EXCLUSIVE-LOCAL (solo en mundo):
            - players.json, status.json, invasions.json, kills.json, respawn.json
            Ubicación: plugins/MMORPGPlugin/data/{level-name}/
    """
    
    # Data universal (siempre en raíz)
    universal_data = {
        'items', 'mobs_global', 'npcs_global', 'quests_global', 
        'enchantments_global', 'pets_global'
    }
    
    # Data que puede ser local O universal (buscar en ambas)
    hybrid_data = {'npcs', 'quests', 'mobs', 'pets', 'enchantments'}
    
    # Data exclusive-local (solo per-mundo)
    exclusive_local_data = {'players', 'status', 'invasions', 'kills', 'respawn', 'squads'}
    
    # Ubicación base
    plugin_data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
    
    # Obtener level-name del mundo activo
    level_name = None
    try:
        world_manager = WorldManager(os.path.join(MINECRAFT_DIR, 'worlds'))
        world = world_manager.get_world(world_slug)
        if world:
            props = world.get_server_properties()
            level_name = props.get('level-name') or props.get('levelname')
            if level_name:
                level_name = os.path.basename(level_name)
    except Exception as e:
        print(f"Error obteniendo level-name: {e}")
    
    # Fallback al slug si no hay level-name
    world_data_dir = level_name or world_slug
    local_path = os.path.join(plugin_data_dir, world_data_dir)
    universal_path = os.path.join(plugin_data_dir)
    
    # Resolver según clasificación
    if scope == 'universal' or data_type in universal_data:
        filename = f"{data_type}.json"
        return os.path.join(universal_path, filename)
    
    elif scope == 'exclusive-local' or data_type in exclusive_local_data:
        filename = f"{data_type}.json"
        return os.path.join(local_path, filename)
    
    elif scope == 'local' or data_type in hybrid_data:
        filename = f"{data_type}.json"
        # Preferir local si existe, sino universal
        local_file = os.path.join(local_path, filename)
        universal_file = os.path.join(universal_path, filename)
        
        if os.path.exists(local_file):
            return local_file
        return local_file  # Retornar local como default para crear si no existe
    
    # Default
    return os.path.join(local_path, f"{data_type}.json")

def _get_active_world_data_candidates():
    """Determina los nombres de carpeta que pueden contener datos RPG."""
    candidates = []

    try:
        world_manager = WorldManager(os.path.join(MINECRAFT_DIR, 'worlds'))
        active_world = world_manager.get_active_world()

        if active_world:
            # Primero, si el server.properties define level-name (ej. "world")
            props = active_world.get_server_properties()
            level_name = props.get('level-name') or props.get('levelname')
            if level_name:
                # Limpiar posibles rutas (p.ej. "worlds/world")
                clean_level = os.path.basename(level_name)
                candidates.append(clean_level)

            # Luego, el slug del mundo (ej. "mmorpg-survival")
            if active_world.slug not in candidates:
                candidates.append(active_world.slug)
    except Exception as e:
        print(f"Error obteniendo candidatos de datos RPG: {e}")

    return candidates

def _get_rpg_file_paths(filename):
    """
    Retorna las rutas local (del mundo) y universal (raíz)
    """
    data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
    candidates = _get_active_world_data_candidates()

    local_path = None
    # Preferir la primera coincidencia que exista (normalmente level-name)
    for candidate in candidates:
        candidate_path = os.path.join(data_dir, candidate, filename)
        if os.path.exists(candidate_path):
            local_path = candidate_path
            break

    # Si ninguna existe, usar el primer candidato para crear
    if not local_path and candidates:
        local_path = os.path.join(data_dir, candidates[0], filename)

    universal_path = os.path.join(data_dir, filename)

    return local_path, universal_path

def _resolve_rpg_file_path(filename):
    """
    Resuelve la ruta correcta para archivos RPG
    Busca primero en el mundo activo, luego en la raíz de /data/
    """
    data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')

    candidates = _get_active_world_data_candidates()

    # Intentar en orden de candidatos
    for candidate in candidates:
        candidate_path = os.path.join(data_dir, candidate, filename)
        if os.path.exists(candidate_path):
            return candidate_path

    # Fallback: raíz /data/
    root_path = os.path.join(data_dir, filename)
    if os.path.exists(root_path):
        return root_path

    # Si no existe, retornar la ruta con el primer candidato para creación
    if candidates:
        return os.path.join(data_dir, candidates[0], filename)

    # Último fallback
    return root_path

def _get_rpg_data_by_scope(filename, data_key):
    """
    Obtiene datos RPG separados por scope (local y universal)
    
    Args:
        filename: nombre del archivo (npcs.json, quests.json, mobs.json)
        data_key: clave en el JSON (npcs, quests, o None para mobs que es dict)
    
    Returns:
        dict con 'local' y 'universal' listas
    """
    local_path, universal_path = _get_rpg_file_paths(filename)
    
    local_data = []
    universal_data = []
    
    # Leer datos locales (del mundo activo)
    if local_path and os.path.exists(local_path):
        try:
            with open(local_path, 'r') as f:
                data = json.load(f)
                if data_key:
                    local_data = data.get(data_key, [])
                else:
                    # Para mobs que es un dict
                    local_data = []
                    for mob_id, mob_data in data.items():
                        mob_data['id'] = mob_id
                        local_data.append(mob_data)
        except Exception as e:
            print(f"Error leyendo datos locales de {filename}: {e}")
    
    # Leer datos universales (de la raíz)
    if os.path.exists(universal_path):
        try:
            with open(universal_path, 'r') as f:
                data = json.load(f)
                if data_key:
                    universal_data = data.get(data_key, [])
                else:
                    # Para mobs que es un dict
                    universal_data = []
                    for mob_id, mob_data in data.items():
                        mob_data['id'] = mob_id
                        universal_data.append(mob_data)
        except Exception as e:
            print(f"Error leyendo datos universales de {filename}: {e}")
    
    return {
        'local': local_data,
        'universal': universal_data
    }

@app.route('/api/rpg/quests')
@login_required
def get_rpg_quests():
    """Obtiene todas las quests registradas (separadas por scope: local y universal)"""
    try:
        world_slug = _get_active_world_slug()
        if not world_slug:
            return jsonify({'success': False, 'message': 'No hay mundo activo'}), 400
        
        # Obtener datos locales
        local_path = _get_data_location(world_slug, 'quests', scope='local')
        quests_local = []
        if os.path.exists(local_path):
            try:
                with open(local_path, 'r') as f:
                    data = json.load(f)
                    quests_local = data.get('quests', [])
            except Exception as e:
                print(f"Error leyendo quests local: {e}")
        
        # Obtener datos universales
        universal_path = _get_data_location(world_slug, 'quests', scope='universal')
        quests_universal = []
        if os.path.exists(universal_path):
            try:
                with open(universal_path, 'r') as f:
                    data = json.load(f)
                    quests_universal = data.get('quests', [])
            except Exception as e:
                print(f"Error leyendo quests universal: {e}")
        
        return jsonify({
            'success': True,
            'quests_local': quests_local,
            'quests_universal': quests_universal
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/npcs')
@login_required
def get_rpg_npcs():
    """Obtiene todos los NPCs registrados (separados por scope: local y universal)"""
    try:
        world_slug = _get_active_world_slug()
        if not world_slug:
            return jsonify({'success': False, 'message': 'No hay mundo activo'}), 400
        
        # Obtener datos locales
        local_path = _get_data_location(world_slug, 'npcs', scope='local')
        npcs_local = []
        if os.path.exists(local_path):
            try:
                with open(local_path, 'r') as f:
                    data = json.load(f)
                    npcs_local = data.get('npcs', [])
            except Exception as e:
                print(f"Error leyendo npcs local: {e}")
        
        # Obtener datos universales
        universal_path = _get_data_location(world_slug, 'npcs', scope='universal')
        npcs_universal = []
        if os.path.exists(universal_path):
            try:
                with open(universal_path, 'r') as f:
                    data = json.load(f)
                    npcs_universal = data.get('npcs', [])
            except Exception as e:
                print(f"Error leyendo npcs universal: {e}")
        
        return jsonify({
            'success': True,
            'npcs_local': npcs_local,
            'npcs_universal': npcs_universal
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/mobs')
@login_required
def get_rpg_mobs():
    """Obtiene todos los mobs custom registrados (separados por scope: local y universal)"""
    try:
        world_slug = _get_active_world_slug()
        if not world_slug:
            return jsonify({'success': False, 'message': 'No hay mundo activo'}), 400
        
        # Obtener mobs locales
        local_path = _get_data_location(world_slug, 'mobs', scope='local')
        mobs_local = []
        if os.path.exists(local_path):
            try:
                with open(local_path, 'r') as f:
                    data = json.load(f)
                    if isinstance(data, dict):
                        # Si es dict, convertir a lista con ID
                        for mob_id, mob_data in data.items():
                            mob_data['id'] = mob_id
                            mobs_local.append(mob_data)
                    else:
                        mobs_local = data if isinstance(data, list) else []
            except Exception as e:
                print(f"Error leyendo mobs local: {e}")
        
        # Obtener mobs universales
        universal_path = _get_data_location(world_slug, 'mobs', scope='universal')
        mobs_universal = []
        if os.path.exists(universal_path):
            try:
                with open(universal_path, 'r') as f:
                    data = json.load(f)
                    if isinstance(data, dict):
                        for mob_id, mob_data in data.items():
                            mob_data['id'] = mob_id
                            mobs_universal.append(mob_data)
                    else:
                        mobs_universal = data if isinstance(data, list) else []
            except Exception as e:
                print(f"Error leyendo mobs universal: {e}")
        
        return jsonify({
            'success': True,
            'mobs_local': mobs_local,
            'mobs_universal': mobs_universal
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/quest/create', methods=['POST'])
@login_required
def create_rpg_quest():
    """Crea una nueva quest via API"""
    try:
        quest_data = request.json
        api_folder = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'api')
        os.makedirs(api_folder, exist_ok=True)
        
        commands_file = os.path.join(api_folder, 'commands.json')
        commands = {'createQuest': quest_data}
        
        with open(commands_file, 'w') as f:
            json.dump(commands, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Quest creada (se aplicará en 5 segundos)'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/quest/<quest_id>', methods=['DELETE'])
@login_required
def delete_rpg_quest(quest_id):
    """Elimina una quest via API"""
    try:
        api_folder = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'api')
        os.makedirs(api_folder, exist_ok=True)
        
        commands_file = os.path.join(api_folder, 'commands.json')
        commands = {'deleteQuest': quest_id}
        
        with open(commands_file, 'w') as f:
            json.dump(commands, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Quest eliminada'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/npc/create', methods=['POST'])
@login_required
def create_rpg_npc():
    """Crea un nuevo NPC via API"""
    try:
        npc_data = request.json
        api_folder = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'api')
        os.makedirs(api_folder, exist_ok=True)
        
        commands_file = os.path.join(api_folder, 'commands.json')
        commands = {'createNPC': npc_data}
        
        with open(commands_file, 'w') as f:
            json.dump(commands, f, indent=2)
        
        return jsonify({'success': True, 'message': 'NPC creado (se aplicará en 5 segundos)'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/npc/<npc_id>', methods=['DELETE'])
@login_required
def delete_rpg_npc(npc_id):
    """Elimina un NPC via API"""
    try:
        api_folder = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'api')
        os.makedirs(api_folder, exist_ok=True)
        
        commands_file = os.path.join(api_folder, 'commands.json')
        commands = {'deleteNPC': npc_id}
        
        with open(commands_file, 'w') as f:
            json.dump(commands, f, indent=2)
        
        return jsonify({'success': True, 'message': 'NPC eliminado'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/mob/create', methods=['POST'])
@login_required
def create_rpg_mob():
    """Crea un nuevo mob custom via API"""
    try:
        mob_data = request.json
        api_folder = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'api')
        os.makedirs(api_folder, exist_ok=True)
        
        commands_file = os.path.join(api_folder, 'commands.json')
        commands = {'createMob': mob_data}
        
        with open(commands_file, 'w') as f:
            json.dump(commands, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Mob creado (se aplicará en 5 segundos)'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/mob/<mob_id>', methods=['DELETE'])
@login_required
def delete_rpg_mob(mob_id):
    """Elimina un mob custom via API (soporta scope: local o universal)"""
    try:
        scope = request.args.get('scope', 'local')  # Obtener scope del query string
        
        data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
        
        # Determinar ruta del archivo
        if scope == 'universal':
            mobs_file = os.path.join(data_dir, 'mobs.json')
        else:
            # Local: usar mundo activo
            active_world = _get_active_world_slug()
            mobs_file = os.path.join(data_dir, active_world, 'mobs.json')
        
        # Leer y actualizar archivo
        if os.path.exists(mobs_file):
            with open(mobs_file, 'r') as f:
                mobs = json.load(f)
            
            # Filtrar el mob
            mobs = [m for m in mobs if m.get('id') != mob_id]
            
            with open(mobs_file, 'w') as f:
                json.dump(mobs, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Mob eliminado'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/npc/save', methods=['POST'])
@login_required
def save_rpg_npc():
    """Guarda un NPC directamente en el JSON (soporta scope: local o universal)"""
    try:
        npc_data = request.json
        scope = npc_data.pop('scope', 'local')  # Extraer scope, por defecto 'local'
        
        data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
        
        # Determinar ruta
        if scope == 'universal':
            npcs_path = os.path.join(data_dir, 'npcs.json')
        else:  # local
            world_slug = _get_active_world_slug()
            if not world_slug:
                return jsonify({'success': False, 'message': 'No active world'}), 400
            npcs_path = os.path.join(data_dir, world_slug, 'npcs.json')
            os.makedirs(os.path.dirname(npcs_path), exist_ok=True)
        
        # Leer archivo existente
        if os.path.exists(npcs_path):
            with open(npcs_path, 'r') as f:
                data = json.load(f)
        else:
            data = {'npcs': []}
        
        # Agregar o actualizar NPC
        npc_id = npc_data.get('id')
        if npc_id:
            # Actualizar
            data['npcs'] = [n for n in data['npcs'] if n.get('id') != npc_id]
        
        data['npcs'].append(npc_data)
        
        # Guardar
        with open(npcs_path, 'w') as f:
            json.dump(data, f, indent=2)
        
        return jsonify({'success': True, 'message': f'NPC guardado ({scope})'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/quest/save', methods=['POST'])
@login_required
def save_rpg_quest():
    """Guarda una Quest directamente en el JSON (soporta scope: local o universal)"""
    try:
        quest_data = request.json
        scope = quest_data.pop('scope', 'local')  # Extraer scope, por defecto 'local'
        
        data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
        
        # Determinar ruta
        if scope == 'universal':
            quests_path = os.path.join(data_dir, 'quests.json')
        else:  # local
            world_slug = _get_active_world_slug()
            if not world_slug:
                return jsonify({'success': False, 'message': 'No active world'}), 400
            quests_path = os.path.join(data_dir, world_slug, 'quests.json')
            os.makedirs(os.path.dirname(quests_path), exist_ok=True)
        
        # Leer archivo existente
        if os.path.exists(quests_path):
            with open(quests_path, 'r') as f:
                data = json.load(f)
        else:
            data = {'quests': []}
        
        # Agregar o actualizar Quest
        quest_id = quest_data.get('id')
        if quest_id:
            # Actualizar
            data['quests'] = [q for q in data['quests'] if q.get('id') != quest_id]
        
        data['quests'].append(quest_data)
        
        # Guardar
        with open(quests_path, 'w') as f:
            json.dump(data, f, indent=2)
        
        return jsonify({'success': True, 'message': f'Quest guardada ({scope})'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/mob/save', methods=['POST'])
@login_required
def save_rpg_mob():
    """Guarda un Mob directamente en el JSON (soporta scope: local o universal)"""
    try:
        mob_data = request.json
        scope = mob_data.pop('scope', 'local')  # Extraer scope, por defecto 'local'
        
        data_dir = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data')
        
        # Determinar ruta
        if scope == 'universal':
            mobs_path = os.path.join(data_dir, 'mobs.json')
        else:  # local
            world_slug = _get_active_world_slug()
            if not world_slug:
                return jsonify({'success': False, 'message': 'No active world'}), 400
            mobs_path = os.path.join(data_dir, world_slug, 'mobs.json')
            os.makedirs(os.path.dirname(mobs_path), exist_ok=True)
        
        # Leer archivo existente
        if os.path.exists(mobs_path):
            with open(mobs_path, 'r') as f:
                data = json.load(f)
        else:
            data = {}
        
        # Agregar o actualizar Mob
        mob_id = mob_data.get('id')
        if not mob_id:
            mob_id = mob_data.get('name', f'mob_{len(data)+1}').lower().replace(' ', '_')
            mob_data['id'] = mob_id
        
        data[mob_id] = mob_data
        
        # Guardar
        with open(mobs_path, 'w') as f:
            json.dump(data, f, indent=2)
        
        return jsonify({'success': True, 'message': f'Mob guardado ({scope})'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ==================== ETAPA 4: KILLS TRACKING & LOOT ====================

@app.route('/api/rpg/items', methods=['GET'])
def get_rpg_items():
    """Obtiene lista de items RPG con atributos y rarezas (siempre universales)"""
    try:
        world_slug = _get_active_world_slug()
        if not world_slug:
            return jsonify({'success': False, 'message': 'No hay mundo activo'}), 400
        
        # Items siempre se obtienen universales (scope='universal')
        items_file = _get_data_location(world_slug, 'items', scope='universal')
        
        if os.path.exists(items_file):
            with open(items_file, 'r') as f:
                data = json.load(f)
                return jsonify({
                    'success': True,
                    'items': data.get('items', []),
                    'rarities': data.get('rarities', {})
                })
        else:
            return jsonify({'success': False, 'message': 'Archivo de items no encontrado'}), 404
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/kills', methods=['GET'])
def get_kills_tracker():
    """Obtiene estadísticas de kills registradas (exclusive-local al mundo)"""
    try:
        world_slug = _get_active_world_slug()
        if not world_slug:
            return jsonify({'success': False, 'message': 'No hay mundo activo'}), 400
        
        # Kills es exclusive-local (solo del mundo actual)
        kills_file = _get_data_location(world_slug, 'kills', scope='exclusive-local')
        
        if os.path.exists(kills_file):
            with open(kills_file, 'r') as f:
                data = json.load(f)
        else:
            data = {'kills': [], 'playerStats': {}}
        
        return jsonify({
            'success': True,
            'kills': data.get('kills', []),
            'playerStats': data.get('playerStats', {})
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/kill/record', methods=['POST'])
def record_kill():
    """Registra un kill de un mob (llamado por el plugin)"""
    try:
        kill_data = request.json
        
        kills_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'kills_tracker.json')
        os.makedirs(os.path.dirname(kills_file), exist_ok=True)
        
        # Leer datos actuales
        if os.path.exists(kills_file):
            with open(kills_file, 'r') as f:
                data = json.load(f)
        else:
            data = {'kills': [], 'playerStats': {}}
        
        # Agregar nuevo kill
        kill_data['timestamp'] = datetime.now().isoformat()
        data['kills'].append(kill_data)
        
        # Actualizar estadísticas del jugador
        player = kill_data.get('playerName', 'Unknown')
        if player not in data['playerStats']:
            data['playerStats'][player] = {
                'totalKills': 0,
                'killsByMob': {},
                'totalXpGained': 0,
                'lastKillTime': None
            }
        
        data['playerStats'][player]['totalKills'] += 1
        mob_id = kill_data.get('mobId', 'unknown')
        if mob_id not in data['playerStats'][player]['killsByMob']:
            data['playerStats'][player]['killsByMob'][mob_id] = 0
        data['playerStats'][player]['killsByMob'][mob_id] += 1
        data['playerStats'][player]['totalXpGained'] += kill_data.get('xpReward', 0)
        data['playerStats'][player]['lastKillTime'] = datetime.now().isoformat()
        
        # Guardar
        with open(kills_file, 'w') as f:
            json.dump(data, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Kill registrado'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/quest-progress', methods=['GET'])
def get_quest_progress():
    """Obtiene progreso de quests con objetivos KILL_MOB"""
    try:
        player = request.args.get('player', '')
        quest_id = request.args.get('quest', '')
        
        kills_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'kills_tracker.json')
        quests_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'quests.json')
        
        kills_data = {}
        if os.path.exists(kills_file):
            with open(kills_file, 'r') as f:
                kills_data = json.load(f)
        
        quests_data = {}
        if os.path.exists(quests_file):
            with open(quests_file, 'r') as f:
                quests_data = json.load(f)
        
        # Filtrar quests con objetivos KILL_MOB
        kill_mob_quests = []
        for quest in quests_data.get('quests', []):
            if quest_id and quest['id'] != quest_id:
                continue
            
            has_kill_obj = any(obj.get('type') == 'KILL_MOB' for obj in quest.get('objectives', []))
            if has_kill_obj:
                # Calcular progreso
                progress = {}
                player_stats = kills_data.get('playerStats', {}).get(player, {})
                kills_by_mob = player_stats.get('killsByMob', {})
                
                for obj in quest.get('objectives', []):
                    if obj.get('type') == 'KILL_MOB':
                        mob_id = obj.get('mobId')
                        required = obj.get('required', 0)
                        current = kills_by_mob.get(mob_id, 0)
                        progress[obj['id']] = {
                            'current': current,
                            'required': required,
                            'completed': current >= required
                        }
                
                quest['progress'] = progress
                kill_mob_quests.append(quest)
        
        return jsonify({
            'success': True,
            'quests': kill_mob_quests,
            'playerKills': kills_data.get('playerStats', {}).get(player, {})
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/stats/kills', methods=['GET'])
def get_kills_statistics():
    """Obtiene estadísticas completas de kills con filtros"""
    try:
        kills_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'kills_tracker.json')
        player_filter = request.args.get('player', '')
        mob_filter = request.args.get('mob', '')
        
        if not os.path.exists(kills_file):
            return jsonify({'success': True, 'stats': {}, 'summary': {}})
        
        with open(kills_file, 'r') as f:
            data = json.load(f)
        
        kills = data.get('kills', [])
        player_stats = data.get('playerStats', {})
        
        # Filtrar kills
        filtered_kills = kills
        if player_filter:
            filtered_kills = [k for k in filtered_kills if k.get('playerName') == player_filter]
        if mob_filter:
            filtered_kills = [k for k in filtered_kills if k.get('mobId') == mob_filter]
        
        # Calcular resumen
        summary = {
            'totalKills': len(filtered_kills),
            'totalPlayers': len(player_stats),
            'totalXpGained': sum(k.get('xpReward', 0) for k in filtered_kills),
            'playerStats': {}
        }
        
        # Stats por jugador
        for pname, pstats in player_stats.items():
            if player_filter and pname != player_filter:
                continue
            
            summary['playerStats'][pname] = {
                'totalKills': pstats.get('totalKills', 0),
                'totalXpGained': pstats.get('totalXpGained', 0),
                'lastKillTime': pstats.get('lastKillTime', ''),
                'killsByMob': pstats.get('killsByMob', {})
            }
        
        return jsonify({
            'success': True,
            'kills': filtered_kills,
            'summary': summary
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/stats/mobs', methods=['GET'])
def get_mobs_kill_statistics():
    """Obtiene estadísticas de kills por mob"""
    try:
        kills_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'kills_tracker.json')
        mobs_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'mobs.json')
        
        if not os.path.exists(kills_file):
            return jsonify({'success': True, 'mobStats': {}})
        
        with open(kills_file, 'r') as f:
            kills_data = json.load(f)
        
        with open(mobs_file, 'r') as f:
            mobs_data = json.load(f)
        
        kills = kills_data.get('kills', [])
        
        # Contar kills por mob
        mob_kill_counts = {}
        for kill in kills:
            mob_id = kill.get('mobId')
            if mob_id:
                if mob_id not in mob_kill_counts:
                    mob_kill_counts[mob_id] = {'count': 0, 'totalXp': 0, 'players': set()}
                mob_kill_counts[mob_id]['count'] += 1
                mob_kill_counts[mob_id]['totalXp'] += kill.get('xpReward', 0)
                mob_kill_counts[mob_id]['players'].add(kill.get('playerName', 'Unknown'))
        
        # Enriquecer con datos del mob
        mob_stats = {}
        for mob_id, counts in mob_kill_counts.items():
            mob_info = mobs_data.get(mob_id, {})
            mob_stats[mob_id] = {
                'name': mob_info.get('name', mob_id),
                'totalKills': counts['count'],
                'totalXpDropped': counts['totalXp'],
                'playersKilled': list(counts['players']),
                'averageXpPerKill': counts['totalXp'] / counts['count'] if counts['count'] > 0 else 0
            }
        
        return jsonify({
            'success': True,
            'mobStats': mob_stats
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/stats/timeline', methods=['GET'])
def get_kills_timeline():
    """Obtiene timeline de kills para gráficos"""
    try:
        kills_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'kills_tracker.json')
        player_filter = request.args.get('player', '')
        
        if not os.path.exists(kills_file):
            return jsonify({'success': True, 'timeline': []})
        
        with open(kills_file, 'r') as f:
            data = json.load(f)
        
        kills = data.get('kills', [])
        if player_filter:
            kills = [k for k in kills if k.get('playerName') == player_filter]
        
        # Agrupar por fecha (día)
        timeline = {}
        for kill in kills:
            timestamp = kill.get('timestamp', '')
            if timestamp:
                date_str = timestamp.split('T')[0]  # YYYY-MM-DD
                if date_str not in timeline:
                    timeline[date_str] = {'kills': 0, 'xp': 0}
                timeline[date_str]['kills'] += 1
                timeline[date_str]['xp'] += kill.get('xpReward', 0)
        
        # Convertir a lista ordenada
        timeline_list = [
            {'date': date, 'kills': data['kills'], 'xp': data['xp']}
            for date, data in sorted(timeline.items())
        ]
        
        return jsonify({
            'success': True,
            'timeline': timeline_list
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# API: Sistema de Respawn de Mobs
@app.route('/api/rpg/respawn/zones', methods=['GET'])
@login_required
def get_respawn_zones():
    """Obtiene todas las zonas de respawn configuradas"""
    try:
        respawn_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'respawn_config.json')
        
        if not os.path.exists(respawn_file):
            return jsonify({'success': True, 'zones': {}, 'enabled': False})
        
        with open(respawn_file, 'r') as f:
            config = json.load(f)
        
        zones = config.get('respawnZones', {})
        global_settings = config.get('globalSettings', {})
        
        # Enriquecer zonas con información de estado
        for zone_id, zone_data in zones.items():
            zone_data['zoneId'] = zone_id
            zone_data['nextRespawn'] = zone_data.get('respawnInterval', 60)
            zone_data['currentMobs'] = 0  # Será calculado por el plugin
        
        return jsonify({
            'success': True,
            'zones': zones,
            'globalSettings': global_settings,
            'enabled': global_settings.get('respawnEnabled', True)
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/respawn/zones/<zone_id>', methods=['PUT'])
@login_required
def update_respawn_zone(zone_id):
    """Actualiza configuración de una zona de respawn"""
    try:
        data = request.get_json()
        respawn_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'respawn_config.json')
        
        if not os.path.exists(respawn_file):
            return jsonify({'success': False, 'message': 'Archivo de config no existe'}), 404
        
        with open(respawn_file, 'r') as f:
            config = json.load(f)
        
        if zone_id not in config.get('respawnZones', {}):
            return jsonify({'success': False, 'message': 'Zona no encontrada'}), 404
        
        zone = config['respawnZones'][zone_id]
        
        # Actualizar campos permitidos
        if 'enabled' in data:
            zone['enabled'] = data['enabled']
        if 'maxMobs' in data:
            zone['maxMobs'] = int(data['maxMobs'])
        if 'respawnInterval' in data:
            zone['respawnInterval'] = int(data['respawnInterval'])
        
        # Guardar cambios
        with open(respawn_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': f'Zona {zone_id} actualizada',
            'zone': zone
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/respawn/global', methods=['PUT'])
@login_required
def update_global_respawn_settings():
    """Actualiza configuración global de respawn"""
    try:
        data = request.get_json()
        respawn_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'respawn_config.json')
        
        if not os.path.exists(respawn_file):
            return jsonify({'success': False, 'message': 'Archivo de config no existe'}), 404
        
        with open(respawn_file, 'r') as f:
            config = json.load(f)
        
        if 'globalSettings' not in config:
            config['globalSettings'] = {}
        
        global_settings = config['globalSettings']
        
        if 'respawnEnabled' in data:
            global_settings['respawnEnabled'] = data['respawnEnabled']
        if 'checkInterval' in data:
            global_settings['checkInterval'] = int(data['checkInterval'])
        if 'logRespawns' in data:
            global_settings['logRespawns'] = data['logRespawns']
        
        # Guardar cambios
        with open(respawn_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Configuración global actualizada',
            'globalSettings': global_settings
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================
# BESTIARY ENDPOINTS
# ============================================

@app.route('/bestiary')
@login_required
def bestiary_panel():
    """Renderiza el panel de bestiario"""
    return render_template('bestiary_panel.html')

@app.route('/api/rpg/bestiary/<player>', methods=['GET'])
@login_required
def get_player_bestiary(player):
    """Obtiene el bestiario de un jugador específico"""
    try:
        # Conectar a BD SQLite del plugin
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpg.db')
        
        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404
        
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Buscar UUID del jugador
        cursor.execute("SELECT uuid FROM players WHERE name = ?", (player,))
        result = cursor.fetchone()
        
        if not result:
            conn.close()
            return jsonify({'success': False, 'message': 'Jugador no encontrado'}), 404
        
        player_uuid = result[0]
        
        # Obtener entradas del bestiario
        cursor.execute("""
            SELECT mob_id, kills, first_kill_date, last_kill_date, current_tier, discovered
            FROM player_bestiary
            WHERE player_uuid = ?
        """, (player_uuid,))
        
        entries = []
        total_kills = 0
        total_discoveries = 0
        
        for row in cursor.fetchall():
            entry = {
                'mobId': row[0],
                'kills': row[1],
                'firstKillDate': row[2],
                'lastKillDate': row[3],
                'currentTier': row[4],
                'discovered': bool(row[5])
            }
            entries.append(entry)
            total_kills += row[1]
            if row[5]:
                total_discoveries += 1
        
        conn.close()
        
        return jsonify({
            'success': True,
            'player': player,
            'playerUUID': player_uuid,
            'entries': entries,
            'totalKills': total_kills,
            'totalDiscoveries': total_discoveries
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/bestiary/config', methods=['GET'])
@login_required
def get_bestiary_config():
    """Obtiene la configuración del bestiario"""
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'bestiary_config.json')
        
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404
        
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        return jsonify({
            'success': True,
            'config': config
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/bestiary/config', methods=['PUT'])
@login_required
def update_bestiary_config():
    """Actualiza la configuración del bestiario"""
    try:
        data = request.get_json()
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'bestiary_config.json')
        
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404
        
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        # Actualizar campos permitidos
        if 'enabled' in data:
            config['enabled'] = data['enabled']
        
        if 'progressThresholds' in data:
            config['progressThresholds'] = data['progressThresholds']
        
        if 'discoveryRewards' in data:
            config['discoveryRewards'] = data['discoveryRewards']
        
        # Guardar cambios
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Configuración actualizada',
            'config': config
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/bestiary/stats', methods=['GET'])
@login_required
def get_bestiary_stats():
    """Obtiene estadísticas globales del bestiario"""
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpg.db')
        
        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404
        
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Top 10 jugadores por descubrimientos
        cursor.execute("""
            SELECT p.name, COUNT(DISTINCT pb.mob_id) as discoveries
            FROM player_bestiary pb
            JOIN players p ON pb.player_uuid = p.uuid
            WHERE pb.discovered = 1
            GROUP BY pb.player_uuid
            ORDER BY discoveries DESC
            LIMIT 10
        """)
        
        top_players = [{'player': row[0], 'discoveries': row[1]} for row in cursor.fetchall()]
        
        # Mob más matado
        cursor.execute("""
            SELECT mob_id, SUM(kills) as total_kills
            FROM player_bestiary
            GROUP BY mob_id
            ORDER BY total_kills DESC
            LIMIT 1
        """)
        
        most_killed_row = cursor.fetchone()
        most_killed = {'mobId': most_killed_row[0], 'kills': most_killed_row[1]} if most_killed_row else None
        
        # Total de descubrimientos únicos
        cursor.execute("SELECT COUNT(DISTINCT mob_id) FROM player_bestiary WHERE discovered = 1")
        total_unique_discoveries = cursor.fetchone()[0]
        
        # Total de kills globales
        cursor.execute("SELECT SUM(kills) FROM player_bestiary")
        total_global_kills = cursor.fetchone()[0] or 0
        
        conn.close()
        
        return jsonify({
            'success': True,
            'topPlayers': top_players,
            'mostKilledMob': most_killed,
            'totalUniqueDiscoveries': total_unique_discoveries,
            'totalGlobalKills': total_global_kills
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/bestiary/category', methods=['POST'])
@login_required
def create_bestiary_category():
    """Crea una nueva categoría de bestiario"""
    try:
        data = request.get_json()
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'bestiary_config.json')
        
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404
        
        # Validar datos requeridos
        if not data.get('id') or not data.get('name') or not data.get('mobs'):
            return jsonify({'success': False, 'message': 'Faltan campos requeridos (id, name, mobs)'}), 400
        
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        if 'categories' not in config:
            config['categories'] = {}
        
        category_id = data['id']
        
        # Verificar que no exista
        if category_id in config['categories']:
            return jsonify({'success': False, 'message': 'La categoría ya existe'}), 409
        
        # Crear categoría
        new_category = {
            'name': data['name'],
            'description': data.get('description', ''),
            'mobs': data['mobs']
        }
        
        # Agregar recompensa si existe
        if 'completionReward' in data:
            new_category['completionReward'] = data['completionReward']
        
        config['categories'][category_id] = new_category
        
        # Guardar
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Categoría creada',
            'category': new_category
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/bestiary/category/<category_id>', methods=['DELETE'])
@login_required
def delete_bestiary_category(category_id):
    """Elimina una categoría de bestiario"""
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'bestiary_config.json')
        
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404
        
        with open(config_file, 'r') as f:
            config = json.load(f)
        
        if 'categories' not in config or category_id not in config['categories']:
            return jsonify({'success': False, 'message': 'Categoría no encontrada'}), 404
        
        # Eliminar categoría
        del config['categories'][category_id]
        
        # Guardar
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        
        return jsonify({
            'success': True,
            'message': 'Categoría eliminada'
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================
# ACHIEVEMENTS ENDPOINTS
# ============================================

@app.route('/achievements')
@login_required
def achievements_panel():
    """Renderiza el panel de logros"""
    return render_template('achievements_panel.html')


@app.route('/api/rpg/achievements/<player>', methods=['GET'])
@login_required
def get_player_achievements(player):
    """Obtiene el progreso de logros de un jugador"""
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpg.db')

        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        cursor.execute("SELECT uuid FROM players WHERE name = ?", (player,))
        result = cursor.fetchone()
        if not result:
            conn.close()
            return jsonify({'success': False, 'message': 'Jugador no encontrado'}), 404

        player_uuid = result[0]

        cursor.execute("""
            SELECT achievement_id, progress, completed, completed_at
            FROM player_achievements
            WHERE player_uuid = ?
        """, (player_uuid,))

        progress_list = []
        for row in cursor.fetchall():
            progress_list.append({
                'id': row[0],
                'progress': row[1],
                'completed': bool(row[2]),
                'completedAt': row[3]
            })

        conn.close()

        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'achievements_config.json')
        definitions = {}
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
                definitions = config.get('achievements', {})

        return jsonify({
            'success': True,
            'player': player,
            'playerUUID': player_uuid,
            'progress': progress_list,
            'definitions': definitions
        })

    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/achievements/config', methods=['GET'])
@login_required
def get_achievements_config():
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'achievements_config.json')
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        with open(config_file, 'r') as f:
            config = json.load(f)

        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/achievements/config', methods=['PUT'])
@login_required
def update_achievements_config():
    try:
        data = request.get_json()
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'achievements_config.json')

        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        with open(config_file, 'r') as f:
            config = json.load(f)

        if 'enabled' in data:
            config['enabled'] = data['enabled']

        if 'achievements' in data:
            config['achievements'] = data['achievements']

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/achievements/stats', methods=['GET'])
@login_required
def get_achievements_stats():
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpg.db')

        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT achievement_id, COUNT(*) as completions
            FROM player_achievements
            WHERE completed = 1
            GROUP BY achievement_id
            ORDER BY completions DESC
        """)

        leaderboard = []
        for row in cursor.fetchall():
            leaderboard.append({
                'id': row[0],
                'completions': row[1]
            })

        cursor.execute("SELECT COUNT(*) FROM player_achievements WHERE completed = 1")
        total_completions = cursor.fetchone()[0] or 0

        cursor.execute("SELECT COUNT(DISTINCT player_uuid) FROM player_achievements")
        total_players = cursor.fetchone()[0] or 0

        conn.close()

        return jsonify({
            'success': True,
            'leaderboard': leaderboard,
            'totalCompletions': total_completions,
            'totalPlayers': total_players
        })

    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/achievements/achievement', methods=['POST'])
@login_required
def create_achievement():
    try:
        data = request.get_json()
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'achievements_config.json')

        if not data.get('id') or not data.get('name') or not data.get('trigger'):
            return jsonify({'success': False, 'message': 'Faltan campos requeridos (id, name, trigger)'}), 400

        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        with open(config_file, 'r') as f:
            config = json.load(f)

        if 'achievements' not in config:
            config['achievements'] = {}

        ach_id = data['id']
        if ach_id in config['achievements']:
            return jsonify({'success': False, 'message': 'El logro ya existe'}), 409

        config['achievements'][ach_id] = {
            'name': data['name'],
            'description': data.get('description', ''),
            'trigger': data['trigger'],
            'reward': data.get('reward', {})
        }

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'achievement': config['achievements'][ach_id]})

    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/achievements/achievement/<achievement_id>', methods=['DELETE'])
@login_required
def delete_achievement(achievement_id):
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'achievements_config.json')
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        with open(config_file, 'r') as f:
            config = json.load(f)

        if 'achievements' not in config or achievement_id not in config['achievements']:
            return jsonify({'success': False, 'message': 'Logro no encontrado'}), 404

        del config['achievements'][achievement_id]

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'message': 'Logro eliminado'})

    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================
# RANKS/TITLES ENDPOINTS
# ============================================


@app.route('/ranks')
@login_required
def ranks_panel():
    """Renderiza el panel de rangos"""
    return render_template('ranks_panel.html')


def _get_player_uuid(conn, player_name):
    cursor = conn.cursor()
    cursor.execute("SELECT uuid FROM players WHERE name = ?", (player_name,))
    result = cursor.fetchone()
    return result[0] if result else None


def _load_ranks_config():
    config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'ranks_config.json')
    if not os.path.exists(config_file):
        return None, config_file
    with open(config_file, 'r') as f:
        return json.load(f), config_file


@app.route('/api/rpg/ranks/config', methods=['GET'])
@login_required
def get_ranks_config():
    try:
        config, _ = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404
        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/config', methods=['PUT'])
@login_required
def update_ranks_config():
    try:
        data = request.get_json()
        config, config_file = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        if 'enabled' in data:
            config['enabled'] = data['enabled']

        if 'ranks' in data:
            config['ranks'] = data['ranks']

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/rank', methods=['POST'])
@login_required
def create_rank():
    try:
        data = request.get_json()
        if not data.get('id') or not data.get('name'):
            return jsonify({'success': False, 'message': 'Faltan campos requeridos (id, name)'}), 400

        config, config_file = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        if 'ranks' not in config:
            config['ranks'] = {}

        rank_id = data['id']
        if rank_id in config['ranks']:
            return jsonify({'success': False, 'message': 'El rango ya existe'}), 409

        config['ranks'][rank_id] = {
            'name': data['name'],
            'description': data.get('description', ''),
            'order': data.get('order', 0),
            'requirements': data.get('requirements', {}),
            'reward': data.get('reward', {})
        }

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'rank': config['ranks'][rank_id]})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/rank/<rank_id>', methods=['DELETE'])
@login_required
def delete_rank(rank_id):
    try:
        config, config_file = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        if 'ranks' not in config or rank_id not in config['ranks']:
            return jsonify({'success': False, 'message': 'Rango no encontrado'}), 404

        del config['ranks'][rank_id]

        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)

        return jsonify({'success': True, 'message': 'Rango eliminado'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/stats', methods=['GET'])
@login_required
def get_ranks_stats():
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')

        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT rank_id, COUNT(*) as total
            FROM player_ranks
            GROUP BY rank_id
            ORDER BY total DESC
        """)

        leaderboard = []
        for row in cursor.fetchall():
            leaderboard.append({
                'rankId': row[0],
                'total': row[1]
            })

        cursor.execute("SELECT COUNT(*) FROM player_ranks")
        total_assignments = cursor.fetchone()[0] or 0

        cursor.execute("SELECT COUNT(*) FROM players")
        total_players = cursor.fetchone()[0] or 0

        conn.close()

        return jsonify({
            'success': True,
            'leaderboard': leaderboard,
            'totalAssignments': total_assignments,
            'totalPlayers': total_players
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks', methods=['GET'])
@login_required
def list_ranks():
    try:
        config, _ = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        ranks = config.get('ranks', {})

        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        counts = {}
        if os.path.exists(db_path):
            conn = sqlite3.connect(db_path)
            cursor = conn.cursor()
            cursor.execute("SELECT rank_id, COUNT(*) FROM player_ranks GROUP BY rank_id")
            for row in cursor.fetchall():
                counts[row[0]] = row[1]
            conn.close()

        return jsonify({
            'success': True,
            'enabled': config.get('enabled', True),
            'ranks': ranks,
            'counts': counts
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/player/<player>', methods=['GET'])
@login_required
def get_player_rank(player):
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')

        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404

        config, _ = _load_ranks_config()
        if config is None:
            return jsonify({'success': False, 'message': 'Configuración no existe'}), 404

        conn = sqlite3.connect(db_path)
        player_uuid = _get_player_uuid(conn, player)
        if not player_uuid:
            conn.close()
            return jsonify({'success': False, 'message': 'Jugador no encontrado'}), 404

        cursor = conn.cursor()
        cursor.execute("SELECT rank_id, title, updated_at FROM player_ranks WHERE player_uuid = ?", (player_uuid,))
        row = cursor.fetchone()
        conn.close()

        rank_info = None
        if row:
            rank_info = {
                'rankId': row[0],
                'title': row[1],
                'updatedAt': row[2]
            }

        return jsonify({
            'success': True,
            'player': player,
            'playerUUID': player_uuid,
            'rank': rank_info,
            'definitions': config.get('ranks', {})
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500


@app.route('/api/rpg/ranks/assign', methods=['POST'])
@login_required
def assign_rank():
    try:
        import sqlite3
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': False, 'message': 'Base de datos no existe'}), 404

        data = request.get_json()
        player = data.get('player')
        rank_id = data.get('rankId')

        if not player or not rank_id:
            return jsonify({'success': False, 'message': 'Se requieren player y rankId'}), 400

        config, _ = _load_ranks_config()
        if config is None or 'ranks' not in config or rank_id not in config['ranks']:
            return jsonify({'success': False, 'message': 'Rango no encontrado en configuración'}), 404

        conn = sqlite3.connect(db_path)
        player_uuid = _get_player_uuid(conn, player)
        if not player_uuid:
            conn.close()
            return jsonify({'success': False, 'message': 'Jugador no encontrado'}), 404

        title = config['ranks'][rank_id].get('reward', {}).get('title')

        cursor = conn.cursor()
        cursor.execute(
            "INSERT OR REPLACE INTO player_ranks (player_uuid, rank_id, title, updated_at) VALUES (?, ?, ?, ?)",
            (player_uuid, rank_id, title, datetime.utcnow().isoformat())
        )
        conn.commit()
        conn.close()

        return jsonify({'success': True, 'message': 'Rango asignado', 'rankId': rank_id})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# =====================================================
# INVASIONES - API ENDPOINTS
# =====================================================

@app.route('/invasions')
@login_required
def invasions_panel():
    """Panel de gestión de invasiones"""
    return render_template('invasions_panel.html')

@app.route('/api/rpg/invasions/config', methods=['GET'])
@login_required
def get_invasions_config():
    """Obtiene la configuración de invasiones"""
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'invasions_config.json')
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Archivo de configuración no encontrado'}), 404
        
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/config', methods=['PUT'])
@login_required
def update_invasions_config():
    """Actualiza la configuración de invasiones"""
    try:
        data = request.json
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'data', 'invasions_config.json')
        
        # Guardar nueva configuración
        os.makedirs(os.path.dirname(config_file), exist_ok=True)
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        return jsonify({'success': True, 'message': 'Configuración actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/active', methods=['GET'])
@login_required
def get_active_invasions():
    """Obtiene invasiones activas en tiempo real"""
    try:
        # Esto debería conectarse al plugin vía RCON o API
        # Por ahora retornamos datos de ejemplo
        active_invasions = []
        
        # TODO: Implementar consulta real al plugin
        # Por ahora, retornamos vacío ya que es un sistema nuevo
        
        return jsonify({'success': True, 'invasions': active_invasions})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/history', methods=['GET'])
@login_required
def get_invasions_history():
    """Obtiene historial de invasiones desde la base de datos"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'history': []})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Obtener últimas 50 invasiones
        cursor.execute("""
            SELECT session_id, invasion_id, world_name, start_time, end_time,
                   total_waves, completed_waves, status, total_mobs_killed,
                   total_mobs_spawned, success, duration_seconds, top_player_uuid,
                   top_player_kills
            FROM invasion_history
            ORDER BY start_time DESC
            LIMIT 50
        """)
        
        rows = cursor.fetchall()
        history = []
        
        for row in rows:
            # Obtener participantes
            cursor.execute("""
                SELECT player_uuid, kills
                FROM invasion_participants
                WHERE session_id = ?
                ORDER BY kills DESC
            """, (row[0],))
            
            participants = []
            participant_rows = cursor.fetchall()
            for p_row in participant_rows:
                player_name = _get_player_name(p_row[0])
                participants.append({
                    'uuid': p_row[0],
                    'name': player_name,
                    'kills': p_row[1]
                })
            
            history.append({
                'sessionId': row[0],
                'invasionId': row[1],
                'worldName': row[2],
                'startTime': row[3],
                'endTime': row[4],
                'totalWaves': row[5],
                'completedWaves': row[6],
                'status': row[7],
                'totalMobsKilled': row[8],
                'totalMobsSpawned': row[9],
                'success': bool(row[10]),
                'durationSeconds': row[11],
                'topPlayerUuid': row[12],
                'topPlayerKills': row[13],
                'participants': participants
            })
        
        conn.close()
        return jsonify({'success': True, 'history': history})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/stats', methods=['GET'])
@login_required
def get_invasions_stats():
    """Obtiene estadísticas globales de invasiones"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'stats': {
                'totalInvasions': 0,
                'successRate': 0,
                'totalMobsKilled': 0,
                'avgDuration': 0,
                'topPlayers': []
            }})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Estadísticas generales
        cursor.execute("""
            SELECT COUNT(*), 
                   SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END),
                   SUM(total_mobs_killed),
                   AVG(duration_seconds)
            FROM invasion_history
        """)
        
        stats_row = cursor.fetchone()
        total_invasions = stats_row[0] or 0
        total_success = stats_row[1] or 0
        total_mobs = stats_row[2] or 0
        avg_duration = stats_row[3] or 0
        
        success_rate = (total_success / total_invasions * 100) if total_invasions > 0 else 0
        
        # Top jugadores por kills en invasiones
        cursor.execute("""
            SELECT player_uuid, SUM(kills) as total_kills
            FROM invasion_participants
            GROUP BY player_uuid
            ORDER BY total_kills DESC
            LIMIT 10
        """)
        
        top_players = []
        for row in cursor.fetchall():
            player_name = _get_player_name(row[0])
            top_players.append({
                'uuid': row[0],
                'name': player_name,
                'totalKills': row[1]
            })
        
        conn.close()
        
        return jsonify({
            'success': True,
            'stats': {
                'totalInvasions': total_invasions,
                'successRate': round(success_rate, 2),
                'totalMobsKilled': total_mobs,
                'avgDuration': round(avg_duration, 2),
                'topPlayers': top_players
            }
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/start', methods=['POST'])
@login_required
def start_invasion_manual():
    """Inicia una invasión manualmente"""
    try:
        data = request.json
        invasion_id = data.get('invasionId')
        
        if not invasion_id:
            return jsonify({'success': False, 'message': 'ID de invasión requerido'}), 400
        
        # TODO: Implementar llamada al plugin vía comando de consola
        # Por ahora retornamos éxito simulado
        command = f"rcon-cli \"invasion start {invasion_id}\""
        # result = subprocess.run(command, shell=True, capture_output=True, text=True)
        
        return jsonify({
            'success': True,
            'message': f'Invasión {invasion_id} iniciada (implementar RCON)',
            'sessionId': 'temp-session-id'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/invasions/stop', methods=['POST'])
@login_required
def stop_invasion():
    """Detiene una invasión activa"""
    try:
        data = request.json
        session_id = data.get('sessionId')
        
        if not session_id:
            return jsonify({'success': False, 'message': 'ID de sesión requerido'}), 400
        
        # TODO: Implementar llamada al plugin vía comando de consola
        command = f"rcon-cli \"invasion stop {session_id}\""
        # result = subprocess.run(command, shell=True, capture_output=True, text=True)
        
        return jsonify({
            'success': True,
            'message': f'Invasión {session_id} detenida (implementar RCON)'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# =====================================================
# EVENTOS TEMÁTICOS - API ENDPOINTS
# =====================================================

@app.route('/events')
@login_required
def events_panel():
    """Panel de gestión de eventos temáticos"""
    return render_template('events_panel.html')

@app.route('/api/rpg/events/config', methods=['GET'])
@login_required
def get_events_config():
    """Obtiene la configuración de eventos temáticos"""
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'events_config.json')
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Archivo de configuración no encontrado'}), 404
        
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/config', methods=['PUT'])
@login_required
def update_events_config():
    """Actualiza la configuración de eventos"""
    try:
        data = request.json
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'events_config.json')
        
        # Guardar nueva configuración
        os.makedirs(os.path.dirname(config_file), exist_ok=True)
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        return jsonify({'success': True, 'message': 'Configuración actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/active', methods=['GET'])
@login_required
def get_active_events():
    """Obtiene eventos activos"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'events': []})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT id, event_id, event_name, started_at, participants, total_kills, world
            FROM event_history
            WHERE status = 'ACTIVE'
            ORDER BY started_at DESC
        """)
        
        rows = cursor.fetchall()
        events = []
        
        for row in rows:
            events.append({
                'historyId': row[0],
                'eventId': row[1],
                'eventName': row[2],
                'startedAt': row[3],
                'participants': row[4],
                'totalKills': row[5],
                'world': row[6]
            })
        
        conn.close()
        return jsonify({'success': True, 'events': events})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/history', methods=['GET'])
@login_required
def get_events_history():
    """Obtiene historial de eventos desde la base de datos"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'history': []})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Obtener últimas 50 eventos
        cursor.execute("""
            SELECT id, event_id, event_name, started_at, ended_at,
                   participants, total_kills, status, world
            FROM event_history
            WHERE status != 'ACTIVE'
            ORDER BY started_at DESC
            LIMIT 50
        """)
        
        rows = cursor.fetchall()
        history = []
        
        for row in rows:
            # Obtener participantes
            cursor.execute("""
                SELECT player_uuid, player_name, kills, event_coins_earned
                FROM event_participants
                WHERE event_history_id = ?
                ORDER BY kills DESC
            """, (row[0],))
            
            participants = []
            participant_rows = cursor.fetchall()
            for p_row in participant_rows:
                participants.append({
                    'uuid': p_row[0],
                    'name': p_row[1],
                    'kills': p_row[2],
                    'eventCoinsEarned': p_row[3]
                })
            
            history.append({
                'historyId': row[0],
                'eventId': row[1],
                'eventName': row[2],
                'startedAt': row[3],
                'endedAt': row[4],
                'participants': row[5],
                'totalKills': row[6],
                'status': row[7],
                'world': row[8],
                'participantDetails': participants
            })
        
        conn.close()
        return jsonify({'success': True, 'history': history})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/stats', methods=['GET'])
@login_required
def get_events_stats():
    """Obtiene estadísticas globales de eventos"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'stats': {
                'totalEvents': 0,
                'completedEvents': 0,
                'totalKills': 0,
                'totalParticipants': 0,
                'topPlayers': []
            }})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Estadísticas generales
        cursor.execute("""
            SELECT COUNT(*), 
                   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
                   SUM(total_kills),
                   SUM(participants)
            FROM event_history
        """)
        
        stats_row = cursor.fetchone()
        total_events = stats_row[0] or 0
        completed_events = stats_row[1] or 0
        total_kills = stats_row[2] or 0
        total_participants = stats_row[3] or 0
        
        # Top jugadores por kills en eventos
        cursor.execute("""
            SELECT player_uuid, player_name, SUM(kills) as total_kills, SUM(event_coins_earned) as total_coins
            FROM event_participants
            GROUP BY player_uuid, player_name
            ORDER BY total_kills DESC
            LIMIT 10
        """)
        
        top_players = []
        for row in cursor.fetchall():
            top_players.append({
                'uuid': row[0],
                'name': row[1],
                'totalKills': row[2],
                'totalEventCoins': row[3]
            })
        
        conn.close()
        
        return jsonify({
            'success': True,
            'stats': {
                'totalEvents': total_events,
                'completedEvents': completed_events,
                'totalKills': total_kills,
                'totalParticipants': total_participants,
                'topPlayers': top_players
            }
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/start', methods=['POST'])
@login_required
def start_event_manual():
    """Inicia un evento manualmente"""
    try:
        data = request.json
        event_id = data.get('eventId')
        world = data.get('world', 'world')
        
        if not event_id:
            return jsonify({'success': False, 'message': 'ID de evento requerido'}), 400
        
        # TODO: Implementar llamada al plugin vía comando de consola
        # Por ahora retornamos éxito simulado
        command = f"rcon-cli \"event start {event_id} {world}\""
        # result = subprocess.run(command, shell=True, capture_output=True, text=True)
        
        return jsonify({
            'success': True,
            'message': f'Evento {event_id} iniciado en mundo {world} (implementar RCON)'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/stop', methods=['POST'])
@login_required
def stop_event():
    """Detiene un evento activo"""
    try:
        data = request.json
        event_id = data.get('eventId')
        
        if not event_id:
            return jsonify({'success': False, 'message': 'ID de evento requerido'}), 400
        
        # TODO: Implementar llamada al plugin vía comando de consola
        command = f"rcon-cli \"event stop {event_id}\""
        # result = subprocess.run(command, shell=True, capture_output=True, text=True)
        
        return jsonify({
            'success': True,
            'message': f'Evento {event_id} detenido (implementar RCON)'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/events/player-currency/<uuid>', methods=['GET'])
@login_required
def get_player_event_currency(uuid):
    """Obtiene las monedas de evento de un jugador"""
    try:
        # TODO: Implementar consulta al plugin/BD
        # Por ahora retornamos 0
        return jsonify({
            'success': True,
            'currency': 0,
            'playerUuid': uuid
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# =====================================================
# MAZMORRAS DINÁMICAS - API ENDPOINTS
# =====================================================

@app.route('/dungeons')
@login_required
def dungeons_panel():
    """Panel de gestión de mazmorras"""
    return render_template('dungeons_panel.html')

@app.route('/api/rpg/dungeons/config', methods=['GET'])
@login_required
def get_dungeons_config():
    """Obtiene la configuración de mazmorras disponibles"""
    try:
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'dungeons_config.json')
        if not os.path.exists(config_file):
            return jsonify({'success': False, 'message': 'Archivo de configuración no encontrado'}), 404
        
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        
        return jsonify({'success': True, 'config': config})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/config', methods=['PUT'])
@login_required
def update_dungeons_config():
    """Actualiza la configuración de mazmorras"""
    try:
        data = request.json
        config_file = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'dungeons_config.json')
        
        os.makedirs(os.path.dirname(config_file), exist_ok=True)
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        return jsonify({'success': True, 'message': 'Configuración actualizada'})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/start', methods=['POST'])
@login_required
def start_dungeon():
    """Inicia una nueva sesión de mazmorra"""
    try:
        data = request.json
        dungeon_id = data.get('dungeonId')
        world = data.get('world', 'world')
        
        if not dungeon_id:
            return jsonify({'success': False, 'message': 'ID de mazmorra requerido'}), 400
        
        # TODO: Implementar llamada al plugin vía comando de consola
        # Por ahora retornamos éxito simulado
        session_id = os.urandom(16).hex()
        
        return jsonify({
            'success': True,
            'message': f'Mazmorra {dungeon_id} iniciada en mundo {world}',
            'sessionId': session_id
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/active', methods=['GET'])
@login_required
def get_active_dungeons():
    """Obtiene mazmorras activas en tiempo real"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'dungeons': []})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT session_id, dungeon_id, dungeon_name, started_at, 
                   player_count, total_mobs_killed, world
            FROM dungeon_history
            WHERE status = 'ACTIVE'
            ORDER BY started_at DESC
        """)
        
        rows = cursor.fetchall()
        dungeons = []
        
        for row in rows:
            dungeons.append({
                'sessionId': row[0],
                'dungeonId': row[1],
                'dungeonName': row[2],
                'startedAt': row[3],
                'playerCount': row[4],
                'totalMobsKilled': row[5],
                'world': row[6]
            })
        
        conn.close()
        return jsonify({'success': True, 'dungeons': dungeons})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/history', methods=['GET'])
@login_required
def get_dungeons_history():
    """Obtiene historial de mazmorras"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'history': []})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT session_id, dungeon_id, dungeon_name, started_at, completed_at,
                   status, player_count, total_mobs_killed, duration_seconds, 
                   completion_rate, world
            FROM dungeon_history
            WHERE status != 'ACTIVE'
            ORDER BY started_at DESC
            LIMIT 50
        """)
        
        rows = cursor.fetchall()
        history = []
        
        for row in rows:
            # Obtener participantes
            cursor.execute("""
                SELECT player_uuid, player_name, kills, damage_dealt, 
                       rewards_xp, rewards_coins
                FROM dungeon_participants
                WHERE session_id = ?
                ORDER BY kills DESC
            """, (row[0],))
            
            participants = []
            participant_rows = cursor.fetchall()
            for p_row in participant_rows:
                participants.append({
                    'uuid': p_row[0],
                    'name': p_row[1],
                    'kills': p_row[2],
                    'damageDealt': p_row[3],
                    'rewardsXp': p_row[4],
                    'rewardsCoins': p_row[5]
                })
            
            history.append({
                'sessionId': row[0],
                'dungeonId': row[1],
                'dungeonName': row[2],
                'startedAt': row[3],
                'completedAt': row[4],
                'status': row[5],
                'playerCount': row[6],
                'totalMobsKilled': row[7],
                'durationSeconds': row[8],
                'completionRate': row[9],
                'world': row[10],
                'participants': participants
            })
        
        conn.close()
        return jsonify({'success': True, 'history': history})
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/stats', methods=['GET'])
@login_required
def get_dungeons_stats():
    """Obtiene estadísticas globales de mazmorras"""
    try:
        db_path = os.path.join(PLUGINS_DIR, 'MMORPGPlugin', 'rpgdata.db')
        if not os.path.exists(db_path):
            return jsonify({'success': True, 'stats': {
                'totalDungeons': 0,
                'completedDungeons': 0,
                'totalMobsKilled': 0,
                'avgCompletionTime': 0,
                'topPlayers': []
            }})
        
        import sqlite3
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # Estadísticas generales
        cursor.execute("""
            SELECT COUNT(*), 
                   SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
                   SUM(total_mobs_killed),
                   AVG(duration_seconds)
            FROM dungeon_history
        """)
        
        stats_row = cursor.fetchone()
        total_dungeons = stats_row[0] or 0
        completed_dungeons = stats_row[1] or 0
        total_mobs = stats_row[2] or 0
        avg_duration = stats_row[3] or 0
        
        # Top jugadores
        cursor.execute("""
            SELECT player_uuid, player_name, SUM(kills) as total_kills, 
                   SUM(rewards_xp) as total_xp, SUM(rewards_coins) as total_coins
            FROM dungeon_participants
            GROUP BY player_uuid, player_name
            ORDER BY total_kills DESC
            LIMIT 10
        """)
        
        top_players = []
        for row in cursor.fetchall():
            top_players.append({
                'uuid': row[0],
                'name': row[1],
                'totalKills': row[2],
                'totalXp': row[3],
                'totalCoins': row[4]
            })
        
        conn.close()
        
        return jsonify({
            'success': True,
            'stats': {
                'totalDungeons': total_dungeons,
                'completedDungeons': completed_dungeons,
                'totalMobsKilled': total_mobs,
                'avgCompletionTime': round(avg_duration / 60, 1),  # Convertir a minutos
                'topPlayers': top_players
            }
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/dungeons/exit', methods=['POST'])
@login_required
def exit_dungeon():
    """Abandona una sesión de mazmorra activa"""
    try:
        data = request.json
        session_id = data.get('sessionId')
        
        if not session_id:
            return jsonify({'success': False, 'message': 'ID de sesión requerido'}), 400
        
        # TODO: Implementar llamada al plugin
        return jsonify({
            'success': True,
            'message': f'Sesión {session_id} abandonada (implementar RCON)'
        })
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# ===== SQUADS ENDPOINTS =====

@app.route('/squads')
@login_required
def squads_panel():
    return render_template('squads_panel.html')

@app.route('/api/rpg/squads/config', methods=['GET'])
def get_squads_config():
    try:
        config_file = os.path.join(CONFIG_DIR, 'squad_config.json')
        if os.path.exists(config_file):
            with open(config_file, 'r') as f:
                config = json.load(f)
                return jsonify(config), 200
        return jsonify({'squad_system': {'enabled': False}}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/squads/config', methods=['PUT'])
def update_squads_config():
    try:
        config = request.json
        config_file = os.path.join(CONFIG_DIR, 'squad_config.json')
        with open(config_file, 'w') as f:
            json.dump(config, f, indent=2)
        return jsonify({'success': True, 'message': 'Configuración guardada'}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/squads/list', methods=['GET'])
def get_squads_list():
    try:
        db_file = os.path.join(PLUGINS_DIR, 'MMORPG', 'squads.db')
        if not os.path.exists(db_file):
            return jsonify({'squads': [], 'total': 0}), 200
        
        import sqlite3
        conn = sqlite3.connect(db_file)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('SELECT * FROM squads WHERE disbanded_at IS NULL ORDER BY created_at DESC LIMIT 50')
        rows = cursor.fetchall()
        
        squads = []
        for row in rows:
            cursor.execute('SELECT COUNT(*) as count FROM squad_members WHERE squad_id = ?', (row['id'],))
            member_count = cursor.fetchone()['count']
            
            cursor.execute('SELECT SUM(contributions_coins) as total_coins FROM squad_members WHERE squad_id = ?', (row['id'],))
            total_coins = cursor.fetchone()['total_coins'] or 0
            
            squads.append({
                'id': row['id'],
                'name': row['name'],
                'captain_uuid': row['captain_uuid'],
                'description': row['description'],
                'level': row['level'],
                'treasury_coins': row['treasury_coins'],
                'treasury_xp': row['treasury_xp'],
                'member_count': member_count,
                'total_contributions': total_coins,
                'created_at': row['created_at']
            })
        
        conn.close()
        return jsonify({'squads': squads, 'total': len(squads)}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/squads/<squad_id>/details', methods=['GET'])
def get_squad_details(squad_id):
    try:
        db_file = os.path.join(PLUGINS_DIR, 'MMORPG', 'squads.db')
        if not os.path.exists(db_file):
            return jsonify({'success': False, 'message': 'Base de datos no encontrada'}), 404
        
        import sqlite3
        conn = sqlite3.connect(db_file)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('SELECT * FROM squads WHERE id = ?', (squad_id,))
        squad = cursor.fetchone()
        
        if not squad:
            conn.close()
            return jsonify({'success': False, 'message': 'Escuadra no encontrada'}), 404
        
        cursor.execute('SELECT * FROM squad_members WHERE squad_id = ? ORDER BY joined_at', (squad_id,))
        members = [dict(row) for row in cursor.fetchall()]
        
        cursor.execute('SELECT * FROM squad_log WHERE squad_id = ? ORDER BY timestamp DESC LIMIT 20', (squad_id,))
        logs = [dict(row) for row in cursor.fetchall()]
        
        conn.close()
        
        return jsonify({
            'squad': dict(squad),
            'members': members,
            'logs': logs
        }), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/squads/history', methods=['GET'])
def get_squads_history():
    try:
        db_file = os.path.join(PLUGINS_DIR, 'MMORPG', 'squads.db')
        if not os.path.exists(db_file):
            return jsonify({'history': []}), 200
        
        import sqlite3
        conn = sqlite3.connect(db_file)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('SELECT * FROM squads WHERE disbanded_at IS NOT NULL ORDER BY disbanded_at DESC LIMIT 50')
        history = [dict(row) for row in cursor.fetchall()]
        
        conn.close()
        return jsonify({'history': history}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/squads/stats', methods=['GET'])
def get_squads_stats():
    try:
        db_file = os.path.join(PLUGINS_DIR, 'MMORPG', 'squads.db')
        if not os.path.exists(db_file):
            return jsonify({
                'stats': {
                    'total_squads': 0,
                    'active_squads': 0,
                    'total_members': 0,
                    'total_treasury': 0
                },
                'top_squads': []
            }), 200
        
        import sqlite3
        conn = sqlite3.connect(db_file)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('SELECT COUNT(*) as count FROM squads')
        total_squads = cursor.fetchone()['count']
        
        cursor.execute('SELECT COUNT(*) as count FROM squads WHERE disbanded_at IS NULL')
        active_squads = cursor.fetchone()['count']
        
        cursor.execute('SELECT COUNT(*) as count FROM squad_members')
        total_members = cursor.fetchone()['count']
        
        cursor.execute('SELECT SUM(treasury_coins) as total FROM squads WHERE disbanded_at IS NULL')
        total_treasury = cursor.fetchone()['total'] or 0
        
        cursor.execute('''
            SELECT name, level, treasury_coins, 
                   (SELECT COUNT(*) FROM squad_members WHERE squad_id = squads.id) as member_count
            FROM squads 
            WHERE disbanded_at IS NULL
            ORDER BY treasury_coins DESC 
            LIMIT 10
        ''')
        top_squads = [dict(row) for row in cursor.fetchall()]
        
        conn.close()
        
        return jsonify({
            'stats': {
                'total_squads': total_squads,
                'active_squads': active_squads,
                'total_members': total_members,
                'total_treasury': total_treasury
            },
            'top_squads': top_squads
        }), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

# =====================================================
# ENDPOINTS DE CRAFTEO (MÓDULO 3.1)
# =====================================================

@app.route('/crafter', methods=['GET'])
@login_required
def crafter_panel():
    """Panel web de crafteo"""
    return render_template('crafter_panel.html')

@app.route('/api/rpg/crafting/recipes', methods=['GET'])
def get_crafting_recipes():
    """Obtener lista de recetas disponibles"""
    try:
        rcon = get_rcon_connection()
        if not rcon:
            return jsonify({'success': False, 'message': 'RCON desconectado'}), 500
        
        # Cargar configuración de crafteo
        craft_config_path = os.path.join(CONFIG_DIR, 'crafting_config.json')
        if not os.path.exists(craft_config_path):
            return jsonify([]), 200
        
        with open(craft_config_path, 'r') as f:
            craft_config = json.load(f)
        
        recipes = []
        if 'recipes' in craft_config:
            for recipe in craft_config['recipes']:
                recipes.append({
                    'id': recipe['id'],
                    'name': recipe['name'],
                    'description': recipe['description'],
                    'tier': recipe['tier'],
                    'result_item': recipe['result_item'],
                    'result_amount': recipe.get('result_amount', 1),
                    'experience_reward': recipe['xp_reward'],
                    'coin_reward': recipe['coin_reward'],
                    'crafting_time_seconds': recipe['crafting_time_seconds'],
                    'required_station': recipe.get('required_station'),
                    'ingredients': recipe.get('ingredients', [])
                })
        
        return jsonify(recipes), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/crafting/recipe/<recipe_id>', methods=['GET'])
def get_crafting_recipe(recipe_id):
    """Obtener detalles de una receta específica"""
    try:
        craft_config_path = os.path.join(CONFIG_DIR, 'crafting_config.json')
        if not os.path.exists(craft_config_path):
            return jsonify({'success': False, 'message': 'Receta no encontrada'}), 404
        
        with open(craft_config_path, 'r') as f:
            craft_config = json.load(f)
        
        for recipe in craft_config.get('recipes', []):
            if recipe['id'] == recipe_id:
                return jsonify({
                    'id': recipe['id'],
                    'name': recipe['name'],
                    'description': recipe['description'],
                    'tier': recipe['tier'],
                    'result_item': recipe['result_item'],
                    'result_amount': recipe.get('result_amount', 1),
                    'experience_reward': recipe['xp_reward'],
                    'coin_reward': recipe['coin_reward'],
                    'crafting_time_seconds': recipe['crafting_time_seconds'],
                    'required_station': recipe.get('required_station'),
                    'ingredients': recipe.get('ingredients', [])
                }), 200
        
        return jsonify({'success': False, 'message': 'Receta no encontrada'}), 404
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/crafting/start', methods=['POST'])
def start_crafting():
    """Iniciar un nuevo crafteo"""
    try:
        data = request.get_json()
        recipe_id = data.get('recipe_id')
        
        if not recipe_id:
            return jsonify({'success': False, 'message': 'Recipe ID requerido'}), 400
        
        rcon = get_rcon_connection()
        if not rcon:
            return jsonify({'success': False, 'message': 'RCON desconectado'}), 500
        
        # Comando para iniciar crafteo en el plugin
        command = f"rpg crafting start {current_user.username} {recipe_id}"
        result = rcon.command(command)
        
        # Simular respuesta (en producción viendría del plugin)
        session_id = f"craft_{int(time() * 1000)}"
        
        return jsonify({
            'success': True,
            'message': 'Crafteo iniciado',
            'session_id': session_id
        }), 201
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/crafting/active', methods=['GET'])
def get_active_crafting_sessions():
    """Obtener crafteos activos del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Obtener crafteos en progreso
        cursor.execute('''
            SELECT id, player_uuid, recipe_id, started_at, completed_at, experience_earned, coins_earned
            FROM crafting_history
            WHERE player_uuid = ? AND completed_at IS NULL
            ORDER BY started_at DESC
            LIMIT 10
        ''', (str(current_user.id),))
        
        sessions = []
        craft_config_path = os.path.join(CONFIG_DIR, 'crafting_config.json')
        
        with open(craft_config_path, 'r') as f:
            craft_config = json.load(f)
        
        recipe_map = {r['id']: r for r in craft_config.get('recipes', [])}
        
        for row in cursor.fetchall():
            recipe = recipe_map.get(row['recipe_id'], {})
            elapsed = (datetime.now() - datetime.fromisoformat(row['started_at'])).total_seconds() * 1000
            total_time = recipe.get('crafting_time_seconds', 60)
            remaining = max(0, (total_time * 1000) - elapsed)
            is_complete = remaining <= 0
            
            sessions.append({
                'session_id': f"craft_{row['id']}",
                'recipe_name': recipe.get('name', 'Desconocido'),
                'recipe_id': row['recipe_id'],
                'elapsed_time': int(elapsed),
                'total_time': total_time,
                'remaining_time': int(remaining),
                'is_complete': is_complete
            })
        
        conn.close()
        return jsonify(sessions), 200
    except Exception as e:
        print(f"Error en get_active_crafting_sessions: {e}")
        return jsonify([]), 200

@app.route('/api/rpg/crafting/complete', methods=['POST'])
def complete_crafting():
    """Completar un crafteo y recoger el item"""
    try:
        data = request.get_json()
        session_id = data.get('session_id')
        
        if not session_id:
            return jsonify({'success': False, 'message': 'Session ID requerido'}), 400
        
        # Comando para completar crafteo
        rcon = get_rcon_connection()
        if rcon:
            command = f"rpg crafting complete {current_user.username} {session_id}"
            rcon.command(command)
        
        return jsonify({
            'success': True,
            'message': '¡Item crafteado y recogido!'
        }), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/crafting/stats', methods=['GET'])
def get_crafting_stats():
    """Obtener estadísticas de crafteo del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Cargar config para total de recetas
        craft_config_path = os.path.join(CONFIG_DIR, 'crafting_config.json')
        total_recipes = 0
        if os.path.exists(craft_config_path):
            with open(craft_config_path, 'r') as f:
                craft_config = json.load(f)
                total_recipes = len(craft_config.get('recipes', []))
        
        # Recetas desbloqueadas
        cursor.execute('''
            SELECT COUNT(*) as count FROM unlocked_recipes
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        unlocked = cursor.fetchone()['count'] or 0
        
        # Crafteos completados
        cursor.execute('''
            SELECT COUNT(*) as total, 
                   COALESCE(SUM(experience_earned), 0) as total_xp,
                   COALESCE(SUM(coins_earned), 0) as total_coins
            FROM crafting_history
            WHERE player_uuid = ? AND completed_at IS NOT NULL
        ''', (str(current_user.id),))
        
        stats = cursor.fetchone()
        conn.close()
        
        return jsonify({
            'recipes_unlocked': unlocked,
            'total_recipes': total_recipes,
            'completed_crafts': stats['total'],
            'total_experience': stats['total_xp'],
            'total_coins': stats['total_coins'],
            'active_sessions': 0
        }), 200
    except Exception as e:
        print(f"Error en get_crafting_stats: {e}")
        return jsonify({
            'recipes_unlocked': 0,
            'total_recipes': 0,
            'completed_crafts': 0,
            'total_experience': 0,
            'total_coins': 0,
            'active_sessions': 0
        }), 200

@app.route('/api/rpg/crafting/history', methods=['GET'])
def get_crafting_history():
    """Obtener historial de crafteos"""
    try:
        limit = request.args.get('limit', 20, type=int)
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT id, player_uuid, recipe_id, started_at, completed_at, 
                   experience_earned, coins_earned
            FROM crafting_history
            WHERE player_uuid = ?
            ORDER BY started_at DESC
            LIMIT ?
        ''', (str(current_user.id), limit))
        
        history = [dict(row) for row in cursor.fetchall()]
        conn.close()
        
        return jsonify(history), 200
    except Exception as e:
        print(f"Error en get_crafting_history: {e}")
        return jsonify([]), 200

# ============================================
# MÓDULO 3.2: ENCANTAMIENTOS PERSONALIZADOS
# ============================================

@app.route('/enchanting')
@login_required
def enchanting_panel():
    """Panel de encantamientos personalizados"""
    return render_template('enchanting_panel.html')

@app.route('/api/rpg/enchanting/list', methods=['GET'])
def list_enchantments():
    """Listar todos los encantamientos disponibles"""
    try:
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        
        if not os.path.exists(enchant_config_path):
            return jsonify({'success': False, 'message': 'Configuración no encontrada'}), 404
        
        with open(enchant_config_path, 'r') as f:
            config = json.load(f)
        
        enchantments = config.get('enchantments', [])
        return jsonify(enchantments), 200
    except Exception as e:
        print(f"Error en list_enchantments: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/enchanting/details/<enchant_id>', methods=['GET'])
def get_enchantment_details(enchant_id):
    """Obtener detalles de un encantamiento específico"""
    try:
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        
        if not os.path.exists(enchant_config_path):
            return jsonify({'success': False, 'message': 'Configuración no encontrada'}), 404
        
        with open(enchant_config_path, 'r') as f:
            config = json.load(f)
        
        enchantments = config.get('enchantments', [])
        enchantment = next((e for e in enchantments if e['id'] == enchant_id), None)
        
        if not enchantment:
            return jsonify({'success': False, 'message': 'Encantamiento no encontrado'}), 404
        
        return jsonify(enchantment), 200
    except Exception as e:
        print(f"Error en get_enchantment_details: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/enchanting/apply', methods=['POST'])
def apply_enchantment():
    """Aplicar un encantamiento a un item"""
    try:
        data = request.get_json()
        item_type = data.get('item_type')
        enchant_id = data.get('enchantment_id')
        level = data.get('level', 1)
        
        if not all([item_type, enchant_id]):
            return jsonify({'success': False, 'message': 'Faltan parámetros'}), 400
        
        # Cargar configuración
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        with open(enchant_config_path, 'r') as f:
            config = json.load(f)
        
        enchantments = config.get('enchantments', [])
        enchantment = next((e for e in enchantments if e['id'] == enchant_id), None)
        
        if not enchantment:
            return jsonify({'success': False, 'message': 'Encantamiento no encontrado'}), 400
        
        # Validar nivel
        if level < 1 or level > enchantment.get('max_level', 1):
            return jsonify({'success': False, 'message': 'Nivel inválido'}), 400
        
        # Validar compatibilidad con item
        applicable_items = enchantment.get('applicable_items', [])
        if item_type not in applicable_items:
            return jsonify({'success': False, 'message': 'Este encantamiento no se puede aplicar a este tipo de item'}), 400
        
        # Calcular costos
        base_cost = enchantment.get('cost_per_level', 100) * level
        xp_cost = enchantment.get('experience_cost', 10) * level
        
        # Calcular tasa de éxito
        enchanting_rules = config.get('enchanting_rules', {})
        base_success = enchanting_rules.get('base_success_rate', 70)
        tier_scaling = enchanting_rules.get('tier_scaling', {})
        tier = enchantment.get('tier', 'UNCOMMON')
        tier_modifier = tier_scaling.get(tier, 100) / 100.0
        success_rate = base_success * tier_modifier
        
        # Simular éxito/fallo
        import random
        success = random.randint(1, 100) <= success_rate
        
        # Guardar en base de datos
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        cursor = conn.cursor()
        
        # Crear tabla si no existe
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS enchanting_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                item_type TEXT NOT NULL,
                enchantment_id TEXT NOT NULL,
                level INTEGER NOT NULL,
                cost INTEGER NOT NULL,
                experience_cost INTEGER NOT NULL,
                success BOOLEAN NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        cursor.execute('''
            INSERT INTO enchanting_history 
            (player_uuid, item_type, enchantment_id, level, cost, experience_cost, success)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (str(current_user.id), item_type, enchant_id, level, base_cost, xp_cost, success))
        
        if success:
            # Crear tabla de items encantados si no existe
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS enchanted_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    item_type TEXT NOT NULL,
                    enchantment_id TEXT NOT NULL,
                    level INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            cursor.execute('''
                INSERT INTO enchanted_items 
                (player_uuid, item_type, enchantment_id, level)
                VALUES (?, ?, ?, ?)
            ''', (str(current_user.id), item_type, enchant_id, level))
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': success,
            'message': '¡Encantamiento aplicado con éxito!' if success else 'El encantamiento falló',
            'success_rate': success_rate,
            'cost': base_cost,
            'xp_cost': xp_cost
        }), 200
    except Exception as e:
        print(f"Error en apply_enchantment: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/enchanting/items', methods=['GET'])
def get_enchanted_items():
    """Obtener items encantados del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS enchanted_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                item_type TEXT NOT NULL,
                enchantment_id TEXT NOT NULL,
                level INTEGER NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        cursor.execute('''
            SELECT id, item_type, enchantment_id, level, created_at
            FROM enchanted_items
            WHERE player_uuid = ?
            ORDER BY created_at DESC
        ''', (str(current_user.id),))
        
        items = [dict(row) for row in cursor.fetchall()]
        conn.close()
        
        # Cargar detalles de encantamientos
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        if os.path.exists(enchant_config_path):
            with open(enchant_config_path, 'r') as f:
                config = json.load(f)
            enchantments = {e['id']: e for e in config.get('enchantments', [])}
            
            for item in items:
                enchant = enchantments.get(item['enchantment_id'], {})
                item['enchantment_name'] = enchant.get('name', item['enchantment_id'])
                item['enchantment_tier'] = enchant.get('tier', 'UNCOMMON')
        
        return jsonify(items), 200
    except Exception as e:
        print(f"Error en get_enchanted_items: {e}")
        return jsonify([]), 200

@app.route('/api/rpg/enchanting/stats', methods=['GET'])
def get_enchanting_stats():
    """Obtener estadísticas de encantamientos del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Items encantados
        cursor.execute('''
            SELECT COUNT(*) as count FROM enchanted_items
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        enchanted_items = cursor.fetchone()['count'] or 0
        
        # Encantamientos aplicados (total, incluyendo fallos)
        cursor.execute('''
            SELECT COUNT(*) as total,
                   COALESCE(SUM(experience_cost), 0) as total_xp,
                   COALESCE(SUM(cost), 0) as total_coins
            FROM enchanting_history
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        
        stats = cursor.fetchone()
        conn.close()
        
        return jsonify({
            'enchanted_items': enchanted_items,
            'enchantments_applied': stats['total'],
            'total_experience': stats['total_xp'],
            'total_coins': stats['total_coins']
        }), 200
    except Exception as e:
        print(f"Error en get_enchanting_stats: {e}")
        return jsonify({
            'enchanted_items': 0,
            'enchantments_applied': 0,
            'total_experience': 0,
            'total_coins': 0
        }), 200

@app.route('/api/rpg/enchanting/history', methods=['GET'])
def get_enchanting_history():
    """Obtener historial de encantamientos"""
    try:
        limit = request.args.get('limit', 10, type=int)
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS enchanting_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                item_type TEXT NOT NULL,
                enchantment_id TEXT NOT NULL,
                level INTEGER NOT NULL,
                cost INTEGER NOT NULL,
                experience_cost INTEGER NOT NULL,
                success BOOLEAN NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        cursor.execute('''
            SELECT id, item_type, enchantment_id, level, cost, experience_cost, success, timestamp
            FROM enchanting_history
            WHERE player_uuid = ?
            ORDER BY timestamp DESC
            LIMIT ?
        ''', (str(current_user.id), limit))
        
        history = [dict(row) for row in cursor.fetchall()]
        conn.close()
        
        # Cargar detalles de encantamientos
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        if os.path.exists(enchant_config_path):
            with open(enchant_config_path, 'r') as f:
                config = json.load(f)
            enchantments = {e['id']: e for e in config.get('enchantments', [])}
            
            for entry in history:
                enchant = enchantments.get(entry['enchantment_id'], {})
                entry['enchantment_name'] = enchant.get('name', entry['enchantment_id'])
        
        return jsonify(history), 200
    except Exception as e:
        print(f"Error en get_enchanting_history: {e}")
        return jsonify([]), 200

@app.route('/api/rpg/enchanting/config', methods=['GET'])
def get_enchanting_config():
    """Obtener configuración completa de encantamientos"""
    try:
        enchant_config_path = os.path.join(CONFIG_DIR, 'enchantments_config.json')
        
        if not os.path.exists(enchant_config_path):
            return jsonify({'success': False, 'message': 'Configuración no encontrada'}), 404
        
        with open(enchant_config_path, 'r') as f:
            config = json.load(f)
        
        return jsonify(config), 200
    except Exception as e:
        print(f"Error en get_enchanting_config: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

# ============================================
# MÓDULO 3.3: MASCOTAS Y MONTURAS
# ============================================

@app.route('/pets')
@login_required
def pets_panel():
    """Panel de mascotas y monturas"""
    return render_template('pets_panel.html')

@app.route('/api/rpg/pets/list', methods=['GET'])
@login_required
def list_pets():
    """Listar todas las mascotas disponibles"""
    try:
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        
        if not os.path.exists(pets_config_path):
            return jsonify({'success': False, 'message': 'Configuración no encontrada'}), 404
        
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        
        pets = config.get('pets', [])
        return jsonify(pets), 200
    except Exception as e:
        print(f"Error en list_pets: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/my-pets', methods=['GET'])
@login_required
def get_my_pets():
    """Obtener mascotas del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Crear tabla si no existe
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_pets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                pet_id TEXT NOT NULL,
                custom_name TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                current_health REAL NOT NULL,
                hunger_level REAL DEFAULT 100.0,
                last_fed_timestamp INTEGER,
                is_active BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        cursor.execute('''
            SELECT * FROM player_pets
            WHERE player_uuid = ?
            ORDER BY created_at DESC
        ''', (str(current_user.id),))
        
        my_pets = [dict(row) for row in cursor.fetchall()]
        conn.close()
        
        # Cargar detalles de mascotas desde config
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        if os.path.exists(pets_config_path):
            with open(pets_config_path, 'r') as f:
                config = json.load(f)
            pets_data = {p['id']: p for p in config.get('pets', [])}
            
            for pet in my_pets:
                pet_config = pets_data.get(pet['pet_id'], {})
                pet['name'] = pet_config.get('name', pet['pet_id'])
                pet['type'] = pet_config.get('type', 'COMBAT')
                pet['rarity'] = pet_config.get('rarity', 'COMMON')
                pet['base_stats'] = pet_config.get('base_stats', {})
                pet['evolution_levels'] = pet_config.get('evolution_levels', [])
        
        return jsonify(my_pets), 200
    except Exception as e:
        print(f"Error en get_my_pets: {e}")
        return jsonify([]), 200

@app.route('/api/rpg/pets/adopt', methods=['POST'])
@login_required
def adopt_pet():
    """Adoptar una mascota"""
    try:
        data = request.get_json()
        pet_id = data.get('pet_id')
        
        if not pet_id:
            return jsonify({'success': False, 'message': 'Falta el ID de mascota'}), 400
        
        # Cargar configuración
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        
        pets = config.get('pets', [])
        pet_config = next((p for p in pets if p['id'] == pet_id), None)
        
        if not pet_config:
            return jsonify({'success': False, 'message': 'Mascota no encontrada'}), 400
        
        adoption_cost = pet_config.get('adoption_cost', 0)
        
        # Verificar límite de mascotas
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_pets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                pet_id TEXT NOT NULL,
                custom_name TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                current_health REAL NOT NULL,
                hunger_level REAL DEFAULT 100.0,
                last_fed_timestamp INTEGER,
                is_active BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            SELECT COUNT(*) as count FROM player_pets WHERE player_uuid = ?
        ''', (str(current_user.id),))
        
        current_pets = cursor.fetchone()['count']
        max_pets = config.get('pet_settings', {}).get('max_pets_per_player', 10)
        
        if current_pets >= max_pets:
            conn.close()
            return jsonify({'success': False, 'message': f'Has alcanzado el límite de {max_pets} mascotas'}), 400
        
        # Insertar mascota
        base_health = pet_config.get('base_stats', {}).get('health', 20)
        
        cursor.execute('''
            INSERT INTO player_pets 
            (player_uuid, pet_id, level, experience, current_health, hunger_level, last_fed_timestamp, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ''', (str(current_user.id), pet_id, 1, 0, base_health, 100.0, int(time()), 0))
        
        # Guardar en historial
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS pet_activity_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                action TEXT NOT NULL,
                target TEXT,
                cost INTEGER DEFAULT 0,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        cursor.execute('''
            INSERT INTO pet_activity_history (player_uuid, action, target, cost)
            VALUES (?, ?, ?, ?)
        ''', (str(current_user.id), 'ADOPT', pet_config.get('name', pet_id), adoption_cost))
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': f'¡Has adoptado a {pet_config.get("name", pet_id)}!',
            'cost': adoption_cost
        }), 200
    except Exception as e:
        print(f"Error en adopt_pet: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/feed', methods=['POST'])
@login_required
def feed_pet():
    """Alimentar una mascota"""
    try:
        data = request.get_json()
        pet_id = data.get('pet_id')
        
        if not pet_id:
            return jsonify({'success': False, 'message': 'Falta el ID de mascota'}), 400
        
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT * FROM player_pets
            WHERE id = ? AND player_uuid = ?
        ''', (pet_id, str(current_user.id)))
        pet = cursor.fetchone()
        
        if not pet:
            conn.close()
            return jsonify({'success': False, 'message': 'Mascota no encontrada'}), 404
        
        # Cargar configuración
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        pets_data = {p['id']: p for p in config.get('pets', [])}
        pet_config = pets_data.get(pet['pet_id'])
        
        if not pet_config:
            conn.close()
            return jsonify({'success': False, 'message': 'Configuración de mascota no encontrada'}), 404
        
        base_health = pet_config.get('base_stats', {}).get('health', 20.0)
        evolution_levels = pet_config.get('evolution_levels', [])
        current_level_index = max(0, min(len(evolution_levels) - 1, pet['level'] - 1))
        level_info = evolution_levels[current_level_index] if evolution_levels else {'stats_multiplier': 1.0}
        stats_multiplier = level_info.get('stats_multiplier', 1.0)
        max_health = base_health * stats_multiplier
        
        health_restore = pet_config.get('feed_restore_health', 10.0)
        hunger_restore = pet_config.get('feed_restore_hunger', 20.0)
        new_health = min(max_health, pet['current_health'] + health_restore)
        new_hunger = min(100.0, pet['hunger_level'] + hunger_restore)
        
        cursor.execute('''
            UPDATE player_pets
            SET hunger_level = ?,
                current_health = ?,
                last_fed_timestamp = ?
            WHERE id = ? AND player_uuid = ?
        ''', (new_hunger, new_health, int(time()), pet_id, str(current_user.id)))
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': 'Mascota alimentada correctamente',
            'health': new_health,
            'hunger': new_hunger
        }), 200
    except Exception as e:
        print(f"Error en feed_pet: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/evolve', methods=['POST'])
@login_required
def evolve_pet():
    """Evolucionar una mascota"""
    try:
        data = request.get_json()
        pet_id = data.get('pet_id')
        
        if not pet_id:
            return jsonify({'success': False, 'message': 'Falta el ID de mascota'}), 400
        
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Obtener mascota
        cursor.execute('''
            SELECT * FROM player_pets
            WHERE id = ? AND player_uuid = ?
        ''', (pet_id, str(current_user.id)))
        
        pet = cursor.fetchone()
        
        if not pet:
            conn.close()
            return jsonify({'success': False, 'message': 'Mascota no encontrada'}), 404
        
        # Cargar configuración
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        
        pets = config.get('pets', [])
        pet_config = next((p for p in pets if p['id'] == pet['pet_id']), None)
        
        if not pet_config:
            conn.close()
            return jsonify({'success': False, 'message': 'Configuración de mascota no encontrada'}), 404
        
        evolution_levels = pet_config.get('evolution_levels', [])
        current_level = pet['level']
        
        if current_level >= len(evolution_levels):
            conn.close()
            return jsonify({'success': False, 'message': 'Mascota ya está en nivel máximo'}), 400
        
        next_evolution = evolution_levels[current_level]
        required_xp = next_evolution.get('required_xp', 0)
        
        if pet['experience'] < required_xp:
            conn.close()
            return jsonify({'success': False, 'message': f'Se necesitan {required_xp} XP (tienes {pet["experience"]} XP)'}), 400
        
        # Evolucionar
        cursor.execute('''
            UPDATE player_pets
            SET level = level + 1,
                experience = 0
            WHERE id = ? AND player_uuid = ?
        ''', (pet_id, str(current_user.id)))
        
        # Guardar en historial
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS pet_activity_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                action TEXT NOT NULL,
                target TEXT,
                cost INTEGER DEFAULT 0,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            INSERT INTO pet_activity_history (player_uuid, action, target, cost)
            VALUES (?, ?, ?, ?)
        ''', (str(current_user.id), 'EVOLVE', pet_config.get('name', pet['pet_id']), 0))
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': f'¡{pet_config.get("name", pet["pet_id"])} ha evolucionado a nivel {current_level + 1}!'
        }), 200
    except Exception as e:
        print(f"Error en evolve_pet: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/equip', methods=['POST'])
@login_required
def equip_pet():
    """Equipar una mascota como activa"""
    try:
        data = request.get_json()
        pet_id = data.get('pet_id')
        
        if not pet_id:
            return jsonify({'success': False, 'message': 'Falta el ID de mascota'}), 400
        
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        cursor = conn.cursor()
        
        # Desactivar todas las mascotas
        cursor.execute('''
            UPDATE player_pets
            SET is_active = 0
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        
        # Activar la mascota seleccionada
        cursor.execute('''
            UPDATE player_pets
            SET is_active = 1
            WHERE id = ? AND player_uuid = ?
        ''', (pet_id, str(current_user.id)))
        
        if cursor.rowcount == 0:
            conn.close()
            return jsonify({'success': False, 'message': 'Mascota no encontrada'}), 404
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': 'Mascota equipada correctamente'
        }), 200
    except Exception as e:
        print(f"Error en equip_pet: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/mounts', methods=['GET'])
@login_required
def get_mounts():
    """Obtener todas las monturas y las desbloqueadas por el jugador"""
    try:
        # Cargar todas las monturas
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        
        if not os.path.exists(pets_config_path):
            return jsonify({'success': False, 'message': 'Configuración no encontrada'}), 404
        
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        
        all_mounts = config.get('mounts', [])
        
        # Cargar monturas desbloqueadas del jugador
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_mounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                mount_id TEXT NOT NULL,
                unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(player_uuid, mount_id)
            )
        ''')
        
        cursor.execute('''
            SELECT mount_id FROM player_mounts
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        
        unlocked = [row['mount_id'] for row in cursor.fetchall()]
        conn.close()
        
        # Marcar monturas desbloqueadas
        for mount in all_mounts:
            mount['unlocked'] = mount['id'] in unlocked
        
        return jsonify(all_mounts), 200
    except Exception as e:
        print(f"Error en get_mounts: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/unlock-mount', methods=['POST'])
@login_required
def unlock_mount():
    """Desbloquear una montura"""
    try:
        data = request.get_json()
        mount_id = data.get('mount_id')
        
        if not mount_id:
            return jsonify({'success': False, 'message': 'Falta el ID de montura'}), 400
        
        # Cargar configuración
        pets_config_path = os.path.join(CONFIG_DIR, 'pets_config.json')
        with open(pets_config_path, 'r') as f:
            config = json.load(f)
        
        mounts = config.get('mounts', [])
        mount_config = next((m for m in mounts if m['id'] == mount_id), None)
        
        if not mount_config:
            return jsonify({'success': False, 'message': 'Montura no encontrada'}), 400
        
        unlock_cost = mount_config.get('unlock_cost', 0)
        unlock_level = mount_config.get('unlock_level', 1)
        
        # TODO: Verificar nivel del jugador
        # Por ahora solo insertamos la montura
        
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_mounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                mount_id TEXT NOT NULL,
                unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(player_uuid, mount_id)
            )
        ''')
        
        try:
            cursor.execute('''
                INSERT INTO player_mounts (player_uuid, mount_id)
                VALUES (?, ?)
            ''', (str(current_user.id), mount_id))
            
            # Guardar en historial
            cursor.execute('''
                INSERT INTO pet_activity_history (player_uuid, action, target, cost)
                VALUES (?, ?, ?, ?)
            ''', (str(current_user.id), 'UNLOCK_MOUNT', mount_config.get('name', mount_id), unlock_cost))
            
            conn.commit()
            
            return jsonify({
                'success': True,
                'message': f'¡Has desbloqueado {mount_config.get("name", mount_id)}!',
                'cost': unlock_cost
            }), 200
        except sqlite3.IntegrityError:
            return jsonify({'success': False, 'message': 'Ya tienes esta montura desbloqueada'}), 400
        finally:
            conn.close()
    except Exception as e:
        print(f"Error en unlock_mount: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/pets/stats', methods=['GET'])
@login_required
def get_pet_stats():
    """Obtener estadísticas de mascotas del jugador"""
    try:
        conn = sqlite3.connect(MINECRAFT_DIR + '/minecraft_rpg.db')
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_pets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                pet_id TEXT NOT NULL,
                custom_name TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                current_health REAL NOT NULL,
                hunger_level REAL DEFAULT 100.0,
                last_fed_timestamp INTEGER,
                is_active BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS player_mounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                mount_id TEXT NOT NULL,
                unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(player_uuid, mount_id)
            )
        ''')
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS pet_activity_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                action TEXT NOT NULL,
                target TEXT,
                cost INTEGER DEFAULT 0,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # Total de mascotas
        cursor.execute('''
            SELECT COUNT(*) as count FROM player_pets
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        total_pets = cursor.fetchone()['count'] or 0
        
        # Total de monturas
        cursor.execute('''
            SELECT COUNT(*) as count FROM player_mounts
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        total_mounts = cursor.fetchone()['count'] or 0
        
        # Evoluciones totales
        cursor.execute('''
            SELECT COUNT(*) as count FROM pet_activity_history
            WHERE player_uuid = ? AND action = 'EVOLVE'
        ''', (str(current_user.id),))
        total_evolutions = cursor.fetchone()['count'] or 0
        
        # Monedas gastadas
        cursor.execute('''
            SELECT COALESCE(SUM(cost), 0) as total FROM pet_activity_history
            WHERE player_uuid = ?
        ''', (str(current_user.id),))
        total_coins_spent = cursor.fetchone()['total'] or 0
        
        # Historial de actividad
        cursor.execute('''
            SELECT action, target, cost, timestamp
            FROM pet_activity_history
            WHERE player_uuid = ?
            ORDER BY timestamp DESC
            LIMIT 10
        ''', (str(current_user.id),))
        
        activity_history = [dict(row) for row in cursor.fetchall()]
        conn.close()
        
        return jsonify({
            'total_pets': total_pets,
            'total_mounts': total_mounts,
            'total_evolutions': total_evolutions,
            'total_coins_spent': total_coins_spent,
            'activity_history': activity_history
        }), 200
    except Exception as e:
        print(f"Error en get_pet_stats: {e}")
        return jsonify({
            'total_pets': 0,
            'total_mounts': 0,
            'total_evolutions': 0,
            'total_coins_spent': 0,
            'activity_history': []
        }), 200

# ============================================
# MÓDULO: EDITOR DE LAYOUT RPG
# ============================================

@app.route('/rpg-layout')
@login_required
def rpg_layout_editor():
    """Panel de edición de coordenadas del mundo RPG"""
    return render_template('rpg_layout.html')

@app.route('/api/rpg/layout', methods=['GET'])
@login_required
def get_rpg_layout():
    """Obtener configuración del layout del mundo RPG"""
    try:
        layout_file = os.path.join(CONFIG_DIR, 'rpg_world_layout.json')
        
        if not os.path.exists(layout_file):
            return jsonify({'success': False, 'message': 'Layout no encontrado'}), 404
        
        with open(layout_file, 'r') as f:
            layout = json.load(f)
        
        return jsonify({'success': True, 'layout': layout}), 200
    except Exception as e:
        print(f"Error en get_rpg_layout: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/layout', methods=['POST'])
@login_required
def save_rpg_layout():
    """Guardar configuración del layout del mundo RPG"""
    try:
        data = request.get_json()
        layout_file = os.path.join(CONFIG_DIR, 'rpg_world_layout.json')
        
        with open(layout_file, 'w') as f:
            json.dump(data, f, indent=2)
        
        return jsonify({'success': True, 'message': 'Layout guardado correctamente'}), 200
    except Exception as e:
        print(f"Error en save_rpg_layout: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/rpg/layout/generate-script', methods=['GET'])
@login_required
def generate_spawn_script():
    """Generar script de spawn automático para cargar el mundo RPG"""
    try:
        layout_file = os.path.join(CONFIG_DIR, 'rpg_world_layout.json')
        
        if not os.path.exists(layout_file):
            return jsonify({'success': False, 'message': 'Layout no encontrado'}), 404
        
        with open(layout_file, 'r') as f:
            layout = json.load(f)
        
        # Generar script bash
        script = "#!/bin/bash\n"
        script += "# Script de carga automática del mundo RPG\n"
        script += "# Generado automáticamente desde el panel web\n\n"
        script += f"WORLD=\"{layout.get('world', 'mmorpg')}\"\n"
        script += "RCON_HOST=\"localhost\"\n"
        script += "RCON_PORT=\"25575\"\n"
        script += "RCON_PASSWORD=\"tu_password\"\n\n"
        script += "echo '=== Iniciando carga de mundo RPG ==='\n\n"
        
        # Spawn NPCs
        script += "# Spawning NPCs\n"
        script += "echo 'Spawneando NPCs...'\n"
        for npc in layout.get('npcs', []):
            loc = npc['location']
            script += f"rcon-cli -H $RCON_HOST -p $RCON_PORT -P $RCON_PASSWORD "
            script += f"\"rpg npc spawn {npc['id']} $WORLD {loc['x']} {loc['y']} {loc['z']}\"\n"
        
        script += "\n# Spawning Mobs\n"
        script += "echo 'Spawneando mobs...'\n"
        for mob in layout.get('mob_spawn_points', []):
            loc = mob['location']
            script += f"rcon-cli -H $RCON_HOST -p $RCON_PORT -P $RCON_PASSWORD "
            script += f"\"rpg mob spawn {mob['mob_id']} $WORLD {loc['x']} {loc['y']} {loc['z']}\"\n"
        
        script += "\n# Configurando zonas de respawn\n"
        script += "echo 'Configurando zonas de respawn...'\n"
        for zone_id, zone in layout.get('zones', {}).items():
            c1, c2 = zone['corners']
            y_range = zone['y_range']
            script += f"rcon-cli -H $RCON_HOST -p $RCON_PORT -P $RCON_PASSWORD "
            script += f"\"rpg zone create {zone_id} {zone['type']} $WORLD "
            script += f"{c1['x']} {y_range['min']} {c1['z']} {c2['x']} {y_range['max']} {c2['z']}\"\n"
        
        script += "\necho '=== Carga completada ==='\n"
        
        return jsonify({'success': True, 'script': script}), 200
    except Exception as e:
        print(f"Error en generate_spawn_script: {e}")
        return jsonify({'success': False, 'message': str(e)}), 500


# ============================================================================
# RESOURCE PACK ENDPOINTS
# ============================================================================

@app.route('/api/resource-pack/config', methods=['GET'])
@login_required
def get_resource_pack_config():
    """Obtiene la configuración actual de resource pack"""
    try:
        config = resource_pack_manager.get_current_config()
        return jsonify({
            'success': True,
            'config': config
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/api/resource-pack/config', methods=['POST'])
@login_required
def update_resource_pack_config():
    """Actualiza la configuración de resource pack"""
    try:
        data = request.get_json()
        
        result = resource_pack_manager.update_config(
            resource_pack_url=data.get('resource_pack_url'),
            sha1_hash=data.get('sha1_hash'),
            require=data.get('require'),
            prompt=data.get('prompt')
        )
        
        if result['success']:
            return jsonify(result)
        else:
            return jsonify(result), 400
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/api/resource-pack/upload', methods=['POST'])
@login_required
def upload_resource_pack():
    """Sube un resource pack al servidor"""
    try:
        if 'file' not in request.files:
            return jsonify({
                'success': False,
                'error': 'No se envió ningún archivo'
            }), 400
        
        file = request.files['file']
        
        if file.filename == '':
            return jsonify({
                'success': False,
                'error': 'Nombre de archivo vacío'
            }), 400
        
        if not file.filename.endswith('.zip'):
            return jsonify({
                'success': False,
                'error': 'El archivo debe ser un .zip'
            }), 400
        
        # Leer archivo
        file_data = file.read()
        filename = secure_filename(file.filename)
        
        # Guardar y calcular SHA-1
        result = resource_pack_manager.save_resource_pack(file_data, filename)
        
        if result['success']:
            return jsonify(result)
        else:
            return jsonify(result), 400
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/api/resource-pack/local', methods=['GET'])
@login_required
def list_local_resource_packs():
    """Lista los resource packs almacenados localmente"""
    try:
        packs = resource_pack_manager.list_local_packs()
        return jsonify({
            'success': True,
            'packs': packs
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@app.route('/api/resource-pack/local/<filename>', methods=['DELETE'])
@login_required
def delete_local_resource_pack(filename):
    """Elimina un resource pack local"""
    try:
        result = resource_pack_manager.delete_pack(filename)
        
        if result['success']:
            return jsonify(result)
        else:
            return jsonify(result), 400
    
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)

