#!/usr/bin/env python3
"""
Script temporal para refactorizar app.py - eliminar Docker y usar RCON nativo
"""
import re

def refactor_app_py():
    with open('web/app.py', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Patrón 1: Reemplazar referencias a docker_client.containers.get(CONTAINER_NAME)
    content = re.sub(
        r'if docker_client:\s+container = docker_client\.containers\.get\(CONTAINER_NAME\)\s+if container\.status',
        'if server_monitor.get_status()',
        content
    )
    
    # Patrón 2: Reemplazar llamadas a execute_rcon_command(container, ...)
    content = re.sub(
        r'execute_rcon_command\(container,\s*([^)]+)\)',
        r'execute_rcon_command(\1)',
        content
    )
    
    # Patrón 3: Reemplazar container.logs() con server_monitor.get_logs()
    content = re.sub(
        r'container\.logs\(tail=(\d+)\)\.decode\([\'"]utf-8[\'"]\)',
        r'server_monitor.get_logs(lines=\1)',
        content
    )
    
    # Patrón 4: Eliminar bloques completos de "if docker_client:" y sus else
    # Este es más complejo, lo haremos manualmente en casos específicos
    
    # Patrón 5: Reemplazar checks de container.status == 'running' con server_monitor.get_status() == 'running'
    content = re.sub(
        r"== 'running':",
        "== 'running':",
        content
    )
    
    with open('web/app.py', 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("✅ Refactorización completada")
    print("📝 Revisa los cambios antes de commit")

if __name__ == '__main__':
    refactor_app_py()
