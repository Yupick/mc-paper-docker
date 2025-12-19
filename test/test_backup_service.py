#!/usr/bin/env python3
"""
Script de verificación del sistema de backups
Ejecutar con: python3 test_backup_service.py
"""
import sys
import os

# Agregar el directorio web al path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'web'))

from services.backup_service import BackupService

def test_backup_service():
    """Probar funcionalidad básica del BackupService"""
    
    print("=" * 60)
    print("PRUEBA DEL SISTEMA DE BACKUPS")
    print("=" * 60)
    
    # Rutas de prueba
    worlds_path = "./worlds"
    backups_path = "./backups/worlds"
    
    print(f"\n📁 Rutas:")
    print(f"   Mundos: {worlds_path}")
    print(f"   Backups: {backups_path}")
    
    # Crear instancia del servicio
    print("\n🔧 Inicializando BackupService...")
    try:
        service = BackupService(worlds_path, backups_path)
        print("   ✅ BackupService inicializado correctamente")
    except Exception as e:
        print(f"   ❌ Error al inicializar: {e}")
        return False
    
    # Verificar directorio de backups
    print("\n📂 Verificando estructura de directorios...")
    if os.path.exists(backups_path):
        print(f"   ✅ Directorio de backups existe: {backups_path}")
    else:
        print(f"   ❌ Directorio de backups NO existe: {backups_path}")
        return False
    
    # Verificar metadata
    print("\n📋 Verificando metadata de backups...")
    metadata = service.metadata
    print(f"   Backups registrados: {len(metadata.get('backups', []))}")
    
    if len(metadata.get('backups', [])) > 0:
        print("\n   Backups existentes:")
        for backup in metadata['backups'][:5]:  # Mostrar solo los primeros 5
            print(f"   - {backup['filename']}")
            print(f"     Mundo: {backup['world_slug']}")
            print(f"     Tamaño: {backup['size_mb']} MB")
            print(f"     Tipo: {backup['type']}")
            print(f"     Creado: {backup['created_at']}")
            print()
    else:
        print("   ℹ️  No hay backups registrados aún")
    
    # Verificar métodos disponibles
    print("\n🔍 Métodos disponibles:")
    methods = [
        'create_backup',
        'restore_backup',
        'list_backups',
        'delete_backup',
        'get_backup_info',
        'get_total_backup_size'
    ]
    
    for method in methods:
        if hasattr(service, method):
            print(f"   ✅ {method}")
        else:
            print(f"   ❌ {method} NO ENCONTRADO")
    
    # Probar listado de backups
    print("\n📊 Probando listado de backups...")
    try:
        all_backups = service.list_backups()
        print(f"   ✅ Total de backups: {len(all_backups)}")
        
        # Agrupar por mundo
        worlds_with_backups = {}
        for backup in all_backups:
            world = backup['world_slug']
            if world not in worlds_with_backups:
                worlds_with_backups[world] = 0
            worlds_with_backups[world] += 1
        
        if worlds_with_backups:
            print("\n   Backups por mundo:")
            for world, count in worlds_with_backups.items():
                print(f"   - {world}: {count} backup(s)")
        
    except Exception as e:
        print(f"   ❌ Error al listar backups: {e}")
    
    # Calcular tamaño total
    print("\n💾 Calculando espacio utilizado...")
    try:
        size_info = service.get_total_backup_size()
        print(f"   Total de backups: {size_info['count']}")
        print(f"   Espacio utilizado: {size_info['total_mb']:.2f} MB")
        print(f"   Espacio utilizado: {size_info['total_bytes']:,} bytes")
    except Exception as e:
        print(f"   ❌ Error al calcular tamaño: {e}")
    
    print("\n" + "=" * 60)
    print("✅ VERIFICACIÓN COMPLETADA")
    print("=" * 60)
    
    return True

if __name__ == '__main__':
    try:
        success = test_backup_service()
        sys.exit(0 if success else 1)
    except Exception as e:
        print(f"\n❌ ERROR FATAL: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
