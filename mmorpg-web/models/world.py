"""
Modelo para gestionar mundos individuales de Minecraft
"""
import json
import os
from datetime import datetime
from pathlib import Path
import subprocess


class World:
    """Representa un mundo individual de Minecraft"""
    
    def __init__(self, slug, worlds_base_path="/server/worlds"):
        """
        Inicializar mundo
        
        Args:
            slug: Identificador único del mundo (ej: "survival-1")
            worlds_base_path: Ruta base donde se almacenan los mundos
        """
        self.slug = slug
        self.worlds_base_path = Path(worlds_base_path)
        self.path = self.worlds_base_path / slug
        self.metadata_file = self.path / "metadata.json"
        self.server_properties_file = self.path / "server.properties"
        self.metadata = self._load_metadata()
    
    def exists(self):
        """Verificar si el mundo existe"""
        return self.path.exists() and self.path.is_dir()
    
    def _load_metadata(self):
        """
        Cargar metadata desde metadata.json
        
        Returns:
            dict: Metadata del mundo
        """
        if not self.exists():
            return None
        
        if not self.metadata_file.exists():
            # Crear metadata por defecto si no existe
            return self._create_default_metadata()
        
        try:
            with open(self.metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
            
            # Validar campos requeridos
            required_fields = ['name', 'slug', 'created_at']
            for field in required_fields:
                if field not in metadata:
                    raise ValueError(f"Campo requerido faltante: {field}")
            
            return metadata
        except (json.JSONDecodeError, ValueError) as e:
            print(f"Error al cargar metadata de {self.slug}: {e}")
            return self._create_default_metadata()
    
    def _create_default_metadata(self):
        """Crear metadata por defecto"""
        return {
            "name": self.slug.replace('-', ' ').title(),
            "slug": self.slug,
            "description": "",
            "gamemode": "survival",
            "difficulty": "normal",
            "created_at": datetime.utcnow().isoformat() + "Z",
            "last_played": datetime.utcnow().isoformat() + "Z",
            "size_mb": 0,
            "seed": "",
            "version": "1.21.1",
            "spawn": {"x": 0, "y": 64, "z": 0},
            "settings": {
                "pvp": True,
                "spawn_monsters": True,
                "spawn_animals": True,
                "view_distance": 10,
                "max_players": 20
            },
            "tags": []
        }
    
    def save_metadata(self):
        """Guardar metadata a disco"""
        if not self.exists():
            raise FileNotFoundError(f"Mundo {self.slug} no existe")
        
        # Actualizar timestamp de modificación
        self.metadata['last_modified'] = datetime.utcnow().isoformat() + "Z"
        
        with open(self.metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.metadata, f, indent=2, ensure_ascii=False)
    
    def get_size(self):
        """
        Calcular tamaño total del mundo en MB
        
        Returns:
            int: Tamaño en megabytes
        """
        if not self.exists():
            return 0
        
        try:
            # Usar du para calcular tamaño
            result = subprocess.run(
                ['du', '-sm', str(self.path)],
                capture_output=True,
                text=True,
                check=True
            )
            size_mb = int(result.stdout.split()[0])
            
            # Actualizar en metadata
            self.metadata['size_mb'] = size_mb
            return size_mb
        except (subprocess.CalledProcessError, ValueError, IndexError):
            return self.metadata.get('size_mb', 0)
    
    def update_last_played(self):
        """Actualizar timestamp de última vez jugado"""
        self.metadata['last_played'] = datetime.utcnow().isoformat() + "Z"
        self.save_metadata()
    
    def get_dimensions(self):
        """
        Obtener lista de dimensiones del mundo
        
        Returns:
            list: Lista de dimensiones disponibles
        """
        dimensions = []
        dimension_folders = [
            ('world', 'Overworld'),
            ('world_nether', 'Nether'),
            ('world_the_end', 'The End')
        ]
        
        for folder, name in dimension_folders:
            dim_path = self.path / folder
            if dim_path.exists() and dim_path.is_dir():
                dimensions.append({
                    'name': name,
                    'folder': folder,
                    'exists': True
                })
        
        return dimensions
    
    def get_player_count(self):
        """
        Contar jugadores únicos que han jugado en este mundo
        
        Returns:
            int: Número de jugadores únicos
        """
        playerdata_path = self.path / 'world' / 'playerdata'
        
        if not playerdata_path.exists():
            return 0
        
        # Contar archivos .dat (cada uno representa un jugador)
        player_files = list(playerdata_path.glob('*.dat'))
        return len(player_files)
    
    def get_server_properties(self):
        """
        Leer server.properties del mundo
        
        Returns:
            dict: Propiedades del servidor
        """
        if not self.server_properties_file.exists():
            return {}
        
        properties = {}
        try:
            with open(self.server_properties_file, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#') and '=' in line:
                        key, value = line.split('=', 1)
                        properties[key.strip()] = value.strip()
        except Exception as e:
            print(f"Error al leer server.properties: {e}")
        
        return properties
    
    def update_server_properties(self, properties):
        """
        Actualizar server.properties del mundo
        
        Args:
            properties: dict con propiedades a actualizar
        """
        if not self.exists():
            raise FileNotFoundError(f"Mundo {self.slug} no existe")
        
        # Leer propiedades actuales
        current_props = self.get_server_properties()
        
        # Actualizar con nuevas propiedades
        current_props.update(properties)
        
        # Escribir de vuelta
        try:
            with open(self.server_properties_file, 'w', encoding='utf-8') as f:
                for key, value in current_props.items():
                    f.write(f"{key}={value}\n")
        except Exception as e:
            raise IOError(f"Error al escribir server.properties: {e}")
    
    def to_dict(self):
        """
        Convertir mundo a diccionario para API
        
        Returns:
            dict: Representación del mundo
        """
        if not self.exists():
            return None
        
        # Actualizar tamaño antes de retornar
        size = self.get_size()
        
        world_dict = {
            'slug': self.slug,
            'name': self.metadata.get('name', self.slug),
            'description': self.metadata.get('description', ''),
            'gamemode': self.metadata.get('gamemode', 'survival'),
            'difficulty': self.metadata.get('difficulty', 'normal'),
            'size_mb': size,
            'created_at': self.metadata.get('created_at'),
            'last_played': self.metadata.get('last_played'),
            'seed': self.metadata.get('seed', ''),
            'version': self.metadata.get('version', '1.21.1'),
            'dimensions': self.get_dimensions(),
            'player_count': self.get_player_count(),
            'settings': self.metadata.get('settings', {}),
            'tags': self.metadata.get('tags', []),
            'isRPG': self.metadata.get('isRPG', False)
        }
        
        # Añadir configuración RPG si está activado
        if world_dict['isRPG']:
            world_dict['rpgConfig'] = self.metadata.get('rpgConfig', {
                'classesEnabled': True,
                'questsEnabled': True,
                'npcsEnabled': True,
                'economyEnabled': True
            })
        
        return world_dict
    
    def __repr__(self):
        return f"<World slug='{self.slug}' name='{self.metadata.get('name', 'Unknown')}'>"
