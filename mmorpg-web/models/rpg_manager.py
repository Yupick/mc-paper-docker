import json
import os
import shutil
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List


class RPGManager:
    """
    Gestiona la integración del plugin MMORPG con el panel web
    
    ESTRUCTURA DE ARCHIVOS RPG:
    ----------------------------
    Universales (compartidos por todos los mundos):
        plugins/MMORPGPlugin/data/
            ├── items.json          # Items universales
            └── mobs.json           # Mobs universales
    
    Locales (específicos por mundo):
        worlds/{world_name}/data/
            ├── npcs.json           # NPCs del mundo
            ├── quests.json         # Quests del mundo
            ├── spawns.json         # Spawn points del mundo
            ├── dungeons.json       # Dungeons del mundo
            ├── players.json        # Jugadores del mundo
            └── status.json         # Estado del mundo RPG
    """
    
    def __init__(self, 
                 base_path: str = None,
                 plugin_data_path: str = None,
                 worlds_path: str = None):
        """
        Inicializa el RPGManager
        
        Args:
            base_path: Ruta base del proyecto (opcional, se detecta automáticamente)
            plugin_data_path: Ruta a plugins/MMORPGPlugin/data (opcional)
            worlds_path: Ruta a worlds/ (opcional)
        """
        # Detectar base_path automáticamente desde la ubicación del script
        if base_path is None:
            # Obtener directorio padre de 'web/' (donde está este archivo)
            current_dir = Path(__file__).resolve().parent.parent
            self.base_path = current_dir
        else:
            self.base_path = Path(base_path)
        
        self.plugin_data_path = Path(plugin_data_path) if plugin_data_path else self.base_path / "plugins" / "MMORPGPlugin" / "data"
        self.worlds_path = Path(worlds_path) if worlds_path else self.base_path / "worlds"
        self.config_path = self.base_path / "config"
        self.universal_db_path = self.config_path / "universal.db"
        
        # Crear directorios si no existen (solo si tenemos permisos)
        try:
            self.plugin_data_path.mkdir(parents=True, exist_ok=True)
        except PermissionError:
            pass  # Directorio ya existe o no tenemos permisos
        
        try:
            self.worlds_path.mkdir(parents=True, exist_ok=True)
        except PermissionError:
            pass  # Directorio ya existe o no tenemos permisos

    def _get_db_connection(self):
        """
        Obtiene conexión a la BD SQLite universal
        
        Returns:
            sqlite3.Connection o None si la BD no existe
        """
        try:
            if self.universal_db_path.exists():
                conn = sqlite3.connect(str(self.universal_db_path))
                conn.row_factory = sqlite3.Row  # Retornar filas como dicts
                return conn
        except Exception as e:
            print(f"Error conectando a BD: {e}")
        return None

    def _get_world_data_dir(self, world_name: str) -> Path:
        """
        Obtiene el directorio de datos locales del mundo ACTIVO
        IMPORTANTE: Siempre usa 'active' independientemente del world_name
        porque worlds/active es un symlink al mundo actualmente activo
        
        Args:
            world_name: Nombre del mundo (ignorado, siempre usa 'active')
            
        Returns:
            Path al directorio worlds/active/data/
        """
        return self.worlds_path / "active" / "data"
    
    def _get_universal_data_dir(self) -> Path:
        """
        Obtiene el directorio de datos universales
        
        Returns:
            Path al directorio plugins/MMORPGPlugin/data/
        """
        return self.plugin_data_path
    
    def _create_world_local_database(self, world_data_dir: Path) -> bool:
        """
        Crea la base de datos local del mundo (world_local.db)
        
        Args:
            world_data_dir: Directorio de datos del mundo (worlds/{world}/data)
            
        Returns:
            True si se creó correctamente
        """
        db_path = world_data_dir / "world_local.db"
        
        # No sobreescribir si ya existe
        if db_path.exists():
            print(f"BD local ya existe: {db_path}")
            return True
        
        try:
            conn = sqlite3.connect(str(db_path))
            cursor = conn.cursor()
            
            # Crear tablas locales del mundo
            # Tabla de spawn points (específicos del mundo)
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS spawn_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    spawn_id TEXT NOT NULL UNIQUE,
                    mob_id TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    spawn_radius REAL DEFAULT 5.0,
                    respawn_time_seconds INTEGER DEFAULT 300,
                    max_mobs INTEGER DEFAULT 1,
                    is_active INTEGER DEFAULT 1,
                    created_at INTEGER
                )
            """)
            
            # Tabla de instancias de dungeons activas
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS dungeon_instances (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dungeon_id TEXT NOT NULL,
                    status TEXT DEFAULT 'WAITING',
                    current_wave INTEGER DEFAULT 0,
                    start_time INTEGER,
                    end_time INTEGER,
                    participants TEXT,
                    created_at INTEGER
                )
            """)
            
            # Tabla de eventos activos en este mundo
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS active_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER,
                    is_active INTEGER DEFAULT 1,
                    data TEXT,
                    created_at INTEGER
                )
            """)
            
            # Tabla de puntos de respawn de jugadores
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS player_respawn_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    set_at INTEGER,
                    UNIQUE(player_uuid)
                )
            """)
            
            # Tabla de estadísticas del mundo
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS world_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    stat_key TEXT NOT NULL UNIQUE,
                    stat_value TEXT,
                    updated_at INTEGER
                )
            """)
            
            # Insertar estadística inicial
            cursor.execute("""
                INSERT OR REPLACE INTO world_stats (stat_key, stat_value, updated_at)
                VALUES ('initialized', 'true', ?)
            """, (int(datetime.now().timestamp()),))
            
            conn.commit()
            conn.close()
            
            print(f"BD local creada: {db_path}")
            return True
            
        except Exception as e:
            print(f"Error creando BD local: {e}")
            return False
    
    def get_rpg_status(self, world_name: str) -> Optional[Dict]:
        """
        Obtiene el estado RPG de un mundo desde worlds/{world_name}/data/status.json
        
        Args:
            world_name: Nombre del mundo
            
        Returns:
            Dict con el estado RPG o None si no existe
        """
        world_data_dir = self._get_world_data_dir(world_name)
        status_file = world_data_dir / "status.json"
        
        if not status_file.exists():
            return None
        
        try:
            with open(status_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo status.json para {world_name}: {e}")
            return None
    
    def get_players_data(self, world_name: str) -> Optional[Dict]:
        """
        Obtiene los datos de jugadores RPG desde worlds/{world_name}/data/players.json
        
        Args:
            world_name: Nombre del mundo
            
        Returns:
            Dict con datos de jugadores o None si no existe
        """
        world_data_dir = self._get_world_data_dir(world_name)
        players_file = world_data_dir / "players.json"
        
        if not players_file.exists():
            return None
        
        try:
            with open(players_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo players.json para {world_name}: {e}")
            return None
    
    def get_world_rpg_config(self, world_name: str) -> Optional[Dict]:
        """
        Obtiene la configuración RPG de un mundo desde worlds/{world_name}/metadata.json
        
        Args:
            world_name: Nombre del mundo
            
        Returns:
            Dict con configuración RPG o None si no existe
        """
        metadata_file = self.worlds_path / world_name / "metadata.json"
        
        if not metadata_file.exists():
            return None
        
        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
                
            if not metadata.get('isRPG', False):
                return None
            
            return metadata.get('rpgConfig', {})
        except Exception as e:
            print(f"Error leyendo metadata.json para {world_name}: {e}")
            return None
    
    def get_rpg_summary(self, world_name: str) -> Optional[Dict]:
        """
        Obtiene un resumen completo del estado RPG de un mundo
        
        Args:
            world_name: Nombre del mundo
            
        Returns:
            Dict con resumen completo o None si el mundo no es RPG
        """
        config = self.get_world_rpg_config(world_name)
        
        if not config:
            return None
        
        status = self.get_rpg_status(world_name)
        players = self.get_players_data(world_name)
        
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
            Lista de nombres de mundos RPG
        """
        rpg_worlds = []
        
        if not self.worlds_path.exists():
            return rpg_worlds
        
        for world_dir in self.worlds_path.iterdir():
            if world_dir.is_dir():
                metadata_file = world_dir / "metadata.json"
                if metadata_file.exists():
                    try:
                        with open(metadata_file, 'r', encoding='utf-8') as f:
                            metadata = json.load(f)
                        if metadata.get('isRPG', False):
                            rpg_worlds.append(world_dir.name)
                    except Exception:
                        pass
        
        return rpg_worlds
    
    def is_rpg_world(self, world_name: str) -> bool:
        """
        Verifica si un mundo tiene el modo RPG activado
        
        Args:
            world_name: Nombre del mundo
            
        Returns:
            True si el mundo es RPG, False en caso contrario
        """
        metadata_file = self.worlds_path / world_name / "metadata.json"
        
        if not metadata_file.exists():
            return False
        
        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                metadata = json.load(f)
            return metadata.get('isRPG', False)
        except Exception:
            return False

    def get_data_by_scope(self, world_name: str, data_type: str, scope: str = 'local') -> Optional[Dict]:
        """
        Obtiene datos RPG separados por scope
        
        Args:
            world_name: Nombre del mundo
            data_type: Tipo de dato (npcs, quests, mobs, items, players, spawns, dungeons, etc.)
            scope: 'local' (mundo), 'universal' (global), o 'auto' (detecta automáticamente)
        
        Returns:
            Dict con los datos o None si no existen
        
        CLASIFICACIÓN DE ARCHIVOS:
        --------------------------
        UNIVERSAL (plugins/MMORPGPlugin/data/):
            - items.json: Items compartidos por todos los mundos
            - mobs.json: Mobs compartidos por todos los mundos
        
        LOCAL (worlds/{world_name}/data/):
            - npcs.json: NPCs específicos del mundo
            - quests.json: Quests específicas del mundo
            - spawns.json: Spawn points del mundo
            - dungeons.json: Dungeons del mundo
            - players.json: Jugadores en el mundo
            - status.json: Estado del mundo RPG
        """
        # Archivos que SIEMPRE son universales
        universal_files = {'items', 'mobs'}
        
        # Archivos que SIEMPRE son locales
        local_files = {'npcs', 'quests', 'spawns', 'dungeons', 'players', 'status'}
        
        # Determinar ruta según tipo de archivo
        if scope == 'universal' or data_type in universal_files:
            # Buscar en plugins/MMORPGPlugin/data/
            file_path = self._get_universal_data_dir() / f"{data_type}.json"
        elif scope == 'local' or data_type in local_files:
            # Buscar en worlds/{world_name}/data/
            file_path = self._get_world_data_dir(world_name) / f"{data_type}.json"
        else:
            # Auto: intentar primero local, luego universal
            local_path = self._get_world_data_dir(world_name) / f"{data_type}.json"
            universal_path = self._get_universal_data_dir() / f"{data_type}.json"
            
            if local_path.exists():
                file_path = local_path
            elif universal_path.exists():
                file_path = universal_path
            else:
                return None
        
        if not file_path.exists():
            return None
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error leyendo {data_type}.json para {world_name}: {e}")
            return None

    def read_file(self, world_name: str, filename: str, scope: str = 'local') -> Optional[Dict]:
        """
        Lee un archivo JSON de datos RPG
        
        Args:
            world_name: Nombre del mundo
            filename: Nombre del archivo (con o sin .json)
            scope: 'local' (mundo), 'universal' (global), o 'auto' (detecta automáticamente)
        
        Returns:
            Contenido del archivo o None si no existe
        """
        if not filename.endswith('.json'):
            filename = f"{filename}.json"
        
        data_type = filename.replace('.json', '')
        return self.get_data_by_scope(world_name, data_type, scope)

    def write_file(self, world_name: str, filename: str, data: Dict, scope: str = 'local') -> bool:
        """
        Escribe un archivo JSON de datos RPG
        
        Args:
            world_name: Nombre del mundo
            filename: Nombre del archivo (con o sin .json)
            data: Datos a escribir
            scope: 'local' (mundo) o 'universal' (global)
        
        Returns:
            True si se escribió correctamente, False en caso contrario
        """
        if not filename.endswith('.json'):
            filename = f"{filename}.json"
        
        universal_files = {'items', 'mobs'}
        data_type = filename.replace('.json', '')
        
        # Determinar ruta destino
        if scope == 'universal' or data_type in universal_files:
            # Escribir en plugins/MMORPGPlugin/data/
            file_path = self._get_universal_data_dir() / filename
        else:
            # Escribir en worlds/{world_name}/data/
            file_path = self._get_world_data_dir(world_name) / filename
        
        try:
            # Crear directorio si no existe
            file_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Escribir archivo
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            return True
        except Exception as e:
            print(f"Error escribiendo {filename} para {world_name}: {e}")
            return False

    def initialize_rpg_world(self, world_name: str, rpg_config: Dict = None) -> bool:
        """
        Inicializa los archivos RPG para un nuevo mundo
        
        ESTRUCTURA CREADA:
        ------------------
        worlds/{world_name}/data/
            ├── npcs.json (legacy - plugin usa BD)
            ├── quests.json (legacy - plugin usa BD)
            ├── world_local.db (BASE DE DATOS LOCAL DEL MUNDO)
            └── status.json
        
        config/data/
            └── universal.db (BASE DE DATOS UNIVERSAL - ya existe)
        
        plugins/MMORPGPlugin/data/
            ├── items.json (copiado desde config/data/ si existe)
            └── mobs.json (copiado desde config/data/ si existe)
        
        Args:
            world_name: Nombre del mundo
            rpg_config: Configuración RPG del mundo
            
        Returns:
            True si se inicializó correctamente
        """
        try:
            # 1. Crear directorio de datos locales del mundo
            world_data_dir = self._get_world_data_dir(world_name)
            world_data_dir.mkdir(parents=True, exist_ok=True)
            
            # 2. Crear base de datos local del mundo (world_local.db)
            self._create_world_local_database(world_data_dir)
            
            # 3. Crear directorio de datos universales (si no existe)
            universal_data_dir = self._get_universal_data_dir()
            universal_data_dir.mkdir(parents=True, exist_ok=True)
            
            # 4. Copiar archivos universales desde config/data/ si existen
            universal_sources = {
                'items.json': self.config_path / 'data' / 'items.json',
                'mobs.json': self.config_path / 'data' / 'mobs.json'
            }
            
            for filename, source_path in universal_sources.items():
                dest_path = universal_data_dir / filename
                # Solo copiar si el origen existe y el destino NO existe
                if source_path.exists() and not dest_path.exists():
                    shutil.copy2(source_path, dest_path)
                    print(f"Copiado {filename} a datos universales")
                elif not dest_path.exists():
                    # Crear archivo vacío con estructura base
                    default_data = {filename.replace('.json', ''): []}
                    with open(dest_path, 'w', encoding='utf-8') as f:
                        json.dump(default_data, f, indent=2, ensure_ascii=False)
                    print(f"Creado {filename} vacío en datos universales")
            
            # 5. Crear archivos locales con estructura base (algunos managers aún usan JSON)
            from datetime import datetime
            timestamp = datetime.utcnow().isoformat() + "Z"
            
            local_files = {
                'status.json': {
                    "active": True,
                    "created_at": timestamp,
                    "last_active": timestamp,
                    "total_players": 0,
                    "total_quests_completed": 0
                },
                'players.json': {"players": {}},
                'npcs.json': {"npcs": []},  # Legacy - plugin usa BD
                'quests.json': {"quests": []},  # Legacy - plugin usa BD
                'spawns.json': {"spawns": []},
                'dungeons.json': {"dungeons": []},
                'invasions.json': {"invasions": []},
                'kills.json': {"kills": [], "playerStats": {}},
                'respawn.json': {"respawnPoints": []},
                'squads.json': {"squads": []}
            }
            
            for filename, default_data in local_files.items():
                file_path = world_data_dir / filename
                if not file_path.exists():
                    with open(file_path, 'w', encoding='utf-8') as f:
                        json.dump(default_data, f, indent=2, ensure_ascii=False)
                    print(f"Creado {filename} en world data/{world_name}/")
            
            print(f"✓ Mundo RPG '{world_name}' inicializado correctamente")
            print(f"  - Datos locales: {world_data_dir}")
            print(f"  - Datos universales: {universal_data_dir}")
            
            return True
        except Exception as e:
            print(f"Error inicializando mundo RPG {world_name}: {e}")
            return False
    # ====== MÉTODOS DE ACCESO A SQLITE ======
    
    def get_achievements(self) -> List[Dict]:
        """
        Obtiene todos los logros desde la BD SQLite
        
        Returns:
            Lista de logros o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM achievements")
            achievements = [dict(row) for row in cursor.fetchall()]
            return achievements
        except Exception as e:
            print(f"Error obteniendo achievements: {e}")
            return []
        finally:
            conn.close()
    
    def get_player_achievements(self, player_uuid: str) -> List[Dict]:
        """
        Obtiene los logros de un jugador desde la BD SQLite
        
        Args:
            player_uuid: UUID del jugador
            
        Returns:
            Lista de logros del jugador o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT pa.*, a.name, a.description, a.icon, a.xp_reward
                FROM player_achievements pa
                JOIN achievements a ON pa.achievement_id = a.achievement_id
                WHERE pa.player_uuid = ?
                ORDER BY pa.unlocked_at DESC
            """, (player_uuid,))
            achievements = [dict(row) for row in cursor.fetchall()]
            return achievements
        except Exception as e:
            print(f"Error obteniendo logros del jugador: {e}")
            return []
        finally:
            conn.close()
    
    def get_crafting_recipes(self) -> List[Dict]:
        """
        Obtiene todas las recetas de crafteo desde la BD SQLite
        
        Returns:
            Lista de recetas o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM crafting_recipes")
            recipes = [dict(row) for row in cursor.fetchall()]
            return recipes
        except Exception as e:
            print(f"Error obteniendo recetas de crafteo: {e}")
            return []
        finally:
            conn.close()
    
    def get_bestiary_entries(self) -> List[Dict]:
        """
        Obtiene todas las entradas del bestiario desde la BD SQLite
        
        Returns:
            Lista de entradas del bestiario o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM bestiary_entries")
            entries = [dict(row) for row in cursor.fetchall()]
            return entries
        except Exception as e:
            print(f"Error obteniendo entradas del bestiario: {e}")
            return []
        finally:
            conn.close()
    
    def get_player_bestiary(self, player_uuid: str) -> List[Dict]:
        """
        Obtiene el progreso del bestiario de un jugador
        
        Args:
            player_uuid: UUID del jugador
            
        Returns:
            Lista de entradas descubiertas o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT pb.*, be.mob_id, be.display_name
                FROM player_bestiary pb
                JOIN bestiary_entries be ON pb.entry_id = be.entry_id
                WHERE pb.player_uuid = ?
            """, (player_uuid,))
            entries = [dict(row) for row in cursor.fetchall()]
            return entries
        except Exception as e:
            print(f"Error obteniendo bestiario del jugador: {e}")
            return []
        finally:
            conn.close()
    
    def get_enchantments(self) -> List[Dict]:
        """
        Obtiene todos los encantamientos personalizados desde la BD SQLite
        
        Returns:
            Lista de encantamientos o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM enchantments")
            enchantments = [dict(row) for row in cursor.fetchall()]
            return enchantments
        except Exception as e:
            print(f"Error obteniendo encantamientos: {e}")
            return []
        finally:
            conn.close()
    
    def get_squads(self) -> List[Dict]:
        """
        Obtiene todos los squads desde la BD SQLite
        
        Returns:
            Lista de squads o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM squads")
            squads = [dict(row) for row in cursor.fetchall()]
            return squads
        except Exception as e:
            print(f"Error obteniendo squads: {e}")
            return []
        finally:
            conn.close()
    
    def get_squad_members(self, squad_id: str) -> List[Dict]:
        """
        Obtiene los miembros de un squad
        
        Args:
            squad_id: ID del squad
            
        Returns:
            Lista de miembros o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT * FROM squad_members
                WHERE squad_id = ?
            """, (squad_id,))
            members = [dict(row) for row in cursor.fetchall()]
            return members
        except Exception as e:
            print(f"Error obteniendo miembros del squad: {e}")
            return []
        finally:
            conn.close()
    
    def get_player_balance(self, player_uuid: str) -> Dict:
        """
        Obtiene el balance de un jugador desde la BD SQLite
        
        Args:
            player_uuid: UUID del jugador
            
        Returns:
            Dict con balance o {"coins": 0, "bank": 0} si no existe
        """
        conn = self._get_db_connection()
        if not conn:
            return {"coins": 0, "bank": 0}
        
        try:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT coins, bank
                FROM player_economy
                WHERE player_uuid = ?
            """, (player_uuid,))
            row = cursor.fetchone()
            if row:
                return {"coins": row["coins"], "bank": row["bank"]}
            return {"coins": 0, "bank": 0}
        except Exception as e:
            print(f"Error obteniendo balance del jugador: {e}")
            return {"coins": 0, "bank": 0}
        finally:
            conn.close()
    
    def get_economy_transactions(self, player_uuid: str = None, limit: int = 100) -> List[Dict]:
        """
        Obtiene historial de transacciones económicas
        
        Args:
            player_uuid: UUID del jugador (opcional, si None obtiene todas)
            limit: Cantidad máxima de transacciones
            
        Returns:
            Lista de transacciones o [] si no hay
        """
        conn = self._get_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            if player_uuid:
                cursor.execute("""
                    SELECT * FROM economy_transactions
                    WHERE player_uuid = ?
                    ORDER BY transaction_time DESC
                    LIMIT ?
                """, (player_uuid, limit))
            else:
                cursor.execute("""
                    SELECT * FROM economy_transactions
                    ORDER BY transaction_time DESC
                    LIMIT ?
                """, (limit,))
            transactions = [dict(row) for row in cursor.fetchall()]
            return transactions
        except Exception as e:
            print(f"Error obteniendo transacciones: {e}")
            return []
        finally:
            conn.close()