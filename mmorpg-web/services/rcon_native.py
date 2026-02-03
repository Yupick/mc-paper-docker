"""
Servicio nativo de RCON para Minecraft sin Docker
"""
import os
import psutil
from mcrcon import MCRcon


class RCONService:
    """Maneja conexiones RCON al servidor Minecraft nativo"""
    
    def __init__(self, host='localhost', port=25575, password=None):
        self.host = host
        self.port = port
        self.password = password or os.getenv('RCON_PASSWORD', 'minecraft123')
    
    def execute_command(self, command):
        """
        Ejecuta un comando RCON directamente al servidor
        
        Args:
            command: Comando de Minecraft a ejecutar
            
        Returns:
            dict con 'output' (bytes) y 'exit_code' (int)
        """
        try:
            with MCRcon(self.host, self.password, self.port) as mcr:
                response = mcr.command(command)
                return {
                    'output': response.encode('utf-8'),
                    'exit_code': 0
                }
        except Exception as e:
            return {
                'output': str(e).encode('utf-8'),
                'exit_code': 1
            }


class ServerMonitor:
    """Monitorea el estado del servidor Minecraft nativo"""
    
    def __init__(self, minecraft_dir):
        self.minecraft_dir = minecraft_dir
        self.server_dir = os.path.join(minecraft_dir, 'minecraft-server')
        self.pid_file = os.path.join(self.server_dir, 'server.pid')
        self.log_file = os.path.join(self.server_dir, 'logs', 'latest.log')
    
    def get_status(self):
        """
        Verifica si el servidor está corriendo
        
        Returns:
            str: 'running' o 'stopped'
        """
        if not os.path.exists(self.pid_file):
            return 'stopped'
        
        try:
            with open(self.pid_file, 'r') as f:
                pid = int(f.read().strip())
            # Verificar si el proceso existe
            os.kill(pid, 0)  # No mata el proceso, solo verifica
            return 'running'
        except (OSError, ProcessLookupError, ValueError):
            return 'stopped'
    
    def get_logs(self, lines=100):
        """
        Lee las últimas N líneas del log del servidor
        
        Args:
            lines: Número de líneas a leer
            
        Returns:
            str: Contenido del log
        """
        if not os.path.exists(self.log_file):
            return ""
        
        try:
            with open(self.log_file, 'r', encoding='utf-8', errors='ignore') as f:
                all_lines = f.readlines()
                return ''.join(all_lines[-lines:])
        except Exception as e:
            return f"Error leyendo logs: {e}"
    
    def get_stats(self):
        """
        Obtiene estadísticas del proceso del servidor
        
        Returns:
            dict con cpu_percent, memory_mb, threads, status o None si no está corriendo
        """
        if not os.path.exists(self.pid_file):
            return None
        
        try:
            with open(self.pid_file, 'r') as f:
                pid = int(f.read().strip())
            process = psutil.Process(pid)
            
            # Obtener uso de CPU (interval=1 para medición precisa)
            cpu_percent = process.cpu_percent(interval=0.5)
            
            # Memoria en MB
            mem_info = process.memory_info()
            memory_mb = mem_info.rss / 1024 / 1024
            
            return {
                'cpu_percent': cpu_percent,
                'memory_mb': memory_mb,
                'memory_bytes': mem_info.rss,
                'threads': process.num_threads(),
                'status': process.status(),
                'create_time': process.create_time()
            }
        except (psutil.NoSuchProcess, FileNotFoundError, ValueError):
            return None
    
    def get_pid(self):
        """
        Obtiene el PID del servidor
        
        Returns:
            int o None si no está corriendo
        """
        if not os.path.exists(self.pid_file):
            return None
        
        try:
            with open(self.pid_file, 'r') as f:
                return int(f.read().strip())
        except (FileNotFoundError, ValueError):
            return None
