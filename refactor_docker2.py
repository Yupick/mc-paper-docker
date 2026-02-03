#!/usr/bin/env python3
"""
Refactorizar app.py - Eliminar todos los bloques if docker_client
"""
import re

def refactor_docker_blocks():
    with open('web/app.py', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    i = 0
    skip_until_indent = None
    
    while i < len(lines):
        line = lines[i]
        
        # Detectar "if docker_client:"
        if 'if docker_client:' in line:
            indent = len(line) - len(line.lstrip())
            # Saltar el if docker_client y la siguiente línea (container = ...)
            i += 1
            if i < len(lines) and 'container =' in lines[i]:
                i += 1
            
            # Cambiar la siguiente línea "if container.status == 'running':" por "if server_monitor.get_status() == 'running':"
            if i < len(lines) and "container.status == 'running'" in lines[i]:
                check_line = lines[i]
                check_indent = len(check_line) - len(check_line.lstrip())
                new_check_line = ' ' * (check_indent - 4) + check_line.lstrip().replace("container.status == 'running'", "server_monitor.get_status() == 'running'")
                new_lines.append(new_check_line)
                i += 1
                continue
            
            continue
        
        # Reemplazar referencias a container.logs()
        if 'container.logs(' in line:
            line = re.sub(r'container\.logs\(tail=(\d+)\)\.decode\([\'"]utf-8[\'"]\)', r'server_monitor.get_logs(lines=\1)', line)
        
        # Eliminar else: return jsonify({'error': 'Docker no disponible'})
        if "'Docker no disponible'" in line or '"Docker no disponible"' in line:
            # Saltar este else block
            i += 1
            continue
        
        new_lines.append(line)
        i += 1
    
    with open('web/app.py', 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    
    print("✅ Refactorización completada")

if __name__ == '__main__':
    refactor_docker_blocks()
