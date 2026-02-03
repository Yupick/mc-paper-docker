#!/usr/bin/env python3
"""
Refactorización completa de app.py - Eliminar Docker, usar servicios nativos
"""
import re

def refactor_app_native():
    with open('web/app.py', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Remover import docker
    content = re.sub(r'\nimport docker\n', '\n', content)
    
    # 2. Agregar import de servicios nativos después de BackupService
    content = content.replace(
        'from services.backup_service import BackupService',
        'from services.backup_service import BackupService\nfrom services.rcon_native import RCONService, ServerMonitor'
    )
    
    # 3. Remover variable CONTAINER_NAME
    content = re.sub(r"CONTAINER_NAME = os\.getenv\('DOCKER_CONTAINER_NAME', 'minecraft-paper'\)\n", '', content)
    
    # 4. Reemplazar bloque de inicialización Docker con servicios nativos
    docker_client_pattern = r"# Cliente Docker\n.*?print\(f\"  Ejecuta: sudo usermod -aG docker \$USER && newgrp docker\"\)\n"
    native_services = """# Servicios nativos de RCON y monitoreo del servidor
rcon_service = RCONService(
    host=os.getenv('RCON_HOST', 'localhost'),
    port=int(os.getenv('RCON_PORT', '25575')),
    password=os.getenv('RCON_PASSWORD', 'minecraft123')
)
server_monitor = ServerMonitor(BASE_DIR)
"""
    content = re.sub(docker_client_pattern, native_services, content, flags=re.DOTALL)
    
    # 5. Refactorizar execute_rcon_command
    old_exec = r"def execute_rcon_command\(container, command, use_cache=False, cache_ttl=5\):.*?return exec_result"
    new_exec = """def execute_rcon_command(command, use_cache=False, cache_ttl=5):
    \"\"\"Ejecuta un comando RCON directamente al servidor nativo\"\"\"
    if use_cache:
        cache_key = f"rcon:{command}"
        cached = rcon_cache.get(cache_key, cache_ttl)
        if cached is not None:
            return cached
    
    exec_result = rcon_service.execute_command(command)
    
    class RCONResult:
        def __init__(self, output, exit_code):
            self.output = output
            self.exit_code = exit_code
    
    result = RCONResult(exec_result['output'], exec_result['exit_code'])
    
    if use_cache:
        rcon_cache.set(cache_key, result, cache_ttl)
    
    return result"""
    content = re.sub(old_exec, new_exec, content, flags=re.DOTALL)
    
    # 6. Reemplazar llamadas a execute_rcon_command(container, ...)
    content = re.sub(r'execute_rcon_command\(container, ', 'execute_rcon_command(', content)
    
    # 7. Reemplazar patron común: if docker_client: ... if container.status == 'running':
    # Patrón más específico para mantener la estructura
    pattern1 = r"if docker_client:\s+container = docker_client\.containers\.get\(CONTAINER_NAME\)\s+if container\.status == 'running':"
    replacement1 = "if server_monitor.get_status() == 'running':"
    content = re.sub(pattern1, replacement1, content)
    
    # 8. Reemplazar container.logs()
    content = re.sub(r"container\.logs\(tail=(\d+)\)\.decode\('utf-8'\)", r'server_monitor.get_logs(lines=\1)', content)
    
    # 9. Reemplazar bloques en server_status
    status_old = r"@app\.route\('/api/server/status'\).*?except Exception as e:\s+return jsonify\(\{'error': str\(e\)\}\), 500"
    status_new = """@app.route('/api/server/status')
@login_required
def server_status():
    try:
        status = server_monitor.get_status()
        stats = server_monitor.get_stats()
        
        if status == 'running' and stats:
            return jsonify({
                'status': status,
                'running': True,
                'cpu_percent': round(stats['cpu_percent'], 2),
                'memory_usage_mb': round(stats['memory_mb'], 2),
                'memory_limit_mb': 1024,
                'memory_percent': round((stats['memory_mb'] / 1024) * 100, 2),
                'threads': stats['threads']
            })
        else:
            return jsonify({
                'status': status,
                'running': False,
                'cpu_percent': 0,
                'memory_usage_mb': 0,
                'memory_limit_mb': 1024,
                'memory_percent': 0
            })
    except Exception as e:
        return jsonify({'error': str(e)}), 500"""
    content = re.sub(status_old, status_new, content, flags=re.DOTALL)
    
    # 10. Reemplazar server_logs
    logs_old = r"# API: Logs del servidor\n@app\.route\('/api/server/logs'\).*?except Exception as e:\s+return jsonify\(\{'error': str\(e\)\}\), 500"
    logs_new = """# API: Logs del servidor
@app.route('/api/server/logs')
@login_required
def server_logs():
    try:
        logs = server_monitor.get_logs(lines=100)
        return jsonify({'logs': logs})
    except Exception as e:
        return jsonify({'error': str(e)}), 500"""
    content = re.sub(logs_old, logs_new, content, flags=re.DOTALL)
    
    # 11. Reemplazar comandos de control (restart/stop/start)
    for cmd in ['restart', 'stop', 'start']:
        old_cmd = rf"@app\.route\('/api/server/{cmd}', methods=\['POST'\]\).*?subprocess\.run\(\s+\['sudo', 'docker-compose', '{cmd}'\],\s+cwd=MINECRAFT_DIR,"
        new_cmd = f"""@app.route('/api/server/{cmd}', methods=['POST'])
@login_required
def {cmd}_server():
    try:
        control_script = os.path.join(BASE_DIR, 'server-control.sh')
        result = subprocess.run(
            [control_script, '{cmd}', 'server'],"""
        content = re.sub(old_cmd, new_cmd, content, flags=re.DOTALL)
    
    # 12. Limpiar líneas con "Docker no disponible" que quedan huérfanas
    content = re.sub(r"\s+else:\s+return jsonify\(\{'error': 'Docker no disponible'\}\), 500\n", '', content)
    content = re.sub(r"return jsonify\(\{'error': 'Docker no disponible'\}\), 500\n", '', content)
    
    with open('web/app.py', 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("✅ Refactorización de app.py completada")
    print("📝 Verifica la sintaxis con: python3 -m py_compile web/app.py")

if __name__ == '__main__':
    refactor_app_native()
