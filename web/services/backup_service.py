"""
Servicio de gestión de backups de mundos
"""
import os
import json
import subprocess
import shutil
from datetime import datetime
from pathlib import Path


class BackupService:
    """Gestiona backups de mundos de Minecraft"""
    
    def __init__(self, worlds_base_path="/server/worlds", backups_base_path="/backups/worlds"):
        """
        Inicializar servicio de backups
        
        Args:
            worlds_base_path: Ruta base donde se almacenan los mundos
            backups_base_path: Ruta base donde se guardan los backups
        """
        self.worlds_base_path = Path(worlds_base_path)
        self.backups_base_path = Path(backups_base_path)
        self.backups_metadata_file = self.backups_base_path / "backups.json"
        
        # Asegurar que existen los directorios
        self.backups_base_path.mkdir(parents=True, exist_ok=True)
        
        # Cargar metadata de backups
        self.metadata = self._load_metadata()
    
    def _load_metadata(self):
        """Cargar metadata de backups desde backups.json"""
        if not self.backups_metadata_file.exists():
            return {"backups": []}
        
        try:
            with open(self.backups_metadata_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except (json.JSONDecodeError, IOError):
            return {"backups": []}
    
    def _save_metadata(self):
        """Guardar metadata de backups a disco"""
        with open(self.backups_metadata_file, 'w', encoding='utf-8') as f:
            json.dump(self.metadata, f, indent=2, ensure_ascii=False)
    
    def create_backup(self, world_slug, auto=False, description=""):
        """
        Crear backup de un mundo
        
        Args:
            world_slug: Slug del mundo a respaldar
            auto: Si es un backup automático
            description: Descripción del backup
            
        Returns:
            dict: Información del backup creado
        """
        world_path = self.worlds_base_path / world_slug
        
        if not world_path.exists():
            raise FileNotFoundError(f"Mundo {world_slug} no encontrado")
        
        # Generar nombre del backup
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        backup_type = "auto" if auto else "manual"
        backup_name = f"{world_slug}_{backup_type}_{timestamp}.tar.gz"
        backup_path = self.backups_base_path / backup_name
        
        # Comprimir mundo
        try:
            # Usar tar para crear el backup
            # -czf: crear, comprimir con gzip, archivo
            # -C: cambiar al directorio antes de comprimir
            result = subprocess.run(
                [
                    'tar', '-czf', str(backup_path),
                    '-C', str(self.worlds_base_path),
                    world_slug
                ],
                capture_output=True,
                text=True,
                check=True
            )
            
            # Obtener tamaño del backup
            backup_size_bytes = backup_path.stat().st_size
            backup_size_mb = round(backup_size_bytes / (1024 * 1024), 2)
            
            # Crear metadata del backup
            backup_info = {
                "id": len(self.metadata["backups"]) + 1,
                "filename": backup_name,
                "world_slug": world_slug,
                "created_at": datetime.now().isoformat() + "Z",
                "size_mb": backup_size_mb,
                "size_bytes": backup_size_bytes,
                "type": backup_type,
                "description": description or ("Backup automático" if auto else "Backup manual"),
                "path": str(backup_path)
            }
            
            # Agregar a metadata
            self.metadata["backups"].append(backup_info)
            self._save_metadata()
            
            # Limpiar backups antiguos si es automático
            if auto:
                # Leer configuración de retención
                import os
                config_file = os.path.join(os.path.dirname(os.path.dirname(__file__)), '../config/backup_config.json')
                retention_count = 5  # Default
                try:
                    if os.path.exists(config_file):
                        import json
                        with open(config_file, 'r') as f:
                            config = json.load(f)
                            retention_count = config.get('retention_count', 5)
                except:
                    pass
                
                self._cleanup_old_backups(world_slug, keep_count=retention_count)
            
            return backup_info
            
        except subprocess.CalledProcessError as e:
            raise IOError(f"Error al crear backup: {e.stderr}")
    
    def restore_backup(self, backup_filename, target_world_slug=None):
        """
        Restaurar un backup
        
        Args:
            backup_filename: Nombre del archivo de backup
            target_world_slug: Slug del mundo destino (opcional, usa el original si no se especifica)
            
        Returns:
            bool: True si se restauró correctamente
        """
        backup_path = self.backups_base_path / backup_filename
        
        if not backup_path.exists():
            raise FileNotFoundError(f"Backup {backup_filename} no encontrado")
        
        # Buscar metadata del backup
        backup_info = None
        for backup in self.metadata["backups"]:
            if backup["filename"] == backup_filename:
                backup_info = backup
                break
        
        if not backup_info:
            raise ValueError(f"Metadata del backup {backup_filename} no encontrada")
        
        # Determinar slug destino
        world_slug = target_world_slug or backup_info["world_slug"]
        world_path = self.worlds_base_path / world_slug
        
        # Si el mundo existe, crear backup de seguridad primero
        if world_path.exists():
            security_backup_name = f"{world_slug}_before_restore_{datetime.now().strftime('%Y%m%d_%H%M%S')}.tar.gz"
            security_backup_path = self.backups_base_path / security_backup_name
            
            subprocess.run(
                ['tar', '-czf', str(security_backup_path), '-C', str(self.worlds_base_path), world_slug],
                check=True
            )
            
            # Eliminar mundo actual
            shutil.rmtree(world_path)
        
        # Extraer backup
        try:
            # -xzf: extraer, descomprimir con gzip, archivo
            # -C: extraer en este directorio
            subprocess.run(
                ['tar', '-xzf', str(backup_path), '-C', str(self.worlds_base_path)],
                capture_output=True,
                text=True,
                check=True
            )
            
            # Si se especificó un slug diferente, renombrar
            if target_world_slug and target_world_slug != backup_info["world_slug"]:
                original_path = self.worlds_base_path / backup_info["world_slug"]
                if original_path.exists():
                    original_path.rename(world_path)
            
            return True
            
        except subprocess.CalledProcessError as e:
            raise IOError(f"Error al restaurar backup: {e.stderr}")
    
    def list_backups(self, world_slug=None):
        """
        Listar backups disponibles
        
        Args:
            world_slug: Filtrar por mundo específico (opcional)
            
        Returns:
            list: Lista de backups
        """
        backups = self.metadata["backups"]
        
        if world_slug:
            backups = [b for b in backups if b["world_slug"] == world_slug]
        
        # Ordenar por fecha (más reciente primero)
        backups.sort(key=lambda x: x["created_at"], reverse=True)
        
        return backups
    
    def delete_backup(self, backup_filename):
        """
        Eliminar un backup
        
        Args:
            backup_filename: Nombre del archivo de backup
            
        Returns:
            bool: True si se eliminó correctamente
        """
        backup_path = self.backups_base_path / backup_filename
        
        if backup_path.exists():
            backup_path.unlink()
        
        # Eliminar de metadata
        self.metadata["backups"] = [
            b for b in self.metadata["backups"] 
            if b["filename"] != backup_filename
        ]
        self._save_metadata()
        
        return True
    
    def _cleanup_old_backups(self, world_slug, keep_count=5):
        """
        Limpiar backups antiguos, manteniendo solo los últimos N
        
        Args:
            world_slug: Slug del mundo
            keep_count: Número de backups a mantener
        """
        # Obtener backups automáticos del mundo
        auto_backups = [
            b for b in self.metadata["backups"]
            if b["world_slug"] == world_slug and b["type"] == "auto"
        ]
        
        # Ordenar por fecha (más antiguos primero)
        auto_backups.sort(key=lambda x: x["created_at"])
        
        # Si hay más de keep_count, eliminar los más antiguos
        if len(auto_backups) > keep_count:
            to_delete = auto_backups[:-keep_count]
            
            for backup in to_delete:
                backup_path = self.backups_base_path / backup["filename"]
                if backup_path.exists():
                    backup_path.unlink()
                
                # Eliminar de metadata
                self.metadata["backups"] = [
                    b for b in self.metadata["backups"]
                    if b["filename"] != backup["filename"]
                ]
            
            self._save_metadata()
    
    def get_backup_info(self, backup_filename):
        """
        Obtener información de un backup específico
        
        Args:
            backup_filename: Nombre del archivo de backup
            
        Returns:
            dict: Información del backup o None
        """
        for backup in self.metadata["backups"]:
            if backup["filename"] == backup_filename:
                return backup
        
        return None
    
    def get_total_backup_size(self, world_slug=None):
        """
        Calcular tamaño total de backups
        
        Args:
            world_slug: Filtrar por mundo (opcional)
            
        Returns:
            dict: Tamaño total en bytes y MB
        """
        backups = self.list_backups(world_slug)
        
        total_bytes = sum(b.get("size_bytes", 0) for b in backups)
        total_mb = round(total_bytes / (1024 * 1024), 2)
        
        return {
            "total_bytes": total_bytes,
            "total_mb": total_mb,
            "count": len(backups)
        }
