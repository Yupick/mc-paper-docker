import json
import os
from pathlib import Path
from typing import Optional, Dict, List


class RPGManager:
    """Gestiona la integración del plugin MMORPG con el panel web"""
    
    def __init__(self, plugin_data_path: str = "/home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data"):
        self.plugin_data_path = Path(plugin_data_path)
    
    def get_rpg_status(self, world_slug: str) -> Optional[Dict]:
        """
        Obtiene el estado RPG de un mundo desde los datos del plugin
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            Dict con el estado RPG o None si no existe
        """
        status_file = self.plugin_data_path / world_slug / "status.json"
        
        if not status_file.exists():
            return None
        
        try:
            with open(status_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo status.json para {world_slug}: {e}")
            return None
    
    def get_players_data(self, world_slug: str) -> Optional[Dict]:
        """
        Obtiene los datos de jugadores RPG de un mundo
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            Dict con datos de jugadores o None si no existe
        """
        players_file = self.plugin_data_path / world_slug / "players.json"
        
        if not players_file.exists():
            return None
        
        try:
            with open(players_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo players.json para {world_slug}: {e}")
            return None
    
    def get_world_rpg_config(self, world_slug: str) -> Optional[Dict]:
        """
        Obtiene la configuración RPG de un mundo desde metadata.json
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            Dict con configuración RPG o None si no existe
        """
        # Leer desde el directorio de mundos
        worlds_path = Path("/home/mkd/contenedores/mc-paper/worlds")
        metadata_file = worlds_path / world_slug / "metadata.json"
        
        if not metadata_file.exists():
            return None
        
        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
                
            if not metadata.get('isRPG', False):
                return None
            
            return metadata.get('rpgConfig', {})
        except Exception as e:
            print(f"Error leyendo metadata.json para {world_slug}: {e}")
            return None
    
    def get_rpg_summary(self, world_slug: str) -> Optional[Dict]:
        """
        Obtiene un resumen completo del estado RPG de un mundo
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            Dict con resumen completo o None si el mundo no es RPG
        """
        config = self.get_world_rpg_config(world_slug)
        
        if not config:
            return None
        
        status = self.get_rpg_status(world_slug)
        players = self.get_players_data(world_slug)
        
        return {
            'config': config,
            'status': status if status else {},
            'players': players if players else {},
            'isActive': status is not None
        }
    
    def list_rpg_worlds(self) -> List[str]:
        """
        Lista todos los mundos que tienen modo RPG activado
        
        Returns:
            Lista de slugs de mundos RPG
        """
        rpg_worlds = []
        
        if not self.plugin_data_path.exists():
            return rpg_worlds
        
        for world_dir in self.plugin_data_path.iterdir():
            if world_dir.is_dir():
                status_file = world_dir / "status.json"
                if status_file.exists():
                    rpg_worlds.append(world_dir.name)
        
        return rpg_worlds
    
    def is_rpg_world(self, world_slug: str) -> bool:
        """
        Verifica si un mundo tiene el modo RPG activado
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            True si el mundo es RPG, False en caso contrario
        """
        metadata_file = Path("/home/mkd/contenedores/mc-paper/worlds") / world_slug / "metadata.json"
        
        if not metadata_file.exists():
            return False
        
        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
            return metadata.get('isRPG', False)
        except Exception:
            return False
