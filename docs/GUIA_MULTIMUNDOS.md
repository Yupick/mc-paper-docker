# 🌍 Guía Completa del Sistema Multi-Mundo

## 📑 Tabla de Contenidos

- [Introducción](#-introducción)
- [Conceptos Fundamentales](#-conceptos-fundamentales)
- [Instalación y Migración](#-instalación-y-migración)
- [Crear tu Primer Mundo](#-crear-tu-primer-mundo)
- [Gestionar Mundos](#-gestionar-mundos)
- [Sistema de Backups](#-sistema-de-backups)
- [Cambiar Entre Mundos](#-cambiar-entre-mundos)
- [Configuración Avanzada](#-configuración-avanzada)
- [Resolución de Problemas](#-resolución-de-problemas)
- [Preguntas Frecuentes](#-preguntas-frecuentes)

---

## 🎯 Introducción

El **Sistema Multi-Mundo** te permite gestionar múltiples mundos de Minecraft en un solo servidor, cada uno con su propia configuración independiente. Puedes cambiar entre mundos en caliente (sin detener el servidor) y mantener backups individuales de cada mundo.

### ¿Por qué usar Multi-Mundo?

✅ **Flexibilidad Total**
- Un mundo survival, otro creative, otro mini-games
- Configuración independiente por mundo (dificultad, PVP, gamemode)
- No necesitas múltiples servidores

✅ **Gestión Simplificada**
- Cambio entre mundos con un click
- Backups automáticos al cambiar
- Panel web intuitivo

✅ **Eficiencia de Recursos**
- Un solo servidor ejecuta todos los mundos
- Solo un mundo activo a la vez
- Ahorro de RAM y CPU

### Casos de Uso Comunes

1. **Servidor Multi-Modalidad**
   - `survival-vanilla` → Survival puro
   - `creative-builds` → Mundo creativo para construcciones
   - `minigames` → Mundo para eventos y mini-juegos

2. **Testing y Desarrollo**
   - `production` → Mundo principal de los jugadores
   - `testing` → Mundo para probar plugins/builds
   - `backup-safe` → Copia de seguridad del mundo principal

3. **Eventos Temporales**
   - `main-world` → Mundo principal permanente
   - `halloween-2025` → Evento de Halloween
   - `xmas-2025` → Evento de Navidad

---

## 📚 Conceptos Fundamentales

### Arquitectura Symlink

El sistema multi-mundo utiliza **enlaces simbólicos (symlinks)** para activar mundos:

```
worlds/
├── active/              ← Symlink → worlds/world-default/
│   ├── world/           (Overworld del mundo activo)
│   ├── world_nether/    (Nether del mundo activo)
│   └── world_the_end/   (End del mundo activo)
├── world-default/       ← Mundo por defecto
│   ├── metadata.json
│   ├── world/
│   ├── world_nether/
│   └── world_the_end/
├── survival-hard/       ← Mundo personalizado
│   ├── metadata.json
│   ├── world/
│   ├── world_nether/
│   └── world_the_end/
└── creative-plots/      ← Otro mundo personalizado
    ├── metadata.json
    ├── world/
    ├── world_nether/
    └── world_the_end/
```

**Ventajas del sistema symlink:**
- ✅ Cambio instantáneo sin mover archivos
- ✅ Docker siempre apunta a `worlds/active/`
- ✅ Integridad de datos garantizada
- ✅ Reversible y seguro

### Estructura de un Mundo

Cada mundo contiene:

#### 1. Dimensiones
- `world/` → Overworld (mundo principal)
- `world_nether/` → Nether
- `world_the_end/` → The End

#### 2. Metadata (`metadata.json`)
```json
{
  "slug": "survival-hard",
  "name": "Survival Extremo",
  "description": "Modo survival con dificultad Hard",
  "created_at": "2025-11-30T18:30:00",
  "last_accessed": "2025-11-30T20:15:00",
  "gamemode": "survival",
  "difficulty": "hard",
  "pvp": true,
  "seed": "12345",
  "generator_settings": null
}
```

#### 3. Configuración (`server.properties`)
Cada mundo puede tener su propio `server.properties` con configuración independiente.

### Slug del Mundo

El **slug** es el identificador único del mundo en el sistema de archivos:

- ✅ Solo letras minúsculas, números y guiones
- ✅ Sin espacios ni caracteres especiales
- ✅ Ejemplo: `survival-hard`, `creative-2025`, `minigames-pvp`

**Ejemplos:**
- ❌ `Survival Hard` → Espacios no permitidos
- ❌ `Survival_Hard` → Guiones bajos no permitidos
- ✅ `survival-hard` → Correcto
- ✅ `survival-extreme-2025` → Correcto

---

## 🚀 Instalación y Migración

### Instalación Inicial (Nuevo Servidor)

Si estás instalando el servidor por primera vez, el sistema multi-mundo ya está incluido:

```bash
# 1. Clonar repositorio
git clone https://github.com/tu-usuario/mc-paper.git
cd mc-paper

# 2. Ejecutar instalación automática
chmod +x create.sh
./create.sh

# 3. Iniciar servicios
docker-compose up -d
cd web && ./start-web-panel.sh
```

El script `create.sh` automáticamente:
- ✅ Crea estructura `worlds/` con soporte multi-mundo
- ✅ Genera archivos de configuración necesarios
- ✅ Prepara sistema de backups por mundo

### Migración desde Servidor Existente

Si ya tienes un servidor funcionando con un solo mundo, usa el script de migración:

```bash
# 1. IMPORTANTE: Crear backup antes de migrar
./backup.sh

# 2. Ejecutar migración
chmod +x migrate-to-multiworld.sh
./migrate-to-multiworld.sh
```

#### ¿Qué hace el script de migración?

**Paso 1: Verificación**
```
✅ Verificar que Docker esté instalado
✅ Verificar que el contenedor exista
✅ Verificar directorio worlds/
✅ Verificar que no haya migración previa
```

**Paso 2: Backup de Seguridad**
```bash
# Crea backup timestamped en backups/
backup_pre-migration_20251130_183045.tar.gz
```

**Paso 3: Detener Servidor**
```bash
docker stop mc-paper
```

**Paso 4: Reestructuración**
```bash
# Mueve mundo actual a worlds/world-default/
mv worlds/world worlds/world-default/world
mv worlds/world_nether worlds/world-default/world_nether
mv worlds/world_the_end worlds/world-default/world_the_end
```

**Paso 5: Crear Symlink**
```bash
# Crea enlace simbólico
ln -s world-default worlds/active
```

**Paso 6: Generar Metadata**
```json
// worlds/world-default/metadata.json
{
  "slug": "world-default",
  "name": "Mundo Principal",
  "description": "Mundo original migrado",
  "created_at": "2025-11-30T18:30:00",
  "gamemode": "survival",
  "difficulty": "normal",
  "pvp": true
}
```

**Paso 7: Actualizar Docker**
```yaml
# docker-compose.yml - Actualiza volúmenes
volumes:
  - ./worlds/active/world:/data/world
  - ./worlds/active/world_nether:/data/world_nether
  - ./worlds/active/world_the_end:/data/world_the_end
```

**Paso 8: Reiniciar Servidor**
```bash
docker-compose up -d
```

#### Verificar Migración Exitosa

```bash
# Ver estructura
ls -la worlds/

# Salida esperada:
# drwxr-xr-x  active -> world-default
# drwxr-xr-x  world-default/

# Verificar symlink
readlink worlds/active
# Output: world-default

# Verificar metadata
cat worlds/world-default/metadata.json
```

#### Rollback (Revertir Migración)

Si algo sale mal, puedes revertir la migración:

```bash
chmod +x rollback-multiworld.sh
./rollback-multiworld.sh
```

**El script de rollback:**
1. Detiene el servidor
2. Elimina symlink `worlds/active/`
3. Mueve mundos de vuelta a `worlds/`
4. Restaura `docker-compose.yml` original
5. Reinicia servidor

---

## 🎨 Crear tu Primer Mundo

### Desde el Panel Web

#### Paso 1: Acceder a la Sección Mundos

1. Abrir panel web: `http://localhost:5000`
2. Login con credenciales
3. Click en **"🌍 Mundos"** en el menú lateral

#### Paso 2: Click en "Crear Mundo"

Verás un botón verde **"+ Crear Mundo"** en la parte superior.

#### Paso 3: Completar Formulario

**Modal "Crear Nuevo Mundo":**

```
┌─────────────────────────────────────────────┐
│ Crear Nuevo Mundo                        × │
├─────────────────────────────────────────────┤
│                                             │
│ Nombre del Mundo: *                        │
│ [Survival Extremo________________]          │
│                                             │
│ Slug (identificador): *                    │
│ [survival-extremo_______________]          │
│ ℹ️ Solo letras minúsculas, números y -     │
│                                             │
│ Descripción:                               │
│ [Modo supervivencia difícil_______]        │
│ [con PVP activado_________________]        │
│                                             │
│ Gamemode: *                                │
│ [▼ Survival]                               │
│   Options: Survival, Creative,             │
│            Adventure, Spectator            │
│                                             │
│ Dificultad: *                              │
│ [▼ Hard]                                   │
│   Options: Peaceful, Easy, Normal, Hard    │
│                                             │
│ PVP:                                       │
│ [✓] Activar PVP                            │
│                                             │
│ Semilla (Seed):                            │
│ [12345______________________]              │
│ ℹ️ Opcional - deja vacío para aleatoria   │
│                                             │
├─────────────────────────────────────────────┤
│        [Cancelar]  [Crear Mundo]           │
└─────────────────────────────────────────────┘
```

**Campos:**

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| **Nombre del Mundo** | Texto | Sí | Nombre amigable (puede tener espacios) |
| **Slug** | Texto | Sí | Identificador único (solo a-z, 0-9, -) |
| **Descripción** | Textarea | No | Descripción del mundo |
| **Gamemode** | Select | Sí | survival, creative, adventure, spectator |
| **Dificultad** | Select | Sí | peaceful, easy, normal, hard |
| **PVP** | Checkbox | No | Activar combate entre jugadores |
| **Seed** | Texto | No | Semilla del mundo (vacío = aleatorio) |

#### Paso 4: Validaciones

El sistema valida automáticamente:

✅ **Nombre del mundo:**
- No puede estar vacío
- Máximo 100 caracteres

✅ **Slug:**
- No puede estar vacío
- Solo `a-z`, `0-9`, `-` permitidos
- Máximo 50 caracteres
- Debe ser único (no puede existir otro mundo con el mismo slug)

✅ **Seed:**
- Solo números (si se proporciona)
- Opcional

❌ **Errores comunes:**
```
Error: El slug solo puede contener letras minúsculas, números y guiones
Error: Ya existe un mundo con el slug "survival-extremo"
Error: El nombre del mundo es requerido
```

#### Paso 5: Confirmación

Al hacer click en **"Crear Mundo"**:

1. ✅ Validación de datos
2. ✅ Creación de directorio `worlds/survival-extremo/`
3. ✅ Creación de subdirectorios (world/, world_nether/, world_the_end/)
4. ✅ Generación de `metadata.json`
5. ✅ Generación de `server.properties` personalizado
6. ✅ Mensaje de éxito: "Mundo creado exitosamente"
7. ✅ Recarga de grid de mundos

### Desde la API REST

```bash
curl -X POST http://localhost:5000/api/worlds \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "name": "Survival Extremo",
    "slug": "survival-extremo",
    "description": "Modo supervivencia con dificultad Hard y PVP",
    "gamemode": "survival",
    "difficulty": "hard",
    "pvp": true,
    "seed": "12345"
  }'
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Mundo creado exitosamente",
  "world": {
    "slug": "survival-extremo",
    "name": "Survival Extremo",
    "description": "Modo supervivencia con dificultad Hard y PVP",
    "active": false,
    "created_at": "2025-11-30T18:30:00",
    "size_mb": 0.5,
    "gamemode": "survival",
    "difficulty": "hard",
    "pvp": true,
    "seed": "12345"
  }
}
```

---

## 🔧 Gestionar Mundos

### Ver Lista de Mundos

#### Desde Panel Web

La sección **Mundos** muestra un grid responsive con tarjetas:

```
┌──────────────────────────────────────────────────────────────┐
│ 🌍 Gestión de Mundos                    [+ Crear Mundo]     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│ ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐│
│ │ world-default   │  │ survival-hard   │  │ creative-2025 ││
│ │                 │  │                 │  │               ││
│ │ 🟢 ACTIVO       │  │ Survival Hard   │  │ Creative 2025 ││
│ │                 │  │                 │  │               ││
│ │ Mundo Principal │  │ Modo survival   │  │ Builds libres ││
│ │ 512 MB          │  │ difícil con PVP │  │ 256 MB        ││
│ │                 │  │ 1.2 GB          │  │               ││
│ │ [Backups]       │  │                 │  │ [Activar]     ││
│ │ [Editar]        │  │ [Activar]       │  │ [Backups]     ││
│ │                 │  │ [Backups]       │  │ [Editar]      ││
│ │                 │  │ [Editar]        │  │ [Duplicar]    ││
│ │                 │  │ [Duplicar]      │  │ [Eliminar]    ││
│ │                 │  │ [Eliminar]      │  │               ││
│ └─────────────────┘  └─────────────────┘  └───────────────┘│
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Información de cada tarjeta:**
- 🟢 Badge verde si es el mundo activo
- Nombre y descripción
- Tamaño total (MB/GB)
- Fecha de creación y último acceso
- Botones de acción

#### Desde API REST

```bash
curl -X GET http://localhost:5000/api/worlds \
  -H "Cookie: session=tu_session_cookie"
```

**Respuesta:**
```json
{
  "worlds": [
    {
      "slug": "world-default",
      "name": "Mundo Principal",
      "description": "Mundo original del servidor",
      "active": true,
      "created_at": "2025-11-30T18:00:00",
      "last_accessed": "2025-11-30T20:15:00",
      "size_mb": 512.5,
      "gamemode": "survival",
      "difficulty": "normal",
      "pvp": true
    },
    {
      "slug": "survival-hard",
      "name": "Survival Hard",
      "description": "Modo survival difícil con PVP",
      "active": false,
      "created_at": "2025-11-30T19:00:00",
      "last_accessed": "2025-11-30T19:30:00",
      "size_mb": 1200.8,
      "gamemode": "survival",
      "difficulty": "hard",
      "pvp": true
    }
  ],
  "active_world": "world-default",
  "total_worlds": 2
}
```

### Duplicar un Mundo

#### Uso: Crear copia exacta de un mundo

**Desde Panel Web:**
1. Ir a tarjeta del mundo que quieres duplicar
2. Click en **"Duplicar"**
3. Aparece modal con nombre sugerido: `{mundo-original}-copy`
4. Editar nombre y slug si deseas
5. Click en **"Duplicar"**

**El proceso:**
- ✅ Copia completa de todas las dimensiones (overworld, nether, end)
- ✅ Copia de metadata.json
- ✅ Copia de server.properties
- ⏱️ Tiempo: depende del tamaño (puede tomar varios minutos)

**Desde API REST:**
```bash
curl -X POST http://localhost:5000/api/worlds/survival-hard/duplicate \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "new_slug": "survival-hard-backup",
    "new_name": "Survival Hard - Backup"
  }'
```

### Editar Configuración de un Mundo

#### Editar server.properties

**Desde Panel Web:**
1. Click en **"Editar"** en la tarjeta del mundo
2. Aparece modal con editor de `server.properties`
3. Modificar propiedades (gamemode, difficulty, max-players, etc.)
4. Click en **"Guardar Configuración"**

**⚠️ IMPORTANTE:**
- Los cambios **NO afectan el mundo activo** hasta que lo actives
- Si es el mundo activo, necesitas reiniciar el servidor para aplicar cambios

**Propiedades editables:**
```properties
# Configuración básica
gamemode=survival
difficulty=hard
pvp=true
max-players=20

# Configuración de mundo
spawn-protection=16
view-distance=10
simulation-distance=10

# Configuración avanzada
enable-command-block=false
spawn-monsters=true
spawn-animals=true
```

**Desde API REST:**
```bash
curl -X PUT http://localhost:5000/api/worlds/survival-hard/config \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "gamemode": "survival",
    "difficulty": "hard",
    "pvp": "true",
    "max-players": "30"
  }'
```

### Eliminar un Mundo

#### ⚠️ ADVERTENCIA: Acción irreversible

**Desde Panel Web:**
1. Click en **"Eliminar"** en la tarjeta del mundo
2. Aparece confirmación: "¿Estás seguro de eliminar el mundo '{nombre}'?"
3. Debes escribir el slug del mundo para confirmar
4. Click en **"Confirmar Eliminación"**

**Restricciones:**
- ❌ No puedes eliminar el mundo activo
- ✅ Solo puedes eliminar mundos inactivos

**El proceso:**
1. Verifica que el mundo no esté activo
2. Elimina directorio `worlds/{slug}/` completo
3. Elimina backups del mundo en `backups/worlds/{slug}/`
4. Actualiza lista de mundos

**Desde API REST:**
```bash
curl -X DELETE http://localhost:5000/api/worlds/survival-hard \
  -H "Cookie: session=tu_session_cookie"
```

---

## 💾 Sistema de Backups

### Backups Individuales por Mundo

Cada mundo tiene su propio sistema de backups independiente:

```
backups/
└── worlds/
    ├── world-default/
    │   ├── world-default_manual_20251130_180000.tar.gz
    │   ├── world-default_auto_20251130_183045.tar.gz
    │   └── world-default_auto_20251130_190000.tar.gz
    ├── survival-hard/
    │   ├── survival-hard_manual_20251130_190030.tar.gz
    │   └── survival-hard_auto_20251130_193000.tar.gz
    └── creative-2025/
        └── creative-2025_manual_20251130_200000.tar.gz
```

### Crear Backup Manual

**Desde Panel Web:**
1. Ir a tarjeta del mundo
2. Click en **"Backups"**
3. Modal "Backups de {mundo}"
4. Click en **"Crear Backup"**
5. Esperar confirmación (puede tomar 1-5 minutos)

**Nomenclatura:**
```
{slug}_manual_{timestamp}.tar.gz

Ejemplos:
survival-hard_manual_20251130_183045.tar.gz
creative-2025_manual_20251201_094500.tar.gz
```

**Contenido del backup:**
- ✅ `world/` (overworld completo)
- ✅ `world_nether/` (nether completo)
- ✅ `world_the_end/` (end completo)
- ✅ `metadata.json` (información del mundo)
- ✅ Compresión tar.gz para optimizar espacio

**Metadata del backup:**
```json
// backups/worlds/survival-hard/survival-hard_manual_20251130_183045.json
{
  "world_slug": "survival-hard",
  "backup_type": "manual",
  "created_at": "2025-11-30T18:30:45",
  "size_bytes": 1258291200,
  "size_mb": 1200.5,
  "filename": "survival-hard_manual_20251130_183045.tar.gz"
}
```

### Backups Automáticos

#### Configuración

**Desde Panel Web:**
1. Ir a sección **"💾 Backups"**
2. Card "Configuración de Backups Automáticos"
3. **Toggle "Auto-Backup":** ON/OFF
4. **Slider "Retención":** 1-50 backups

**Archivo de configuración:**
```json
// config/backup_config.json
{
  "auto_backup_enabled": true,
  "retention_count": 5
}
```

#### ¿Cuándo se crean backups automáticos?

Los backups automáticos se crean **al cambiar de mundo activo**:

**Escenario:**
1. Mundo activo actual: `world-default`
2. Usuario activa: `survival-hard`
3. Sistema automáticamente:
   - ✅ Crea backup de `world-default` (si auto_backup_enabled = true)
   - ✅ Cambia symlink a `survival-hard`
   - ✅ Reinicia servidor

**Nomenclatura:**
```
{slug}_auto_{timestamp}.tar.gz

Ejemplo:
world-default_auto_20251130_183045.tar.gz
```

#### Auto-Cleanup (Limpieza Automática)

**Funcionamiento:**
- Solo afecta backups **automáticos** (tipo: auto)
- Mantiene los últimos N backups (según `retention_count`)
- **Nunca** elimina backups manuales

**Ejemplo con retention_count = 5:**

```
Backups antes del cleanup:
1. world-default_auto_20251125_100000.tar.gz  ← Más antiguo
2. world-default_auto_20251126_100000.tar.gz
3. world-default_auto_20251127_100000.tar.gz
4. world-default_auto_20251128_100000.tar.gz
5. world-default_auto_20251129_100000.tar.gz
6. world-default_auto_20251130_100000.tar.gz  ← Más reciente
7. world-default_manual_20251130_150000.tar.gz ← Manual (no se toca)

Backups después del cleanup:
2. world-default_auto_20251126_100000.tar.gz
3. world-default_auto_20251127_100000.tar.gz
4. world-default_auto_20251128_100000.tar.gz
5. world-default_auto_20251129_100000.tar.gz
6. world-default_auto_20251130_100000.tar.gz  ← Solo últimos 5
7. world-default_manual_20251130_150000.tar.gz ← Intacto
```

### Restaurar Backup

#### ⚠️ ADVERTENCIA: Reemplaza el mundo actual

**Desde Panel Web:**
1. Click en **"Backups"** del mundo
2. Lista de backups disponibles
3. Click en **"Restore"** del backup deseado
4. Confirmación: "¿Restaurar este backup? Se creará un backup de seguridad antes."
5. Click en **"Confirmar"**

**El proceso:**
1. ✅ Crea backup de seguridad del estado actual
2. ✅ Detiene servidor (si es el mundo activo)
3. ✅ Elimina contenido actual del mundo
4. ✅ Extrae backup seleccionado
5. ✅ Reinicia servidor (si es el mundo activo)

**Backup pre-restauración:**
```
Nombre: {slug}_pre-restore_{timestamp}.tar.gz
Ejemplo: survival-hard_pre-restore_20251130_184500.tar.gz
Tipo: manual (nunca se elimina automáticamente)
```

**Desde API REST:**
```bash
curl -X POST http://localhost:5000/api/worlds/survival-hard/restore \
  -H "Content-Type: application/json" \
  -H "Cookie: session=tu_session_cookie" \
  -d '{
    "backup_filename": "survival-hard_manual_20251130_183045.tar.gz"
  }'
```

### Eliminar Backups

**Desde Panel Web:**
1. Modal "Backups de {mundo}"
2. Click en **"Delete"** del backup
3. Confirmación
4. Click en **"Confirmar"**

**Desde API REST:**
```bash
curl -X DELETE http://localhost:5000/api/backups/survival-hard_manual_20251130_183045.tar.gz \
  -H "Cookie: session=tu_session_cookie"
```

---

## 🔄 Cambiar Entre Mundos

### Activar un Mundo (Cambio en Caliente)

#### Desde Panel Web

**Método 1: Botón "Activar"**
1. Ir a tarjeta del mundo que deseas activar
2. Click en **"Activar"**
3. Aparece modal de confirmación:

```
┌─────────────────────────────────────────────┐
│ Confirmar Cambio de Mundo               × │
├─────────────────────────────────────────────┤
│                                             │
│ Mundo actual: world-default                 │
│ Nuevo mundo:  survival-hard                 │
│                                             │
│ ⚠️ ADVERTENCIA:                             │
│ • El servidor se reiniciará                 │
│ • Todos los jugadores serán desconectados   │
│ • Se creará backup automático (si habilitado)│
│                                             │
│ ¿Continuar con el cambio de mundo?          │
│                                             │
├─────────────────────────────────────────────┤
│        [Cancelar]  [Sí, Cambiar]           │
└─────────────────────────────────────────────┘
```

4. Click en **"Sí, Cambiar"**
5. Esperar proceso (30-60 segundos)

**El proceso completo:**

**Paso 1: Backup Automático** (si habilitado)
```bash
# Crea backup del mundo activo actual
Creating backup: world-default_auto_20251130_183045.tar.gz
Compressing world files... 512 MB
Backup created successfully
```

**Paso 2: Detener Servidor**
```bash
Stopping Minecraft server...
Sending SIGTERM to process...
Waiting for graceful shutdown...
Server stopped
```

**Paso 3: Cambiar Symlink**
```bash
# Elimina symlink actual
rm worlds/active

# Crea nuevo symlink
ln -s survival-hard worlds/active

# Verifica
readlink worlds/active
# Output: survival-hard
```

**Paso 4: Actualizar Metadata**
```json
// worlds/survival-hard/metadata.json
{
  ...
  "last_accessed": "2025-11-30T18:30:45"  ← Actualizado
}
```

**Paso 5: Reiniciar Servidor**
```bash
Starting Minecraft server...
Loading world: survival-hard
[18:31:00] [Server thread/INFO]: Preparing level "world"
[18:31:05] [Server thread/INFO]: Done (5.234s)!
Server started successfully
```

**Paso 6: Confirmación**
```
✅ Mundo activado exitosamente
   Mundo activo: survival-hard
   Backup creado: world-default_auto_20251130_183045.tar.gz
```

#### Desde API REST

```bash
curl -X POST http://localhost:5000/api/worlds/survival-hard/activate \
  -H "Cookie: session=tu_session_cookie"
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Mundo activado exitosamente",
  "previous_world": "world-default",
  "new_world": "survival-hard",
  "backup_created": true,
  "backup_filename": "world-default_auto_20251130_183045.tar.gz",
  "server_restarted": true,
  "restart_time_seconds": 45
}
```

### Tiempo de Inactividad

El cambio de mundo requiere reiniciar el servidor:

| Tamaño del Mundo | Tiempo Estimado |
|------------------|------------------|
| < 500 MB         | 30-45 segundos  |
| 500 MB - 2 GB    | 45-90 segundos  |
| 2 GB - 5 GB      | 90-180 segundos |
| > 5 GB           | 3-5 minutos     |

**Factores que afectan:**
- Tamaño del mundo
- Velocidad del disco (SSD vs HDD)
- RAM disponible
- Número de chunks cargados

### Notificar a Jugadores

**Recomendaciones:**

#### Antes del Cambio:
```bash
# Avisar a jugadores (5 minutos antes)
/say Cambio de mundo en 5 minutos. Por favor, guarda tu progreso.

# Avisar (1 minuto antes)
/say Cambio de mundo en 1 minuto. Desconexión inminente.

# Avisar (10 segundos antes)
/say Cambiando a mundo: survival-hard en 10 segundos...
```

#### Durante el Cambio:
- Los jugadores verán: "Connection Lost: Server is restarting"
- Pueden reconectarse cuando el servidor esté listo

#### Después del Cambio:
```bash
# Verificar que el mundo correcto está activo
/seed
/difficulty
```

---

## ⚙️ Configuración Avanzada

### server.properties Independiente

Cada mundo puede tener su propia configuración completa:

**Ejemplo: world-default**
```properties
# worlds/world-default/server.properties (si existe)
gamemode=survival
difficulty=normal
pvp=true
max-players=20
view-distance=10
```

**Ejemplo: creative-plots**
```properties
# worlds/creative-plots/server.properties
gamemode=creative
difficulty=peaceful
pvp=false
max-players=50
view-distance=16
spawn-protection=0
enable-command-block=true
```

**Jerarquía de configuración:**

1. `worlds/{slug}/server.properties` (específico del mundo)
2. `config/server.properties` (configuración global)

Si existe `worlds/{slug}/server.properties`, se usa ese.
Si no, se usa la configuración global.

### Semillas (Seeds) Personalizadas

**Al crear mundo:**
```json
{
  "seed": "12345"
}
```

**Semillas útiles:**

| Tipo | Seed | Descripción |
|------|------|-------------|
| **Spawn en Taiga** | `-4199716205743947022` | Spawn en bioma Taiga con aldea |
| **Islas** | `2151901553968352745` | Múltiples islas grandes |
| **Montañas** | `8678942899319966093` | Picos nevados extremos |
| **Mesa** | `4031384495743822299` | Bioma Mesa (Badlands) |
| **Todos los biomas** | `3257840388504953787` | Diversidad de biomas cerca |

### Configuración de Generación

**Mundo flat (superflat):**
```json
{
  "level_type": "flat",
  "generator_settings": "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block"
}
```

**Mundo amplificado:**
```json
{
  "level_type": "amplified"
}
```

**Mundo personalizado:**
```json
{
  "level_type": "customized",
  "generator_settings": "{...json personalizado...}"
}
```

### Limitar Tamaño de Mundos

**Usando world border:**
```bash
# Activar mundo
# Conectarse al servidor
# Ejecutar desde consola o in-game:
/worldborder set 10000
/worldborder center 0 0
```

Esto limita el mundo a 10,000 bloques de radio.

### Configuración de Spawn

**Protección de spawn:**
```properties
# server.properties del mundo
spawn-protection=16  # Bloques de radio protegidos
```

**Forzar spawn en punto específico:**
```bash
# Conectarse al mundo
/setworldspawn 100 64 200
```

---

## 🐛 Resolución de Problemas

### Problema: Error al Crear Mundo

#### Síntoma
```
Error: No se pudo crear el mundo
```

**Causas posibles:**
1. Slug ya existe
2. Permisos de escritura
3. Espacio en disco insuficiente

**Solución:**
```bash
# Verificar mundos existentes
ls -la worlds/

# Verificar permisos
ls -ld worlds/
# Debe ser: drwxr-xr-x mkd mkd

# Arreglar permisos
chmod 755 worlds/
chown $USER:$USER worlds/

# Verificar espacio
df -h
```

### Problema: Symlink Roto

#### Síntoma
```bash
ls -la worlds/active
# lrwxrwxrwx  active -> world-default (red, broken)
```

El servidor no puede iniciar, error: "World not found"

**Solución:**
```bash
# 1. Ver hacia dónde apunta
readlink worlds/active
# Output: world-default

# 2. Verificar que el mundo existe
ls worlds/world-default/
# Si no existe, el symlink está roto

# 3. Eliminar symlink roto
rm worlds/active

# 4. Crear symlink correcto
cd worlds
ln -s mundo-que-existe active
cd ..

# 5. Verificar
readlink worlds/active
ls -la worlds/active/world/
```

### Problema: Backup Falla

#### Síntoma
```
Error: No se pudo crear backup
Timeout al comprimir archivos
```

**Causas:**
- Mundo muy grande (> 10 GB)
- Disco lento (HDD)
- Poco espacio

**Solución:**
```bash
# Verificar espacio
df -h backups/

# Ver tamaño del mundo
du -sh worlds/survival-hard/

# Limpiar backups antiguos
cd backups/worlds/survival-hard/
ls -lh
# Eliminar backups muy antiguos
rm *_20251120_*.tar.gz

# Probar backup de nuevo
```

### Problema: Servidor No Reinicia Después de Cambio

#### Síntoma
Después de activar mundo, el servidor no inicia.

**Diagnóstico:**
```bash
# Ver logs del contenedor
docker logs mc-paper --tail 100

# Ver estado del contenedor
docker ps -a | grep mc-paper

# Errores comunes en logs:
# [ERROR]: Failed to load world
# [ERROR]: Invalid level-name in server.properties
```

**Solución:**
```bash
# 1. Verificar symlink
readlink worlds/active
ls -la worlds/active/world/

# 2. Verificar metadata del mundo
cat worlds/survival-hard/metadata.json

# 3. Reiniciar manualmente
docker restart mc-paper

# 4. Si falla, revertir a mundo anterior
rm worlds/active
ln -s world-default worlds/active
docker restart mc-paper
```

### Problema: Restauración de Backup Falla

#### Síntoma
```
Error: No se pudo restaurar backup
```

**Solución:**
```bash
# 1. Verificar que el backup existe
ls -lh backups/worlds/survival-hard/survival-hard_manual_*.tar.gz

# 2. Probar extracción manual
cd /tmp
tar -tzf /home/mkd/contenedores/mc-paper/backups/worlds/survival-hard/backup.tar.gz
# Si da error, el backup está corrupto

# 3. Si está corrupto, usar backup anterior
# Desde panel web: seleccionar backup más antiguo

# 4. Si backup es válido pero falla restauración:
# Detener servidor
docker stop mc-paper

# Eliminar mundo actual
rm -rf worlds/survival-hard/world*

# Extraer backup manualmente
cd worlds/survival-hard/
tar -xzf ../../backups/worlds/survival-hard/backup.tar.gz

# Reiniciar
docker start mc-paper
```

### Problema: Mundos No Aparecen en Panel

#### Síntoma
Panel web muestra lista vacía o no muestra todos los mundos.

**Solución:**
```bash
# 1. Verificar estructura de mundos
ls -la worlds/

# 2. Verificar metadata.json en cada mundo
cat worlds/world-default/metadata.json
cat worlds/survival-hard/metadata.json

# 3. Si falta metadata.json, crearlo:
cat > worlds/survival-hard/metadata.json << 'EOF'
{
  "slug": "survival-hard",
  "name": "Survival Hard",
  "description": "Mundo de supervivencia difícil",
  "created_at": "2025-11-30T18:00:00",
  "gamemode": "survival",
  "difficulty": "hard",
  "pvp": true
}
EOF

# 4. Recargar panel web (F5)
```

---

## ❓ Preguntas Frecuentes

### General

**P: ¿Cuántos mundos puedo tener?**
R: No hay límite técnico. El límite es el espacio en disco disponible.

**P: ¿Puedo tener múltiples mundos activos simultáneamente?**
R: No, solo un mundo puede estar activo a la vez. Para múltiples mundos simultáneos necesitas múltiples servidores o un servidor proxy (BungeeCord/Velocity).

**P: ¿Los jugadores pierden inventario al cambiar de mundo?**
R: Sí, cada mundo tiene sus propios datos de jugadores (inventario, ubicación, experiencia). Son completamente independientes.

### Backups

**P: ¿Los backups automáticos consumen mucho espacio?**
R: Depende del tamaño de tus mundos. Con retention_count=5, mantendrás 5 backups automáticos + todos los manuales. Ejemplo: mundo de 1GB = 5GB en backups auto.

**P: ¿Puedo descargar backups?**
R: Sí, los archivos .tar.gz están en `backups/worlds/{slug}/` y puedes descargarlos vía SCP/SFTP o directamente desde el servidor.

**P: ¿Qué pasa si elimino un backup manualmente?**
R: El panel web lo detectará en el próximo refresh. Solo asegúrate de eliminar tanto el .tar.gz como el .json (si existe).

### Migración

**P: ¿Puedo revertir la migración después de varios días?**
R: Sí, siempre que no hayas eliminado el backup pre-migración. El script `rollback-multiworld.sh` funciona en cualquier momento.

**P: ¿La migración afecta mis plugins?**
R: No, los plugins están en `/plugins` y son independientes de los mundos.

### Rendimiento

**P: ¿Cambiar de mundo afecta el rendimiento?**
R: El cambio en sí causa 30-60s de inactividad. Una vez activo, el rendimiento es idéntico a tener un solo mundo.

**P: ¿Tener muchos mundos ralentiza el servidor?**
R: No, solo el mundo activo se carga en RAM. Los mundos inactivos son solo archivos en disco.

### Configuración

**P: ¿Puedo usar diferentes versiones de Minecraft por mundo?**
R: No, todos los mundos usan la misma versión del servidor PaperMC. Solo puedes cambiar la versión globalmente con `./change-server-version.sh`.

**P: ¿Puedo compartir plugins entre mundos pero con configs diferentes?**
R: Depende del plugin. Algunos plugins soportan configuración multi-mundo (como Multiverse), otros comparten la misma config.

**P: ¿Cómo hago para que un mundo sea siempre peaceful?**
R: Edita el `server.properties` del mundo y establece `difficulty=peaceful`. También puedes usar comandos: `/difficulty peaceful` y `/gamerule doMobSpawning false`.

### Solución de Problemas

**P: El servidor no inicia después de cambiar de mundo**
R: Verifica:
1. `readlink worlds/active` apunta a mundo válido
2. `ls worlds/active/world/` contiene archivos del mundo
3. `docker logs mc-paper` para ver errores específicos

**P: Los jugadores no pueden ver sus construcciones después del cambio**
R: Esto es normal si cambiaste a un mundo diferente. Cada mundo es independiente. Si esperabas ver las construcciones, probablemente quieres volver al mundo anterior.

**P: ¿Cómo recupero un mundo eliminado por error?**
R: Si tienes backups, usa la función de restaurar. Si no hay backups, lamentablemente el mundo está perdido. Por eso es crucial mantener backups automáticos activados.

---

## 📞 Soporte y Recursos

### Documentación Relacionada

- **[../README.md](../README.md)** - Documentación principal del proyecto
- **[../BACKUP_SYSTEM.md](../BACKUP_SYSTEM.md)** - Sistema de backups detallado
- **[../BACKUP_CONFIG.md](../BACKUP_CONFIG.md)** - Configuración de backups
- **[../PERFORMANCE_OPTIMIZATION.md](../PERFORMANCE_OPTIMIZATION.md)** - Optimización del panel

### Scripts de Testing

```bash
# Ejecutar suite completa de tests
./run-tests.sh

# Tests específicos del sistema multi-mundo:
# - Verificar estructura de directorios
# - Verificar archivos de modelos
# - Verificar symlinks
# - Test de BackupService
# - Verificar endpoints API
```

### Reportar Problemas

Si encuentras un bug o tienes una sugerencia:
1. Verifica que no sea un problema conocido (FAQ arriba)
2. Ejecuta `./run-tests.sh` y guarda el output
3. Crea un Issue en GitHub con:
   - Descripción del problema
   - Pasos para reproducir
   - Output de `./run-tests.sh`
   - Logs relevantes (`docker logs mc-paper`)

### Contribuir

¿Quieres mejorar el sistema multi-mundo?
1. Lee [CONTRIBUTING.md](CONTRIBUTING.md) (si existe)
2. Crea un fork del repositorio
3. Implementa tu mejora
4. Crea un Pull Request

---

## 🎉 Conclusión

El **Sistema Multi-Mundo** te da la flexibilidad de gestionar múltiples mundos de Minecraft con total independencia, backups automáticos, y cambio en caliente desde un panel web intuitivo.

**Características principales:**
- ✅ Mundos ilimitados con configuración independiente
- ✅ Cambio entre mundos sin detener el servidor (solo restart rápido)
- ✅ Backups automáticos y manuales por mundo
- ✅ Sistema de retención configurable
- ✅ Restauración con backup de seguridad automático
- ✅ Panel web completo para todas las operaciones
- ✅ API REST para automatización

**Próximos pasos:**
1. Crea tu primer mundo personalizado
2. Configura backups automáticos
3. Experimenta con diferentes configuraciones
4. ¡Disfruta de tu servidor multi-mundo!

---

<div align="center">

**Sistema Multi-Mundo v2.0** 🌍✨

[⬆ Volver arriba](#-guía-completa-del-sistema-multi-mundo)

</div>
