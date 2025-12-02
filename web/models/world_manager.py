"""
Gestor central de mundos de Minecraft
"""
import json
import os
import shutil
import subprocess
from pathlib import Path
from datetime import datetime
from .world import World


class WorldManager:
    """Gestiona todos los mundos del servidor"""
    
    def __init__(self, worlds_base_path="/server/worlds"):
        """
        Inicializar gestor de mundos
        
        Args:
            worlds_base_path: Ruta base donde se almacenan los mundos
        """
        self.worlds_base_path = Path(worlds_base_path)
        self.config_file = self.worlds_base_path / "worlds.json"
        self.active_symlink = self.worlds_base_path / "active"
        self.templates_path = self.worlds_base_path / "templates"
        
        # Asegurar que existen los directorios necesarios
        self.worlds_base_path.mkdir(parents=True, exist_ok=True)
        self.templates_path.mkdir(exist_ok=True)
        
        # Cargar configuración
        self.config = self._load_config()
    
    def _load_config(self):
        """Cargar configuración desde worlds.json"""
        if not self.config_file.exists():
            return self._create_default_config()
        
        try:
            with open(self.config_file, 'r', encoding='utf-8') as f:
                config = json.load(f)
            return config
        except (json.JSONDecodeError, IOError) as e:
            print(f"Error al cargar worlds.json: {e}")
            return self._create_default_config()
    
    def _create_default_config(self):
        """Crear configuración por defecto"""
        config = {
            "active_world": None,
            "worlds": [],
            "settings": {
                "max_worlds": 10,
                "auto_backup_before_switch": True,
                "keep_backups": 5
            }
        }
        self._save_config(config)
        return config
    
    def _save_config(self, config=None):
        """Guardar configuración a disco"""
        if config is None:
            config = self.config
        
        with open(self.config_file, 'w', encoding='utf-8') as f:
            json.dump(config, f, indent=2, ensure_ascii=False)
    
    def list_worlds(self):
        """
        Listar todos los mundos disponibles
        
        Returns:
            list: Lista de objetos World
        """
        worlds = []
        
        # Iterar sobre directorios en worlds/
        for item in self.worlds_base_path.iterdir():
            # Ignorar symlinks, templates y archivos
            if item.is_symlink() or item.name == 'templates' or not item.is_dir():
                continue
            
            # Verificar que contenga al menos una dimensión
            has_world_data = (
                (item / 'world').exists() or
                (item / 'world_nether').exists() or
                (item / 'world_the_end').exists()
            )
            
            if has_world_data or (item / 'metadata.json').exists():
                world = World(item.name, str(self.worlds_base_path))
                worlds.append(world)
        
        return worlds
    
    def get_world(self, slug):
        """
        Obtener un mundo específico
        
        Args:
            slug: Identificador del mundo
            
        Returns:
            World: Objeto World o None si no existe
        """
        world = World(slug, str(self.worlds_base_path))
        return world if world.exists() else None
    
    def get_active_world(self):
        """
        Obtener el mundo actualmente activo
        
        Returns:
            World: Mundo activo o None
        """
        # Verificar symlink
        if not self.active_symlink.exists():
            return None
        
        if not self.active_symlink.is_symlink():
            return None
        
        # Leer symlink para obtener el slug del mundo activo
        target = self.active_symlink.resolve()
        slug = target.name
        
        return self.get_world(slug)
    
    def create_world(self, name, template="vanilla", seed="", gamemode="survival", 
                    difficulty="normal", description="", tags=None, motd=None):
        """
        Crear nuevo mundo
        
        Args:
            name: Nombre del mundo
            template: Plantilla a usar (vanilla, flat, amplified, etc)
            seed: Seed del mundo (opcional)
            gamemode: Modo de juego
            difficulty: Dificultad
            description: Descripción del mundo
            tags: Lista de etiquetas
                        motd: Mensaje del día (MOTD)
            
        Returns:
            World: Mundo creado o None si falla
        """
        # Generar slug a partir del nombre
        slug = name.lower().replace(' ', '-').replace('_', '-')
        slug = ''.join(c for c in slug if c.isalnum() or c == '-')
        
        # Verificar que no exista
        if self.get_world(slug) is not None:
            raise ValueError(f"Ya existe un mundo con el slug: {slug}")
        
        # Verificar límite de mundos
        max_worlds = self.config['settings'].get('max_worlds', 10)
        if len(self.list_worlds()) >= max_worlds:
            raise ValueError(f"Se alcanzó el límite de {max_worlds} mundos")
        
        # Crear directorio del mundo
        world_path = self.worlds_base_path / slug
        world_path.mkdir(exist_ok=True)
        
        # Crear metadata
        metadata = {
            "name": name,
            "slug": slug,
            "description": description,
            "gamemode": gamemode,
            "difficulty": difficulty,
            "created_at": datetime.utcnow().isoformat() + "Z",
            "last_played": None,
            "size_mb": 0,
            "seed": seed,
            "version": "1.21.1",
            "spawn": {"x": 0, "y": 64, "z": 0},
            "settings": {
                "pvp": True,
                "spawn_monsters": True,
                "spawn_animals": True,
                "view_distance": 10,
                "max_players": 20
            },
            "tags": tags or []
        }
        
        metadata_file = world_path / "metadata.json"
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        
        # Crear server.properties base
        self._create_server_properties(world_path, gamemode, difficulty, seed, template, motd)
        
        # Copiar template si existe
        if template != "vanilla":
            template_path = self.templates_path / template
            if template_path.exists():
                # Copiar archivos del template
                for item in template_path.iterdir():
                    if item.is_dir():
                        shutil.copytree(item, world_path / item.name, dirs_exist_ok=True)
                    else:
                        shutil.copy2(item, world_path / item.name)
        
        # Asegurar estructura de dimensiones requerida
        self._ensure_world_structure(world_path)
        
        # Actualizar configuración
        self.config['worlds'].append({
            "slug": slug,
            "status": "inactive",
            "auto_backup": True,
            "backup_interval": "6h"
        })
        self._save_config()
        
        return World(slug, str(self.worlds_base_path))
    
    def _create_server_properties(self, world_path, gamemode, difficulty, seed, level_type, motd=None):
        """Crear archivo server.properties para el mundo"""
        properties = {
            "gamemode": gamemode,
            "difficulty": difficulty,
            "level-seed": seed,
            "level-type": "minecraft:normal" if level_type == "vanilla" else f"minecraft:{level_type}",
            "motd": motd or "A Minecraft Server",
            "pvp": "true",
            "spawn-monsters": "true",
            "spawn-animals": "true",
            "spawn-npcs": "true",
            "view-distance": "10",
            "simulation-distance": "10",
            "max-players": "20",
            "enable-rcon": "true",
            "rcon.port": "25575",
            "rcon.password": "minecraft123",
            "broadcast-rcon-to-ops": "true"
        }
        
        server_properties_file = world_path / "server.properties"
        with open(server_properties_file, 'w', encoding='utf-8') as f:
            for key, value in properties.items():
                f.write(f"{key}={value}\n")

    def _ensure_world_structure(self, world_path: Path):
        """Asegura que existan las carpetas de dimensiones requeridas."""
        for sub in ("world", "world_nether", "world_the_end"):
            (world_path / sub).mkdir(parents=True, exist_ok=True)
    
    def delete_world(self, slug, create_backup=True):
        """
        Eliminar mundo
        
        Args:
            slug: Identificador del mundo
            create_backup: Si crear backup antes de eliminar
            
        Returns:
            bool: True si se eliminó correctamente
        """
        # Verificar que no sea el mundo activo
        active_world = self.get_active_world()
        if active_world and active_world.slug == slug:
            raise ValueError("No se puede eliminar el mundo activo. Cambia a otro mundo primero.")
        
        world = self.get_world(slug)
        if world is None:
            raise FileNotFoundError(f"Mundo {slug} no encontrado")
        
        # Crear backup si se solicita
        if create_backup:
            # TODO: Implementar con BackupService
            pass
        
        # Eliminar directorio
        shutil.rmtree(world.path)
        
        # Actualizar configuración
        self.config['worlds'] = [
            w for w in self.config['worlds'] if w['slug'] != slug
        ]
        self._save_config()
        
        return True
    
    def switch_world(self, slug):
        """
        Cambiar al mundo especificado
        
        Args:
            slug: Identificador del mundo destino
            
        Returns:
            bool: True si el cambio fue exitoso
        """
        # Verificar que el mundo existe
        target_world = self.get_world(slug)
        if target_world is None:
            raise FileNotFoundError(f"Mundo {slug} no encontrado")
        
        # Verificar que no sea ya el mundo activo
        current_active = self.get_active_world()
        if current_active and current_active.slug == slug:
            raise ValueError(f"El mundo {slug} ya está activo")
        
        # Asegurar estructura del mundo destino
        self._ensure_world_structure(target_world.path)

        # Actualizar symlink de forma robusta
        try:
            if self.active_symlink.is_symlink() or self.active_symlink.exists():
                self.active_symlink.unlink()
        except FileNotFoundError:
            pass
        
        # Crear nuevo symlink (relativo) apuntando al slug
        self.active_symlink.symlink_to(slug)
        
        # Actualizar configuración
        # Marcar mundo anterior como inactivo
        for world_config in self.config['worlds']:
            if world_config['status'] == 'active':
                world_config['status'] = 'inactive'
        
        # Marcar nuevo mundo como activo
        world_config_found = False
        for world_config in self.config['worlds']:
            if world_config['slug'] == slug:
                world_config['status'] = 'active'
                world_config_found = True
                break
        
        # Si no existe en config, agregarlo
        if not world_config_found:
            self.config['worlds'].append({
                "slug": slug,
                "status": "active",
                "auto_backup": True,
                "backup_interval": "6h"
            })
        
        self.config['active_world'] = slug
        self._save_config()
        
        # Actualizar metadata del mundo
        target_world.update_last_played()
        
        return True
    
    def duplicate_world(self, source_slug, new_name):
        """
        Duplicar un mundo existente
        
        Args:
            source_slug: Slug del mundo a duplicar
            new_name: Nombre para el nuevo mundo
            
        Returns:
            World: Nuevo mundo creado
        """
        source_world = self.get_world(source_slug)
        if source_world is None:
            raise FileNotFoundError(f"Mundo {source_slug} no encontrado")
        
        # Generar nuevo slug
        new_slug = new_name.lower().replace(' ', '-').replace('_', '-')
        new_slug = ''.join(c for c in new_slug if c.isalnum() or c == '-')
        
        # Verificar que no exista
        if self.get_world(new_slug) is not None:
            raise ValueError(f"Ya existe un mundo con el slug: {new_slug}")
        
        # Copiar directorio completo
        new_world_path = self.worlds_base_path / new_slug
        shutil.copytree(source_world.path, new_world_path)

        # Asegurar estructura de dimensiones requerida
        self._ensure_world_structure(new_world_path)
        
        # Actualizar metadata
        new_world = World(new_slug, str(self.worlds_base_path))
        new_world.metadata['name'] = new_name
        new_world.metadata['slug'] = new_slug
        new_world.metadata['description'] = f"Copia de {source_world.metadata['name']}"
        new_world.metadata['created_at'] = datetime.utcnow().isoformat() + "Z"
        new_world.metadata['last_played'] = None
        new_world.save_metadata()
        
        # Agregar a configuración
        self.config['worlds'].append({
            "slug": new_slug,
            "status": "inactive",
            "auto_backup": True,
            "backup_interval": "6h"
        })
        self._save_config()
        
        return new_world
    
    def get_worlds_summary(self):
        """
        Obtener resumen de todos los mundos
        
        Returns:
            dict: Resumen con mundos y mundo activo
        """
        worlds = self.list_worlds()
        active_world = self.get_active_world()
        
        worlds_data = []
        for world in worlds:
            world_dict = world.to_dict()
            world_dict['status'] = 'active' if (active_world and world.slug == active_world.slug) else 'inactive'
            worlds_data.append(world_dict)
        
        return {
            'worlds': worlds_data,
            'active_world': active_world.slug if active_world else None,
            'total_worlds': len(worlds),
            'max_worlds': self.config['settings'].get('max_worlds', 10)
        }
