# Configuración de Backups Automáticos

## ✅ Implementado

Se ha agregado un sistema completo de configuración para activar/desactivar los backups automáticos desde el panel web.

## 🎛️ Ubicación en el Panel

La configuración se encuentra en:
```
Panel Web → Sección "Backups" → Card "Configuración de Backups"
```

## 🔧 Características

### 1. Toggle de Activación/Desactivación
- **Switch visual** de tamaño grande (3rem)
- **Badge de estado** que muestra:
  - 🟢 Verde "Activado" cuando está habilitado
  - ⚫ Gris "Desactivado" cuando está deshabilitado
- **Persistencia**: La configuración se guarda en `config/backup_config.json`

### 2. Control de Retención
- **Select dropdown** con opciones:
  - 3 backups
  - 5 backups (predeterminado)
  - 10 backups
  - 15 backups
- Solo afecta a backups **automáticos**
- Los backups **manuales** nunca se eliminan automáticamente

### 3. Descripción Clara
- Texto explicativo sobre cuándo se crean los backups
- Información sobre la política de limpieza

## 📁 Archivo de Configuración

**Ubicación**: `config/backup_config.json`

**Formato**:
```json
{
  "auto_backup_enabled": true,
  "retention_count": 5
}
```

**Campos**:
- `auto_backup_enabled` (boolean): Activa/desactiva backups automáticos
- `retention_count` (integer): Número de backups automáticos a conservar (1-50)

## 🔌 Endpoints API

### GET /api/backup-config
Obtener configuración actual de backups.

**Respuesta**:
```json
{
  "success": true,
  "config": {
    "auto_backup_enabled": true,
    "retention_count": 5
  }
}
```

### PUT /api/backup-config
Actualizar configuración de backups.

**Body**:
```json
{
  "auto_backup_enabled": false,
  "retention_count": 10
}
```

**Respuesta**:
```json
{
  "success": true,
  "message": "Configuración actualizada",
  "config": {
    "auto_backup_enabled": false,
    "retention_count": 10
  }
}
```

**Validaciones**:
- `retention_count` debe estar entre 1 y 50
- Si no existe el archivo, se crea con valores predeterminados

## 🔄 Integración con Cambio de Mundo

### Comportamiento Actual

Cuando se activa un mundo diferente:

1. **Se lee la configuración** de `config/backup_config.json`
2. **Si `auto_backup_enabled` = true**:
   - Se crea backup del mundo actual
   - Descripción: "Backup automático antes de cambiar a [nombre-mundo-nuevo]"
   - Se ejecuta limpieza según `retention_count`
3. **Si `auto_backup_enabled` = false**:
   - NO se crea backup automático
   - El usuario puede seguir marcando el checkbox en el modal de confirmación

### Modal de Confirmación

El checkbox "Crear backup antes de cambiar" en el modal de confirmación:
- Se **precarga** con el valor de `auto_backup_enabled`
- El usuario puede **modificarlo** para ese cambio específico
- No afecta la configuración guardada

## 💡 Funciones JavaScript

### loadBackupConfig()
```javascript
async function loadBackupConfig()
```
- Carga configuración desde `/api/backup-config`
- Actualiza toggle y select
- Se llama automáticamente al mostrar sección de backups

### toggleAutoBackup()
```javascript
async function toggleAutoBackup()
```
- Maneja el cambio del switch
- Actualiza configuración vía PUT
- Muestra notificación de éxito/error
- Revierte toggle si falla

### updateBackupRetention()
```javascript
async function updateBackupRetention()
```
- Maneja cambio en select de retención
- Actualiza configuración vía PUT
- Muestra notificación confirmando cambio

### activateWorld(slug, name)
```javascript
async function activateWorld(slug, name)
```
- **Modificado** para cargar configuración
- Precarga checkbox con `auto_backup_enabled`
- Muestra modal de confirmación

## 🎨 Diseño Visual

### Card de Configuración
```html
<div class="card mb-3">
    <div class="card-header">
        <i class="bi bi-gear"></i> Configuración de Backups
    </div>
    <div class="card-body">
        <!-- Toggle + Descripción -->
        <!-- Select de Retención -->
    </div>
</div>
```

**Características**:
- Tema oscuro coherente (`#242837`)
- Switch de 3rem x 1.5rem (fácil de usar)
- Badge de estado visualmente claro
- Separador horizontal entre secciones
- Layout responsive (col-md-8 / col-md-4)

## 📊 Flujo de Datos

```
Usuario cambia toggle
    ↓
toggleAutoBackup()
    ↓
PUT /api/backup-config
    ↓
Actualiza config/backup_config.json
    ↓
Respuesta con nuevo estado
    ↓
Actualiza badge en UI
    ↓
Muestra notificación
```

## 🧪 Testing

El script `run-tests.sh` verifica:
- ✅ Existencia de `config/backup_config.json`
- ✅ Valores de configuración son válidos
- ✅ Funciones JavaScript existen
- ✅ Endpoints API están implementados

## 🔐 Seguridad

- Todos los endpoints requieren `@login_required`
- Validación de rangos (retention_count: 1-50)
- Manejo de errores robusto
- Creación automática de archivo si no existe

## 📝 Valores Predeterminados

Si no existe `config/backup_config.json`, se crea con:
```json
{
  "auto_backup_enabled": true,
  "retention_count": 5
}
```

**Razón**: Mejor tener backups por defecto para proteger datos.

## 🚀 Ejemplos de Uso

### Desactivar Backups Automáticos

1. Ir a sección "Backups"
2. Desactivar el switch
3. Confirmación: "Backups automáticos desactivados"
4. Ahora los cambios de mundo NO crearán backups automáticos

### Aumentar Retención

1. Cambiar select a "10 backups"
2. Confirmación: "Se conservarán los últimos 10 backups automáticos"
3. La próxima limpieza automática conservará 10 backups

### Uso Mixto

- Backups automáticos: **Desactivados** en configuración
- Al cambiar mundo: Usuario **marca** checkbox manualmente
- Resultado: Se crea backup solo para ese cambio específico

## 🔮 Futuras Mejoras

- [ ] Programar backups automáticos periódicos (cron)
- [ ] Configurar diferentes retenciones por mundo
- [ ] Notificaciones cuando se eliminan backups antiguos
- [ ] Estadísticas de backups en dashboard
- [ ] Exportar configuración a otros servidores
