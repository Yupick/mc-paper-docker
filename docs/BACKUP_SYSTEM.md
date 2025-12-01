# Sistema de Backups Multi-Mundo

## 📋 Descripción

Sistema completo de gestión de backups para mundos de Minecraft, integrado con el panel de administración web.

## 🚀 Características

### Backups Automáticos
- Se crean automáticamente al cambiar de mundo (si está habilitado)
- Limpieza automática: mantiene los últimos 5 backups automáticos por mundo
- Compresión eficiente con `tar.gz`

### Backups Manuales
- Crear backups bajo demanda desde el panel web
- Agregar descripción personalizada
- Sin límite de almacenamiento (solo automáticos se limpian)

### Gestión de Backups
- Listar todos los backups de un mundo específico
- Ver tamaño total y cantidad de backups
- Restaurar cualquier backup
- Eliminar backups específicos

## 📁 Estructura de Archivos

```
/backups/
└── worlds/
    ├── backups.json                    # Metadata de todos los backups
    ├── mundo-1_manual_20240101_120000.tar.gz
    ├── mundo-1_auto_20240102_080000.tar.gz
    ├── mundo-2_manual_20240103_150000.tar.gz
    └── ...
```

### Formato de backups.json

```json
{
  "backups": [
    {
      "id": 1,
      "filename": "mundo-survival_manual_20240101_120000.tar.gz",
      "world_slug": "mundo-survival",
      "created_at": "2024-01-01T12:00:00Z",
      "size_mb": 245.67,
      "size_bytes": 257588224,
      "type": "manual",
      "description": "Backup antes de actualizar plugins",
      "path": "/backups/worlds/mundo-survival_manual_20240101_120000.tar.gz"
    }
  ]
}
```

## 🔧 API Endpoints

### Listar Backups de un Mundo
```http
GET /api/worlds/<slug>/backups
```

**Respuesta:**
```json
{
  "success": true,
  "backups": [...],
  "total_size_mb": 1234.56,
  "total_count": 8
}
```

### Crear Backup Manual
```http
POST /api/worlds/<slug>/backup
Content-Type: application/json

{
  "description": "Descripción opcional del backup"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Backup creado correctamente",
  "backup": {
    "id": 5,
    "filename": "mundo-survival_manual_20240101_120000.tar.gz",
    "size_mb": 245.67,
    ...
  }
}
```

### Restaurar Backup
```http
POST /api/worlds/<slug>/restore
Content-Type: application/json

{
  "backup_filename": "mundo-survival_manual_20240101_120000.tar.gz"
}
```

**Notas:**
- El mundo no puede estar activo para ser restaurado
- Se crea un backup de seguridad antes de restaurar
- El backup de seguridad se nombra: `{slug}_before_restore_{timestamp}.tar.gz`

### Eliminar Backup
```http
DELETE /api/backups/<backup_filename>
```

## 🎮 Uso desde el Panel Web

### Crear Backup Manual

1. Ve a la sección **Mundos**
2. Haz clic en el menú de acciones (⋮) del mundo
3. Selecciona **Backup**
4. Se abrirá el modal de backups
5. Haz clic en **Crear Backup Ahora**
6. Ingresa una descripción (opcional)
7. El backup se creará y aparecerá en la lista

### Ver Backups de un Mundo

1. En la tarjeta del mundo, haz clic en **⋮ → Backup**
2. Se mostrará el modal con:
   - Estadísticas (cantidad y tamaño total)
   - Lista de todos los backups
   - Opciones para restaurar o eliminar

### Restaurar un Backup

1. Abre el modal de backups del mundo
2. Localiza el backup que deseas restaurar
3. Haz clic en **Restaurar**
4. Confirma la acción

**⚠️ ADVERTENCIA:** 
- El mundo será reemplazado completamente
- Se crea un backup de seguridad automáticamente
- No se puede restaurar un mundo que esté activo

### Eliminar un Backup

1. Abre el modal de backups
2. Localiza el backup a eliminar
3. Haz clic en el botón **🗑️**
4. Confirma la acción

## 🔄 Backups Automáticos al Cambiar Mundo

Cuando cambias de mundo activo con la opción de backup habilitada:

1. **Detener Servidor**: Se ejecuta `save-all` y `stop`
2. **Crear Backup**: Se comprime el mundo actual a `tar.gz`
3. **Cambiar Symlink**: Se actualiza `worlds/active`
4. **Reiniciar Servidor**: Se inicia con el nuevo mundo

El backup automático se guarda con:
- Tipo: `auto`
- Descripción: `"Backup automático antes de cambiar a [nuevo-mundo]"`
- Limpieza: Solo se mantienen los últimos 5 backups automáticos

## 🛠️ Clase BackupService

### Métodos Principales

#### `create_backup(world_slug, auto=False, description="")`
Crea un backup comprimido del mundo.

**Parámetros:**
- `world_slug`: Identificador del mundo
- `auto`: Si es automático (activa limpieza)
- `description`: Descripción personalizada

**Retorna:** Diccionario con información del backup

#### `restore_backup(backup_filename, target_world_slug=None)`
Restaura un mundo desde un backup.

**Parámetros:**
- `backup_filename`: Nombre del archivo de backup
- `target_world_slug`: Slug destino (opcional)

**Retorna:** `True` si fue exitoso

#### `list_backups(world_slug=None)`
Lista backups disponibles.

**Parámetros:**
- `world_slug`: Filtrar por mundo (opcional)

**Retorna:** Lista de backups ordenados por fecha

#### `delete_backup(backup_filename)`
Elimina un backup específico.

#### `get_total_backup_size(world_slug=None)`
Calcula el tamaño total de backups.

**Retorna:**
```python
{
  "total_bytes": 1234567890,
  "total_mb": 1177.38,
  "count": 8
}
```

## 📊 Ejemplos de Uso

### Ejemplo 1: Backup Manual con Descripción
```javascript
// Desde dashboard.js
const response = await fetch('/api/worlds/mundo-survival/backup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        description: 'Antes de actualizar a Paper 1.20.4'
    })
});
```

### Ejemplo 2: Restaurar Backup Específico
```javascript
const response = await fetch('/api/worlds/mundo-survival/restore', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        backup_filename: 'mundo-survival_manual_20240101_120000.tar.gz'
    })
});
```

### Ejemplo 3: Listar Todos los Backups
```javascript
const response = await fetch('/api/worlds/mundo-survival/backups');
const data = await response.json();

console.log(`Total: ${data.total_count} backups`);
console.log(`Tamaño: ${data.total_size_mb} MB`);
data.backups.forEach(b => console.log(`- ${b.description} (${b.size_mb} MB)`));
```

## 🔐 Seguridad

- Todos los endpoints requieren autenticación (`@login_required`)
- Validación de existencia de mundos y backups
- Backups de seguridad antes de restaurar
- Confirmaciones dobles en operaciones destructivas

## ⚡ Rendimiento

- Compresión `gzip` para reducir tamaño (~60-80% reducción)
- Limpieza automática de backups antiguos
- Uso de `tar` nativo para máxima velocidad
- Metadata en JSON para consultas rápidas

## 🐛 Troubleshooting

### "Error al crear backup: Permission denied"
- Verifica permisos del directorio `/backups/`
- Ejecuta: `chmod -R 755 /backups`

### "Backup no encontrado"
- El archivo puede haber sido eliminado manualmente
- Verifica que `backups.json` esté sincronizado

### "Error al restaurar: Mundo activo"
- No se puede restaurar el mundo actualmente activo
- Cambia a otro mundo primero

### Backup muy lento
- Mundos grandes (>5GB) pueden tardar varios minutos
- Considera limpiar archivos innecesarios del mundo
- Verifica espacio en disco disponible

## 📝 Notas Técnicas

- Los backups se almacenan fuera del contenedor Docker
- Formato de nombre: `{slug}_{tipo}_{timestamp}.tar.gz`
- Timestamp formato: `YYYYMMDD_HHMMSS`
- Los backups manuales nunca se eliminan automáticamente
- Solo los backups automáticos se limpian (últimos 5)

## 🔮 Mejoras Futuras

- [ ] Backup programado (cronjob)
- [ ] Subir backups a cloud storage (S3, Google Drive)
- [ ] Verificación de integridad de backups
- [ ] Compresión incremental
- [ ] Cifrado de backups
- [ ] Exportar/importar backups entre servidores
