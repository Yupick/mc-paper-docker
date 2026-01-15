# 📊 ANÁLISIS DETALLADO: Carpeta `/config/`

**Fecha:** 27 de diciembre de 2025  
**Tamaño total:** ~540 KB (pequeño, pero la carpeta `data/` es 308 KB)

---

## 🔍 DESGLOSE POR ELEMENTO

### 1️⃣ ARCHIVOS DE CONFIGURACIÓN CRÍTICOS (MANTENER)

#### `config.yml` (712 bytes)
```yaml
Contenido: Configuración general del plugin
Ubicación: /config/config.yml
Usado por: Plugin MMORPG en onEnable()
Criticidad: 🔴 ESENCIAL - Sin este el plugin no inicia bien
¿Eliminar?: NO
```

#### `backup_config.json` (58 bytes)
```json
Contenido: {"auto_backup_enabled": true, "retention_count": 5}
Ubicación: /config/backup_config.json
Usado por: BackupService (backup automático de mundos)
Criticidad: 🟡 IMPORTANTE
¿Eliminar?: NO
```

#### `panel_config.json` (179 bytes)
```json
Contenido: refresh_interval, logs_interval, cache settings
Ubicación: /config/panel_config.json
Usado por: Web panel (app.py)
Criticidad: 🟡 IMPORTANTE
¿Eliminar?: NO
```

#### `server.properties` (229 bytes)
```properties
Contenido: Configuración de Minecraft (motd, difficulty, etc)
Ubicación: /config/server.properties
Usado por: create.sh → copia a worlds/active/server.properties
Criticidad: 🟡 IMPORTANTE - Template
¿Eliminar?: MANTENER COMO TEMPLATE
```

---

### 2️⃣ ARCHIVOS DE CONFIGURACIÓN DUDOSOS (A DECIDIR)

#### `crafting_config.json` (240 bytes)
```json
Contenido: Template de config de crafteo
Ubicación: /config/crafting_config.json
Usado por: create.sh (copia a plugins si no existe)
Tipo: TEMPLATE que se copia una vez
Futuro: Debe ir a SQLite (universal.db)
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `enchanting_config.json` (259 bytes)
```json
Contenido: Template de config de encantamientos
Ubicación: /config/enchanting_config.json
Usado por: create.sh (copia a plugins si no existe)
Tipo: TEMPLATE que se copia una vez
Futuro: Debe ir a SQLite (universal.db)
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `enchantments_config.json` (906 bytes)
```json
Contenido: Definición de 12 encantamientos únicos
Ubicación: /config/enchantments_config.json
Usado por: EnchantmentManager (carga al iniciar)
Criticidad: 🟡 IMPORTANTE - Datos de sistema
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
Nota: Este SÍ contiene datos de negocio, no solo template
```

#### `events_config.json` (1.69 KB)
```json
Contenido: Definición de eventos globales
Ubicación: /config/events_config.json
Usado por: EventManager (carga al iniciar)
Criticidad: 🟡 IMPORTANTE - Datos de sistema
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `pets_config.json` (21 KB)
```json
Contenido: Definición de 50+ mascotas con stats
Ubicación: /config/pets_config.json
Usado por: PetManager (carga al iniciar)
Criticidad: 🟡 IMPORTANTE - Mucho contenido
Tamaño: GRANDE (55% del tamaño de config/*.json)
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `respawn_config.json` (239 bytes)
```json
Contenido: Template de config de respawn
Ubicación: /config/respawn_config.json
Usado por: RespawnManager (carga configuración)
Criticidad: 🟠 MODERADA
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `squad_config.json` (229 bytes)
```json
Contenido: Template de config de escuadras
Ubicación: /config/squad_config.json
Usado por: SquadManager (carga configuración)
Criticidad: 🟠 MODERADA
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

#### `dungeons_config.json` (2.09 KB)
```json
Contenido: Definición de 3-5 dungeons con templates
Ubicación: /config/dungeons_config.json
Usado por: DungeonManager (carga al iniciar)
Criticidad: 🟡 IMPORTANTE
¿Eliminar?: MIGRAMOS A SQLITE LUEGO
```

---

### 3️⃣ BASES DE DATOS EXISTENTES (A CONSOLIDAR)

#### `rpgdata.db` (180 KB) 🔴 IMPORTANTE
```sql
Contenido: Datos principales del plugin
Tablas: players, quests, npcs, economy_transactions, etc
Ubicación: /config/rpgdata.db
Usado por: DatabaseManager.java
Estado: ACTIVO, en uso
Plan: CONSOLIDAR en universal.db + {world}.db
```

#### `minecraft_rpg.db` (28 KB)
```sql
Contenido: Crafteo, historial de encantamientos
Tablas: crafting_history, unlocked_recipes, etc
Ubicación: /config/minecraft_rpg.db
Usado por: CraftingManager, EnchantmentManager
Estado: ACTIVO, en uso
Plan: CONSOLIDAR en universal.db + {world}.db
```

#### `squads.db` (36 KB)
```sql
Contenido: Escuadras/Guilds
Tablas: squads, members, levels
Ubicación: /config/squads.db
Usado por: SquadManager
Estado: ACTIVO, en uso
Plan: CONSOLIDAR en {world}.db
```

---

### 4️⃣ DIRECTORIOS OBSOLETOS (ELIMINAR AHORA)

#### `config/data/` (308 KB) 🔴 PUNTO CRÍTICO
```
Contenido:
├── mmorpg/          (datos del mundo "mmorpg")
│   ├── quests.json
│   ├── npcs.json
│   ├── status.json
│   ├── players.json
│   ├── classes.json
│   └── ...
├── mundodos/        (datos del mundo "mundodos")
│   └── ... (similar)
├── mmorpg-survival/ (datos del mundo "mmorpg-survival")
│   └── ... (similar)
├── active/          (datos del mundo "active")
│   └── ... (similar)
├── quests.json      (global?)
├── items.json
├── mobs.json
└── achievements.json

Status: OBSOLETO
Razón: El panel web lee de /worlds/active/data/, no de /config/data/
¿Eliminar?: SÍ DEFINITIVAMENTE
Notas: 
- Contiene múltiples versiones de cada mundo
- Solo hay UN mundo activo (worlds/active/)
- Los datos en config/data/ NO se usan
```

#### `config/MMORPGPlugin/` (32 KB)
```
Contenido: Copia/backup del directorio del plugin
Estructura de carpetas igual al plugin source
Status: DUPLICADO/OBSOLETO
¿Eliminar?: SÍ (es un duplicado innecesario)
```

#### `config/api/` (8 KB)
```
Contenido: Archivos JSON viejos de API
Status: OBSOLETO
¿Eliminar?: SÍ
```

#### `config/npcs/` (8 KB)
```
Contenido: Datos individuales de NPCs
Status: OBSOLETO (deberían estar en quest-progress/)
¿Eliminar?: SÍ
```

#### `config/pets/` (4 KB)
```
Contenido: Datos de mascotas por jugador?
Status: DUDOSO
¿Eliminar?: SÍ (parece vacío o obsoleto)
```

#### `config/plugin-data/` (24 KB)
```
Contenido: Datos de plugins?
Status: DUDOSO/OBSOLETO
¿Eliminar?: SÍ (probablemente no se usa)
```

#### `config/quest-progress/` (4 KB)
```
Contenido: Progreso de quests (JSON)
Status: OBSOLETO (debe estar en SQLite)
¿Eliminar?: SÍ
```

#### `config/classes/` (4 KB)
```
Contenido: Datos de clases?
Status: OBSOLETO
¿Eliminar?: SÍ
```

#### `config/plugin/` (52 KB)
```
Contenido: Datos/carpeta del plugin
Status: DUPLICADO
¿Eliminar?: SÍ
```

#### `config/lang/` (12 KB)
```
Contenido: Archivos de idioma
Status: IMPORTANTE SI SE USA
¿Pendiente?: Verificar si crear.sh los usa
Recomendación: MANTENER POR AHORA O MOVER A /lang/
```

---

## 📋 RESUMEN DE DECISIONES

### ✅ DEFINITIVAMENTE MANTENER (CRÍTICOS)
```
config/config.yml              (configuración del plugin)
config/backup_config.json      (backup automático)
config/panel_config.json       (config del panel web)
config/server.properties       (template)
```

### ⏳ MIGRAR A SQLITE DESPUÉS (Templates/Definiciones)
```
config/crafting_config.json
config/enchanting_config.json
config/enchantments_config.json
config/events_config.json
config/pets_config.json
config/respawn_config.json
config/squad_config.json
config/dungeons_config.json
```

### 🔴 ELIMINAR AHORA (Obsoletos/Duplicados)
```
config/data/                   (308 KB - PUNTO CRÍTICO)
config/MMORPGPlugin/           (duplicado)
config/api/                    (datos viejos)
config/npcs/                   (datos viejos)
config/pets/                   (parece vacío)
config/plugin-data/           (datos viejos)
config/quest-progress/        (datos viejos)
config/classes/               (datos viejos)
config/plugin/                (duplicado)
```

### 🤔 REVISAR (Validar uso antes de eliminar)
```
config/lang/                   (¿se usa? Ver create.sh)
```

---

## 🎯 PLAN INMEDIATO

### FASE 1A: Limpieza Inmediata (ANTES de cambiar código)
1. Backup de todo: `cp -r config config.backup.$(date +%s)`
2. Eliminar directorios obsoletos (sin código afectado)
3. Mantener archivos críticos y templates
4. Validar que create.sh sigue funcionando

### FASE 1B: Migración a SQLite (DURANTE cambios de código)
1. Crear universal.db con tablas de definiciones
2. Migrar templates JSON → Cargas iniciales en universal.db
3. Las aplicaciones leen de universal.db en lugar de JSON

### RESULTADO FINAL
```
config/
├── config.yml                    ✅ MANTENER
├── backup_config.json            ✅ MANTENER
├── panel_config.json             ✅ MANTENER
├── server.properties             ✅ MANTENER
├── templates/                    ⏳ OPCIONAL
│   ├── crafting_template.json
│   ├── enchanting_template.json
│   └── ...
└── [eliminados 10+ archivos y carpetas]

Ahorro: ~350 KB + mejor performance (sin leer JSON)
```

---

## ⚠️ ORDEN DE OPERACIONES RECOMENDADO

```
1. APROBACIÓN de este análisis
2. Hacer BACKUP completo
3. Fase 1A: Eliminar solo config/data/ + carpetas obsoletas
4. Verificar que create.sh aún funciona
5. LUEGO: Cambiar código del plugin + web
6. Migrar templates a SQLite
7. Eliminar templates JSON cuando no se usen
```

**¿Aprobamos esta limpieza inicial?**
