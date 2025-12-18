import json
import os
from pathlib import Path
from typing import Optional, Dict, List


class RPGManager:
    """Gestiona la integración del plugin MMORPG con el panel web"""
    
    def __init__(self, plugin_data_path: str = None, worlds_base_path: str = None):
        """
        Inicializa el RPG Manager
        
        Args:
            plugin_data_path: Ruta al directorio de datos del plugin (ej: /path/plugins/MMORPGPlugin/data)
            worlds_base_path: Ruta base donde se almacenan los mundos (ej: /path/worlds)
        """
        if plugin_data_path is None:
            # Ruta por defecto (para compatibilidad con código existente)
            plugin_data_path = "/home/mkd/contenedores/mc-paper/plugins/MMORPGPlugin/data"
        
        if worlds_base_path is None:
            # Ruta por defecto
            worlds_base_path = "/home/mkd/contenedores/mc-paper/worlds"
        
        self.plugin_data_path = Path(plugin_data_path)
        self.worlds_base_path = Path(worlds_base_path)

    def _resolve_world_data_dir(self, world_slug: str) -> Path:
        """Devuelve la carpeta donde el plugin guarda datos para el mundo activo."""
        # 1) Intentar level-name real (server.properties)
        server_props = self.worlds_base_path / world_slug / "server.properties"
        candidates = []
        if server_props.exists():
            try:
                with open(server_props, 'r', encoding='utf-8') as f:
                    for line in f:
                        if line.strip().startswith('level-name'):
                            level_name = line.split('=', 1)[1].strip()
                            if level_name:
                                candidates.append(Path(level_name).name)
                            break
            except Exception:
                pass

        # 2) Slug del mundo
        candidates.append(world_slug)

        # Buscar el primero que exista, si no, devolver el primero como ruta de destino
        for candidate in candidates:
            candidate_path = self.plugin_data_path / candidate
            if candidate_path.exists():
                return candidate_path

        return self.plugin_data_path / candidates[0]
    
    def get_rpg_status(self, world_slug: str) -> Optional[Dict]:
        """
        Obtiene el estado RPG de un mundo desde los datos del plugin
        
        Args:
            world_slug: Slug del mundo
            
        Returns:
            Dict con el estado RPG o None si no existe
        """
        world_dir = self._resolve_world_data_dir(world_slug)
        status_file = world_dir / "status.json"
        
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
        world_dir = self._resolve_world_data_dir(world_slug)
        players_file = world_dir / "players.json"
        
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
        metadata_file = self.worlds_base_path / world_slug / "metadata.json"
        
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
        metadata_file = self.worlds_base_path / world_slug / "metadata.json"
        
        if not metadata_file.exists():
            return False
        
        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
            return metadata.get('isRPG', False)
        except Exception:
            return False

    def get_data_by_scope(self, world_slug: str, data_type: str, scope: str = 'local') -> Optional[Dict]:
        """
        Obtiene datos RPG separados por scope (local, universal, exclusive-local)
        
        Args:
            world_slug: Slug del mundo
            data_type: Tipo de dato (npcs, quests, mobs, items, players, etc.)
            scope: 'local' (per-mundo), 'universal' (global), o 'exclusive-local'
        
        Returns:
            Dict con los datos o None si no existen
        
        Clasificación:
            UNIVERSAL: items, mobs_global, npcs_global, quests_global, enchantments_global, pets_global
            LOCAL: npcs, quests, mobs, pets, enchantments (busca primero local, luego universal)
            EXCLUSIVE-LOCAL: players, status, invasions, kills, respawn, squads (solo local)
        """
        universal_data = {'items', 'mobs_global', 'npcs_global', 'quests_global', 
                         'enchantments_global', 'pets_global'}
        hybrid_data = {'npcs', 'quests', 'mobs', 'pets', 'enchantments'}
        exclusive_local_data = {'players', 'status', 'invasions', 'kills', 'respawn', 'squads'}
        
        world_dir = self._resolve_world_data_dir(world_slug)
        
        # Resolver ruta según scope
        if scope == 'universal' or data_type in universal_data:
            file_path = self.plugin_data_path / f"{data_type}.json"
        elif scope == 'exclusive-local' or data_type in exclusive_local_data:
            file_path = world_dir / f"{data_type}.json"
        else:  # scope == 'local' or hybrid_data
            local_file = world_dir / f"{data_type}.json"
            universal_file = self.plugin_data_path / f"{data_type}.json"
            # Preferir local si existe, sino universal
            file_path = local_file if local_file.exists() else universal_file
        
        if not file_path.exists():
            return None
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo {data_type}.json para {world_slug}: {e}")
            return None

    def read_file(self, world_slug: str, filename: str, scope: str = 'local') -> Optional[Dict]:
        """
        Lee un archivo JSON de datos RPG
        
        Args:
            world_slug: Slug del mundo
            filename: Nombre del archivo (con o sin .json)
            scope: 'local', 'universal', o 'exclusive-local'
        
        Returns:
            Contenido del archivo o None si no existe
        """
        if not filename.endswith('.json'):
            filename = f"{filename}.json"
        
        data_type = filename.replace('.json', '')
        return self.get_data_by_scope(world_slug, data_type, scope)

    def write_file(self, world_slug: str, filename: str, data: Dict, scope: str = 'local') -> bool:
        """
        Escribe un archivo JSON de datos RPG
        
        Args:
            world_slug: Slug del mundo
            filename: Nombre del archivo (con o sin .json)
            data: Datos a escribir
            scope: 'local', 'universal', o 'exclusive-local'
        
        Returns:
            True si se escribió correctamente, False en caso contrario
        """
        if not filename.endswith('.json'):
            filename = f"{filename}.json"
        
        universal_data = {'items', 'mobs_global', 'npcs_global', 'quests_global', 
                         'enchantments_global', 'pets_global'}
        exclusive_local_data = {'players', 'status', 'invasions', 'kills', 'respawn', 'squads'}
        
        data_type = filename.replace('.json', '')
        world_dir = self._resolve_world_data_dir(world_slug)
        
        # Determinar ruta destino
        if scope == 'universal' or data_type in universal_data:
            file_path = self.plugin_data_path / filename
        else:  # local o exclusive-local
            file_path = world_dir / filename
        
        try:
            # Crear directorio si no existe
            file_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Escribir archivo
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            return True
        except Exception as e:
            print(f"Error escribiendo {filename} para {world_slug}: {e}")
            return False

    def initialize_rpg_world(self, world_slug: str, rpg_config: Optional[Dict] = None) -> bool:
        """
        Inicializa los archivos RPG para un nuevo mundo
        Copia archivos universales y crea archivos exclusive-local vacíos
        
        Args:
            world_slug: Slug del mundo
            rpg_config: Configuración RPG del mundo (opcional)
            
        Returns:
            True si se inicializó correctamente, False en caso contrario
        """
        try:
            world_dir = self._resolve_world_data_dir(world_slug)
            
            # Crear directorio del mundo si no existe
            world_dir.mkdir(parents=True, exist_ok=True)
            
            # Archivos híbridos que pueden tener versión local (copiar de universal)
            hybrid_files = ['npcs.json', 'quests.json', 'mobs.json', 'pets.json', 'enchantments.json']
            
            for filename in hybrid_files:
                universal_file = self.plugin_data_path / filename
                local_file = world_dir / filename
                
                # Si existe el archivo universal, copiarlo como base
                if universal_file.exists():
                    try:
                        with open(universal_file, 'r', encoding='utf-8') as f:
                            data = json.load(f)
                        
                        # Escribir en archivo local
                        with open(local_file, 'w', encoding='utf-8') as f:
                            json.dump(data, f, indent=2, ensure_ascii=False)
                    except Exception as e:
                        print(f"Error copiando {filename}: {e}")
                        # Si falla, crear archivo vacío con estructura básica
                        empty_data = {}
                        if filename in ['npcs.json', 'quests.json']:
                            key = filename.replace('.json', '')
                            empty_data = {key: []}
                        elif filename == 'mobs.json':
                            empty_data = {'mobs': {}}
                        
                        with open(local_file, 'w', encoding='utf-8') as f:
                            json.dump(empty_data, f, indent=2, ensure_ascii=False)
                else:
                    # No existe archivo universal, crear vacío
                    empty_data = {}
                    if filename in ['npcs.json', 'quests.json']:
                        key = filename.replace('.json', '')
                        empty_data = {key: []}
                    elif filename == 'mobs.json':
                        empty_data = {'mobs': {}}
                    
                    with open(local_file, 'w', encoding='utf-8') as f:
                        json.dump(empty_data, f, indent=2, ensure_ascii=False)
            
            # Archivos exclusive-local (crear vacíos con estructura inicial)
            exclusive_files = {
                'players.json': {},
                'status.json': {
                    'rpgEnabled': True,
                    'serverStarted': False,
                    'worldName': world_slug,
                    'lastUpdate': None
                },
                'invasions.json': {
                    'invasions': [],
                    'active': []
                },
                'kills.json': {
                    'kills': {}
                },
                'respawn.json': {
                    'zones': [],
                    'spawns': []
                },
                'spawns.json': {
                    'item_spawns': [],
                    'mob_spawns': []
                },
                'squads.json': {
                    'squads': []
                },
                'metadata.json': rpg_config or {
                    'classesEnabled': True,
                    'questsEnabled': True,
                    'customMobsEnabled': True,
                    'petsEnabled': True
                }
            }
            
            for filename, initial_data in exclusive_files.items():
                local_file = world_dir / filename
                if not local_file.exists():
                    with open(local_file, 'w', encoding='utf-8') as f:
                        json.dump(initial_data, f, indent=2, ensure_ascii=False)
            
            print(f"✓ Archivos RPG inicializados para el mundo '{world_slug}'")
            return True
            
        except Exception as e:
            print(f"Error inicializando archivos RPG para {world_slug}: {e}")
            return False
