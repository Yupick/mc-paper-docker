# 🗺️ ROADMAP: Migración Completa a SQLite

**Objetivo:** Unificar todo el almacenamiento de datos del MMORPG Plugin en SQLite, eliminando JSON para datos dinámicos.

**Fecha:** 27 de diciembre de 2025  
**Status:** PLANIFICACIÓN (Requiere aprobación antes de codificación)

---

## 📋 FASE 1: ANÁLISIS Y PREPARACIÓN

### 1.1 Limpieza de `/config/`

#### ❌ CARPETAS/ARCHIVOS A ELIMINAR:
```
config/data/                    # TODO (múltiples mundos, datos desactualizados)
config/MMORPGPlugin/            # Duplicado del plugin, no necesario
config/api/                     # Datos viejos API
config/npcs/                    # Datos individuales, irán a SQLite
config/pets/                    # Config templates, se mueve o elimina
config/plugin-data/            # Datos obsoletos
config/quest-progress/         # Datos viejos
config/classes/                 # Datos viejos
```

#### ✅ ARCHIVOS A MANTENER:
```
config/config.yml              # Configuración general del plugin (ESENCIAL)
config/backup_config.json      # Config backup automático (ESENCIAL)
config/panel_config.json       # Config panel web (ESENCIAL)
config/server.properties       # Props servidor Minecraft (ESENCIAL)
```

#### ⚠️ ARCHIVOS PENDIENTE DE DECISIÓN (Templates de configuración):
```
config/crafting_config.json    # ¿Template o ir a SQLite?
config/enchanting_config.json  # ¿Template o ir a SQLite?
config/enchantments_config.json # ¿Template o ir a SQLite?
config/events_config.json      # ¿Template o ir a SQLite?
config/pets_config.json        # ¿Template o ir a SQLite?
config/respawn_config.json     # ¿Template o ir a SQLite?
config/squad_config.json       # ¿Template o ir a SQLite?
config/dungeons_config.json    # ¿Template o ir a SQLite?
```

**RECOMENDACIÓN:** Estos pueden quedar como **templates de configuración** en `/config/templates/` que el plugin lee UNA SOLA VEZ al inicializar y luego guarda en SQLite. O pueden eliminarse si ya están en SQLite.

---

## 📊 FASE 2: ESTRUCTURA DE BASES DE DATOS

### 2.1 Base de Datos UNIVERSAL (Compartida por todos los mundos)
**Ubicación:** `plugins/MMORPGPlugin/universal.db`  
**Inicializado por:** Plugin al iniciar  
**Replicado por:** Web cuando lo necesita

#### Tablas en `universal.db`:
```sql
-- Definiciones de sistemas (no instancias)
items              -- Items especiales, drops únicos
mobs               -- Definición de mobs especiales
enchantments       -- Definición de encantamientos
crafting_recipes   -- Definición de recetas
achievements       -- Definición de logros (no instancias)
ranks              -- Definición de rangos
events             -- Definición de eventos globales
invasions_templates -- Templates de invasiones (no instancias activas)
squads_templates   -- Definición de niveles de escuadras
```

### 2.2 Bases de Datos LOCALES (Por cada mundo)
**Ubicación:** `plugins/MMORPGPlugin/data/{world_slug}/{world_slug}.db`  
**Inicializado por:** Plugin cuando entra al mundo  
**Replicado por:** Web cuando crea/accede al mundo

#### Tablas en cada `{world_slug}.db`:
```sql
-- Datos locales del mundo
players            -- Datos de jugadores en ESE mundo
player_abilities   -- Habilidades de jugadores
player_quests      -- Progreso de quests por jugador
npcs               -- NPCs spawneados en el mundo
quests             -- Quests disponibles en el mundo
quest_objectives   -- Objetivos de quests
spawns             -- Puntos de spawn del mundo
dungeons           -- Instancias de dungeons
dungeon_participants -- Participantes en dungeons
invasions_active   -- Invasiones activas ahora
economy_transactions -- Transacciones de monedas
enchanted_items    -- Items encantados
crafting_history   -- Historial de crafteos
squads             -- Escuadras del mundo
squad_members      -- Miembros de escuadras
respawn_points     -- Puntos de respawn customizados
kills_tracking     -- Tracking de mobs/jugadores matados
```

### 2.3 Bases de Datos ESPECIALIZADAS (Opcionales, consolidar)
```
minecraft_rpg.db   → CONSOLIDAR en universal.db + {world}.db
squads.db          → CONSOLIDAR en {world}.db
rpgdata.db         → CONSOLIDAR en universal.db + {world}.db
```

---

## 🔧 FASE 3: MODIFICACIONES DEL PLUGIN MMORPG

### 3.1 Cambios en `MMORPGPlugin.java`
```java
// ANTES:
- DatabaseManager (rpgdata.db) → Jugadores, quests, economía
- DataManager (JSON files)     → Exportación a JSON

// DESPUÉS:
- DatabaseManager refactorizado:
  + Connection universal → universal.db
  + Connection world → {world}.db
  + Métodos para obtener DB por mundo
  + Métodos para sincronizar/replicar datos
```

### 3.2 Cambios en managers principales

| Manager | Cambio |
|---------|--------|
| **DataManager** | Eliminar exportación JSON, usar DBs |
| **NPCManager** | Leer/escribir de {world}.db |
| **QuestManager** | Leer/escribir de {world}.db |
| **ClassManager** | Leer/escribir de {world}.db |
| **CraftingManager** | Leer templates de config.yml → guardar en universal.db |
| **EnchantmentManager** | Leer templates → guardar en universal.db |
| **SquadManager** | Migrar de squads.db a {world}.db |
| **EconomyManager** | Leer/escribir de {world}.db |
| **EventManager** | Leer templates → guardar en universal.db |
| **InvasionManager** | Leer/escribir de {world}.db |
| **AchievementManager** | Leer/escribir de universal.db + {world}.db |
| **RankManager** | Leer/escribir de universal.db |

### 3.3 Creación de estructura al arrancar
```
Plugin inicia
├── Crear universal.db si no existe
├── Crear tablas universales
├── Cargar templates de config/ → universal.db (UNA sola vez)
└── Para cada mundo RPG:
    ├── Crear {world}.db si no existe
    ├── Crear tablas locales
    └── Inicializar datos por defecto
```

---

## 🌐 FASE 4: MODIFICACIONES DEL PANEL WEB

### 4.1 Cambios en `rpg_manager.py`
```python
# ANTES:
- Lee JSON desde /config/data/ y /worlds/active/data/

# DESPUÉS:
- Se conecta a SQLite:
  + universal.db → datos globales
  + {world}.db → datos locales del mundo activo
- Métodos para consultar DBs en lugar de leer JSON
```

### 4.2 Cambios en `app.py` (rutas RPG)
```python
# Todas las rutas /api/rpg/* 
├── Usar rpg_manager.get_from_database() en lugar de JSON
├── Crear endpoints para actualizar datos en DB
└── Eliminar lectura de JSON
```

### 4.3 Creación de estructura al crear mundo
```python
def create_world():
    # ... código existente ...
    if is_rpg:
        # Crear {world_slug}.db en plugins/MMORPGPlugin/data/{world_slug}/
        # Copiar schema desde universal.db
        # Inicializar datos por defecto
        # Crear symlink en worlds/{world_name}/data/ → plugins/...db
        pass
```

---

## 🗂️ FASE 5: NUEVA ESTRUCTURA DE DIRECTORIOS

### ANTES:
```
config/
├── config.yml
├── backup_config.json
├── panel_config.json
├── crafting_config.json
├── enchanting_config.json
├── pets_config.json
├── data/                    ← ELIMINAR (300MB+)
│   ├── quests.json
│   ├── npcs.json
│   └── mmorpg/
│       └── ...
└── [otras carpetas]

plugins/MMORPGPlugin/
├── MMORPGPlugin.jar
└── data/                    ← Actualmente aquí
    ├── items.json
    └── mobs.json

worlds/
├── active/ → (symlink)
│   └── data/                ← JSON por mundo
│       ├── npcs.json
│       ├── quests.json
│       └── ...
└── world/
    └── ...
```

### DESPUÉS:
```
config/
├── config.yml               (Configuración general)
├── backup_config.json       (Backup automático)
├── panel_config.json        (Config panel)
├── templates/               (OPCIONAL - templates de config)
│   ├── crafting_template.json
│   ├── enchanting_template.json
│   └── ...
└── [solo lo esencial]

plugins/MMORPGPlugin/
├── MMORPGPlugin.jar
└── data/
    ├── universal.db         ← Datos universales (items, mobs, enchantments, etc)
    └── {world_slug}/
        └── {world_slug}.db  ← Datos locales (jugadores, NPCs, quests, etc)

worlds/
├── active/ → (symlink)
│   └── metadata.json        (Solo metadata del mundo, SIN JSON de datos)
└── world/
    └── metadata.json
```

---

## 📝 FASE 6: PASOS DE IMPLEMENTACIÓN

### Paso 1: Preparar Migraciones de Datos
- [ ] Script para convertir JSON existente → SQLite
- [ ] Backup de datos actuales
- [ ] Validar integridad de datos

### Paso 2: Modificar Plugin
- [ ] Refactorizar DatabaseManager (2 conexiones)
- [ ] Actualizar todos los managers (6-8 cambios)
- [ ] Crear/modificar tablas necesarias
- [ ] Probar con mundo existente

### Paso 3: Modificar Web Panel
- [ ] Refactorizar rpg_manager.py
- [ ] Actualizar endpoints API
- [ ] Probar lectura desde SQLite
- [ ] Validar visualización de datos

### Paso 4: Limpiar Directorios
- [ ] Eliminar config/data/
- [ ] Eliminar archivos JSON redundantes
- [ ] Validar que todo sigue funcionando

### Paso 5: Testing Integral
- [ ] Crear mundo nuevo (debe crear DB automáticamente)
- [ ] Acceder desde panel web
- [ ] Cambiar datos en-juego
- [ ] Verificar reflejo en web
- [ ] Cambiar datos en web
- [ ] Verificar lectura en plugin

---

## ⚠️ CONSIDERACIONES IMPORTANTES

### Base de Datos Compartida vs Separada
**Opción A (Recomendada):** Una DB universal + una DB por mundo
- ✅ Mejor performance
- ✅ Datos claros (universales vs locales)
- ✅ Fácil sincronización
- ❌ Más archivos

**Opción B:** Una sola DB para todo
- ✅ Más simple
- ❌ Puede crecer mucho
- ❌ Sincronización más compleja

### Sincronización Web-Plugin
- Plugin escribe en SQLite
- Web lee de SQLite
- Web puede escribir en SQLite
- Plugin lee cambios del web

**Problema:** Race conditions si ambos escriben al mismo tiempo
**Solución:** 
1. Usar transacciones SQLite
2. Lock mechanism
3. Timestamps para resolver conflictos

### Backups y Recuperación
- ¿Respaldar DB o JSON?
- ¿Frecuencia?
- ¿Cuántas copias mantener?

---

## 🎯 ESTIMACIÓN

| Fase | Tareas | Complejidad | Horas |
|------|--------|------------|-------|
| 1 | Limpieza | Baja | 1 |
| 2 | Diseño DB | Media | 2 |
| 3 | Plugin | Alta | 8-10 |
| 4 | Web | Media | 4-6 |
| 5 | Directorios | Baja | 1 |
| 6 | Testing | Alta | 4-6 |
| **Total** | | | **20-26 horas** |

---

## ❓ PREGUNTAS PARA CONFIRMAR

1. **¿Mantener templates de config?** (crafting, enchanting, etc.)
   - [ ] SÍ: Guardar en config/templates/
   - [ ] NO: Eliminar, usar scripts de setup
   - [ ] MIGRAR: Pasar todo a SQLite

2. **¿Una DB universal o dos?**
   - [ ] Una universal + múltiples locales (RECOMENDADO)
   - [ ] Una sola DB para todo

3. **¿Sincronización web-plugin en tiempo real?**
   - [ ] SÍ: Cada X segundos
   - [ ] NO: Solo lectura desde web

4. **¿Migrar datos existentes?**
   - [ ] SÍ: Convertir JSON → SQLite
   - [ ] NO: Empezar de cero

5. **¿Mantener respaldo JSON?**
   - [ ] SÍ: Exportar periódicamente
   - [ ] NO: SQLite como única fuente

---

## ✅ PRÓXIMO PASO

Una vez **aprobado este roadmap**, procederemos con:

1. Responder las 5 preguntas
2. Comenzar **Fase 1: Limpieza de `/config/`**
3. Seguir con las demás fases en orden

**¿Confirmamos este plan?**
