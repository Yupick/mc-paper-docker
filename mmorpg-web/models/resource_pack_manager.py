"""
resource_pack_manager.py - Gestión de Resource Packs para el servidor
Permite configurar y actualizar resource packs en server.properties
"""
import os
import hashlib
import re
import json
from pathlib import Path


class ResourcePackManager:
    def __init__(self, base_path):
        """
        Inicializa el gestor de resource packs
        
        Args:
            base_path: Ruta base del proyecto (donde está config/)
        """
        self.base_path = Path(base_path)
        self.config_path = self.base_path / "config"
        self.server_properties_path = self.config_path / "server.properties"
        self.resource_packs_dir = self.base_path / "resource-packs"
        
        # Crear directorio de resource packs si no existe
        self.resource_packs_dir.mkdir(exist_ok=True)
    
    def get_current_config(self):
        """
        Lee la configuración actual de resource pack desde server.properties
        
        Returns:
            dict con las propiedades actuales
        """
        config = {
            'resource-pack': '',
            'resource-pack-sha1': '',
            'require-resource-pack': 'false',
            'resource-pack-prompt': ''
        }
        
        if not self.server_properties_path.exists():
            return config
        
        with open(self.server_properties_path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    if '=' in line:
                        key, value = line.split('=', 1)
                        key = key.strip()
                        if key in config:
                            config[key] = value.strip()
        
        return config
    
    def update_config(self, resource_pack_url=None, sha1_hash=None, 
                     require=None, prompt=None):
        """
        Actualiza la configuración de resource pack en server.properties
        
        Args:
            resource_pack_url: URL del resource pack (opcional)
            sha1_hash: Hash SHA-1 del archivo (opcional)
            require: Si el resource pack es obligatorio (opcional)
            prompt: Mensaje opcional para el jugador (opcional)
        
        Returns:
            dict con success y message
        """
        try:
            if not self.server_properties_path.exists():
                return {
                    'success': False,
                    'error': 'server.properties no encontrado'
                }
            
            # Leer todo el archivo
            with open(self.server_properties_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            # Actualizar líneas
            updated_lines = []
            keys_found = set()
            
            for line in lines:
                stripped = line.strip()
                
                # Línea vacía o comentario
                if not stripped or stripped.startswith('#'):
                    updated_lines.append(line)
                    continue
                
                # Línea con propiedad
                if '=' in stripped:
                    key, current_value = stripped.split('=', 1)
                    key = key.strip()
                    
                    # Actualizar si corresponde
                    if key == 'resource-pack' and resource_pack_url is not None:
                        updated_lines.append(f'resource-pack={resource_pack_url}\n')
                        keys_found.add(key)
                    elif key == 'resource-pack-sha1' and sha1_hash is not None:
                        updated_lines.append(f'resource-pack-sha1={sha1_hash}\n')
                        keys_found.add(key)
                    elif key == 'require-resource-pack' and require is not None:
                        value = 'true' if require else 'false'
                        updated_lines.append(f'require-resource-pack={value}\n')
                        keys_found.add(key)
                    elif key == 'resource-pack-prompt' and prompt is not None:
                        updated_lines.append(f'resource-pack-prompt={prompt}\n')
                        keys_found.add(key)
                    else:
                        updated_lines.append(line)
                else:
                    updated_lines.append(line)
            
            # Agregar propiedades que no existían
            if resource_pack_url is not None and 'resource-pack' not in keys_found:
                updated_lines.append(f'resource-pack={resource_pack_url}\n')
            if sha1_hash is not None and 'resource-pack-sha1' not in keys_found:
                updated_lines.append(f'resource-pack-sha1={sha1_hash}\n')
            if require is not None and 'require-resource-pack' not in keys_found:
                value = 'true' if require else 'false'
                updated_lines.append(f'require-resource-pack={value}\n')
            if prompt is not None and 'resource-pack-prompt' not in keys_found:
                updated_lines.append(f'resource-pack-prompt={prompt}\n')
            
            # Escribir archivo actualizado
            with open(self.server_properties_path, 'w', encoding='utf-8') as f:
                f.writelines(updated_lines)
            
            return {
                'success': True,
                'message': 'Configuración de resource pack actualizada correctamente'
            }
        
        except Exception as e:
            return {
                'success': False,
                'error': f'Error al actualizar server.properties: {str(e)}'
            }
    
    def calculate_sha1(self, file_path):
        """
        Calcula el hash SHA-1 de un archivo
        
        Args:
            file_path: Ruta al archivo
        
        Returns:
            string con el hash SHA-1 en hexadecimal
        """
        sha1 = hashlib.sha1()
        
        with open(file_path, 'rb') as f:
            while chunk := f.read(8192):
                sha1.update(chunk)
        
        return sha1.hexdigest()
    
    def save_resource_pack(self, file_data, filename):
        """
        Guarda un resource pack subido en el directorio local
        
        Args:
            file_data: Bytes del archivo
            filename: Nombre del archivo
        
        Returns:
            dict con success, path y sha1
        """
        try:
            # Validar extensión
            if not filename.endswith('.zip'):
                return {
                    'success': False,
                    'error': 'El resource pack debe ser un archivo .zip'
                }
            
            # Guardar archivo
            pack_path = self.resource_packs_dir / filename
            with open(pack_path, 'wb') as f:
                f.write(file_data)
            
            # Calcular SHA-1
            sha1_hash = self.calculate_sha1(pack_path)
            
            return {
                'success': True,
                'path': str(pack_path),
                'sha1': sha1_hash,
                'message': f'Resource pack guardado: {filename}'
            }
        
        except Exception as e:
            return {
                'success': False,
                'error': f'Error al guardar resource pack: {str(e)}'
            }
    
    def list_local_packs(self):
        """
        Lista los resource packs almacenados localmente
        
        Returns:
            list de dicts con información de cada pack
        """
        packs = []
        
        if not self.resource_packs_dir.exists():
            return packs
        
        for pack_file in self.resource_packs_dir.glob('*.zip'):
            try:
                size_mb = pack_file.stat().st_size / (1024 * 1024)
                sha1_hash = self.calculate_sha1(pack_file)
                
                packs.append({
                    'filename': pack_file.name,
                    'size_mb': round(size_mb, 2),
                    'sha1': sha1_hash,
                    'path': str(pack_file)
                })
            except Exception as e:
                print(f"Error al procesar {pack_file.name}: {e}")
        
        return packs
    
    def delete_pack(self, filename):
        """
        Elimina un resource pack local
        
        Args:
            filename: Nombre del archivo a eliminar
        
        Returns:
            dict con success y message
        """
        try:
            pack_path = self.resource_packs_dir / filename
            
            if not pack_path.exists():
                return {
                    'success': False,
                    'error': 'Resource pack no encontrado'
                }
            
            pack_path.unlink()
            
            return {
                'success': True,
                'message': f'Resource pack eliminado: {filename}'
            }
        
        except Exception as e:
            return {
                'success': False,
                'error': f'Error al eliminar resource pack: {str(e)}'
            }
