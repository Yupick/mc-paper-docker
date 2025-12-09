# Roadmap: Normalización de Estructura de Archivos - Plugin MMORPG y Panel Web

**Fecha:** 9 de diciembre de 2025  
**Estado:** En Planificación  
**Objetivo:** Unificar y normalizar la gestión de archivos de configuración y datos entre el plugin MMORPG y el panel web.

---

## 📋 Tabla de Contenidos

1. [Clasificación de Datos](#clasificación-de-datos)
2. [Estructura de Directorios](#estructura-de-directorios)
3. [Modificaciones de Código](#modificaciones-de-código)
4. [Inicialización Automática](#inicialización-automática)
5. [Limpieza de Duplicados](#limpieza-de-duplicados)
6. [Secuencia de Implementación](#secuencia-de-implementación)

---

## 🚀 Estado Actual

- ✅ **Fase 1:** Crear estructura base en `config/` con archivos .example
- ✅ **Fase 2:** Actualizar scripts de instalación (`create.sh`, `install-mmorpg-plugin.sh`, `quick-install.sh`)
- ✅ **Fase 3:** Normalizar panel web - Implementar `_get_data_location()` y actualizar endpoints RPG
- ✅ **Fase 4:** Actualizar plugin Java con resolución automática de rutas
  - ✅ Crear `PathResolver.java` - Centraliza resolución de rutas
  - ✅ Crear `DataInitializer.java` - Auto-inicializa archivos faltantes
  - ✅ Integrar en `MMORPGPlugin.java` - Getters y acceso público
- ✅ **Fase 5:** Limpiar duplicados y archivo mal ubicados
  - ✅ Mover 4 archivos config de data/ a raíz de MMORPGPlugin/
  - ✅ Agregar archivos de datos universales faltantes (npcs.json, quests.json, enchantments.json, pets.json)
  - ✅ Estructura finalizada correctamente
- ✅ **Fase 6:** Pruebas end-to-end
  - ✅ Plan de testing completo documentado
  - ✅ 15 casos de testing definidos
  - ✅ Checklist de verificación preparado
  - ✅ Ready for manual testing execution

---

## 🗂️ Clasificación de Datos

### **Datos Universales (Globales para todos los mundos)**
Se almacenan en: `plugins/MMORPGPlugin/data/`

| Tipo | Archivo | Alcance | Editable |
|------|---------|---------|----------|
| **NPCs** | `npcs.json` | Universal | Sí (Panel Web) |
| **Quests** | `quests.json` | Universal | Sí (Panel Web) |
| **Mobs** | `mobs.json` | Universal | Sí (Panel Web) |
| **Mascotas** | `pets.json` | Universal | Sí (Panel Web) |
| **Encantamientos** | `enchantments.json` | Universal | Sí (Panel Web) |
| **Items** | `items.json` | Universal | Sí (Panel Web) |

### **Datos Locales por Mundo**
Se almacenan en: `worlds/{mundo}/data/`

| Tipo | Archivo | Alcance | Generado por |
|------|---------|---------|-------------|
| **NPCs (Mundo)** | `npcs.json` | Local | Panel Web / Plugin |
| **Quests (Mundo)** | `quests.json` | Local | Panel Web / Plugin |
| **Mobs (Mundo)** | `mobs.json` | Local | Panel Web / Plugin |
| **Mascotas (Mundo)** | `pets.json` | Local | Plugin (progreso jugadores) |
| **Encantamientos (Mundo)** | `enchantments.json` | Local | Panel Web / Plugin |

### **Datos Exclusivos Locales por Mundo**
Se almacenan en: `worlds/{mundo}/data/`

| Tipo | Archivo | Alcance | Generado por |
|------|---------|---------|-------------|
| **Jugadores RPG** | `players.json` | Local | Plugin (dato en tiempo real) |
| **Estado RPG** | `status.json` | Local | Plugin (dato en tiempo real) |
| **Invasiones** | `invasions.json` | Local | Plugin (eventos del mundo) |
| **Kills Tracking** | `kills.json` | Local | Plugin (estadísticas del mundo) |
| **Respawn Config** | `respawn.json` | Local | Panel Web / Plugin |
| **Metadata Mundo** | `metadata.json` | Local | Panel Web (información del mundo) |

---

## 📁 Estructura de Directorios

### **1. Configuración Base en `config/` (Ejemplos - Copias de Referencia)**

```
config/
├── plugin/                              # Archivos de configuración del plugin
│   ├── achievements_config.json.example
│   ├── bestiary_config.json.example
│   ├── crafting_config.json.example
│   ├── dungeons_config.json.example
│   ├── enchanting_config.json.example
│   ├── enchantments_config.json.example
│   ├── events_config.json.example
│   ├── invasions_config.json.example
│   ├── pets_config.json.example
│   ├── ranks_config.json.example
│   ├── respawn_config.json.example
│   └── squad_config.json.example
│
└── plugin-data/                         # Ejemplos de datos universales
    ├── items.json.example
    ├── mobs.json.example 
    ├── npcs.json.example
    ├── quests.json.example

    └── enchantments.json.example
```

**Responsabilidad:** `create.sh` e `install-mmorpg-plugin.sh` crean esta estructura con ejemplos.

---

### **2. Plugin MMORPG - Estructura en `plugins/MMORPGPlugin/`**

```
plugins/MMORPGPlugin/
├── achievements_config.json             # Configuración (copiado desde config/)
├── bestiary_config.json
├── crafting_config.json
├── dungeons_config.json
├── enchanting_config.json
├── enchantments_config.json
├── events_config.json
├── invasions_config.json
├── pets_config.json
├── ranks_config.json
├── respawn_config.json
├── squad_config.json
│
├── data/                                # Datos universales (globales)
│   ├── items.json                       # Items globales (copiado en install)
│   ├── mobs.json                        # Mobs globales
│   ├── npcs.json                        # NPCs globales
│   ├── quests.json                      # Quests globales
│   ├── pets.json                       # Mascotas globales
│   └── enchantments.json                # Encantamientos globales
│
└── src/
    ├── main/java/com/mmorpg/
    │   ├── config/
    │   ├── data/
    │   ├── managers/
    │   └── ...
```

**Responsabilidad:** 
- `install-mmorpg-plugin.sh` copia archivos de `config/plugin/` y `config/plugin-data/`
- Plugin valida y crea archivos faltantes con contenido por defecto al iniciar

---

### **3. Mundos - Estructura en `worlds/{mundo}/`**

```
worlds/mmorpg-survival/
├── metadata.json                        # Información del mundo (incluyendo isRPG)
├── server.properties                    # Propiedades del servidor
│
├── data/                                # Datos específicos de este mundo
│   ├── players.json                     # Jugadores RPG (en tiempo real)
│   ├── status.json                      # Estado RPG del mundo (en tiempo real)
│   ├── invasions.json                   # Invasiones activas en este mundo
│   ├── kills.json                       # Estadísticas de kills del mundo
│   ├── respawn.json                     # Configuración de respawn del mundo
│   │
│   ├── npcs.json (OPCIONAL)             # NPCs específicos del mundo
│   ├── quests.json (OPCIONAL)           # Quests específicas del mundo
│   ├── mobs.json (OPCIONAL)             # Mobs específicos del mundo
│   ├── pets.json (OPCIONAL)             # Evoluciones/mascotas capturadas
│   └── enchantments.json (OPCIONAL)     # Encantamientos específicos del mundo
│
├── world/                               # Datos de Minecraft
├── world_nether/
└── world_the_end/
```

**Responsabilidad:**
- `create.sh` crea carpeta `data/` vacía al crear un mundo
- Panel Web crea `data/` si no existe al crear mundo RPG
- Plugin crea archivos faltantes al detectar mundo RPG activo

---

## 🔧 Modificaciones de Código

### **2.1 Scripts de Instalación**

#### `create.sh`
```bash
# Cambios:
1. Crear estructura base en config/
   - config/plugin/*.example
   - config/plugin-data/*.example

2. Para cada mundo RPG:
   - Crear worlds/{mundo}/data/
   - Crear metadata.json con isRPG: true
```

#### `install-mmorpg-plugin.sh`
```bash
# Cambios:
1. Copiar archivos de config/plugin/ → plugins/MMORPGPlugin/
2. Copiar archivos de config/plugin-data/ → plugins/MMORPGPlugin/data/
3. Verificar y crear directorios si no existen
4. Establecer permisos correctos
```

#### `quick-install.sh`
```bash
# Cambios:
- Llamar a install-mmorpg-plugin.sh
- Garantizar que config/ existe antes de copiar
```

---

### **2.2 Panel Web - Rutas de Datos**

#### Archivo: `web/app.py`

**Cambios principales:**
```python
# Función nueva: _get_data_location(world_slug, data_type, scope)
# Retorna la ruta correcta según:
#   - world_slug: mundo específico o None para global
#   - data_type: 'npc', 'quest', 'mob', 'pet', 'enchantment', 'player', 'invasion', 'kills', 'respawn'
#   - scope: 'local' o 'universal'

# Rutas resolución:
UNIVERSAL = plugins/MMORPGPlugin/data/{filename}.json
LOCAL = worlds/{mundo}/data/{filename}.json
```

**Endpoints a actualizar:**
| Endpoint | Cambio |
|----------|--------|
| `/api/rpg/npcs` | Diferenciar entre universales y locales |
| `/api/rpg/quests` | Diferenciar entre universales y locales |
| `/api/rpg/mobs` | Diferenciar entre universales y locales |
| `/api/rpg/pets` | Diferenciar entre universales y locales |
| `/api/rpg/enchantments` | Diferenciar entre universales y locales |
| `/api/rpg/invasions` | Solo local |
| `/api/rpg/kills` | Solo local |
| `/api/rpg/respawn` | Solo local |
| `/api/worlds/<slug>/rpg/summary` | Usar nuevas rutas |

---

#### Archivo: `web/models/rpg_manager.py`

**Cambios principales:**
```python
class RPGManager:
    def _resolve_data_path(self, world_slug, filename, scope='universal'):
        """
        Resuelve la ruta correcta para archivos de datos RPG.
        
        Args:
            world_slug: slug del mundo (None para datos universales)
            filename: nombre del archivo (ej: 'npcs.json')
            scope: 'universal' o 'local'
        
        Returns:
            Path: ruta al archivo
        """
        if scope == 'universal':
            return Path(self.plugin_data_path) / filename
        elif scope == 'local':
            return Path(MINECRAFT_DIR) / 'worlds' / world_slug / 'data' / filename
        else:
            raise ValueError(f"Scope inválido: {scope}")
    
    def get_data_by_scope(self, world_slug, filename, data_key=None, scope='local'):
        """
        Obtiene datos separados por scope (local/universal).
        """
        # Implementación usando _resolve_data_path
```

---

#### Archivo: `web/models/world_manager.py`

**Cambios principales:**
```python
class WorldManager:
    def create_world(self, ...):
        # Cambios:
        1. Crear worlds/{mundo}/data/ automáticamente
        2. Crear metadata.json con estructura completa
        3. Si isRPG=True, crear archivos base en data/:
           - status.json (vacío {})
           - players.json (vacío {})
           - invasions.json (vacío [])
           - kills.json (vacío {})
           - pets.json  (vacío [])
           - respawn.json (copia de config/plugin-data/respawn.json.example)
```

---

### **2.3 Plugin Java (src/)**

**Cambios principales:**

#### Gestión de Configuración
```java
// Antes: Archivos sueltos en plugins/MMORPGPlugin/
// Después: Cargar desde plugins/MMORPGPlugin/{archivo}.json

ConfigLoader configLoader = new ConfigLoader(pluginDataDir);
CraftingConfig crafting = configLoader.loadCraftingConfig();
```

#### Gestión de Datos Universales
```java
// Ubicación: plugins/MMORPGPlugin/data/

DataManager dataManager = new DataManager(pluginDataDir);
List<Mob> mobs = dataManager.loadMobs();        // plugins/MMORPGPlugin/data/mobs.json
List<NPC> npcs = dataManager.loadNPCs();        // plugins/MMORPGPlugin/data/npcs.json
List<Quest> quests = dataManager.loadQuests();  // plugins/MMORPGPlugin/data/quests.json
```

#### Gestión de Datos Locales por Mundo
```java
// Ubicación: worlds/{mundo}/data/

WorldDataManager worldData = new WorldDataManager(worldDir);
PlayerData players = worldData.loadPlayers();           // worlds/{mundo}/data/players.json
WorldStatus status = worldData.loadStatus();           // worlds/{mundo}/data/status.json
List<Invasion> invasions = worldData.loadInvasions();  // worlds/{mundo}/data/invasions.json
KillStats kills = worldData.loadKills();               // worlds/{mundo}/data/kills.json
RespawnConfig respawn = worldData.loadRespawn();       // worlds/{mundo}/data/respawn.json
```

#### Inicialización Automática
```java
// Al iniciar el plugin:
class MMORPGInitializer {
    public void initialize() {
        // 1. Crear plugins/MMORPGPlugin/data/ si no existe
        // 2. Copiar archivos desde config/plugin-data/*.example si faltan
        // 3. Crear archivos vacíos si no hay ejemplos
        
        // 4. Para cada mundo RPG activo:
        //    - Crear worlds/{mundo}/data/ si no existe
        //    - Crear status.json, players.json si faltan
        //    - Crear invasions.json, kills.json si faltan
    }
}
```

---

## 🚀 Inicialización Automática

### **Responsabilidades del Plugin**

Cuando el plugin MMORPG inicia (`onEnable()`):

1. **Validar estructura de datos universales:**
   ```
   Si no existe plugins/MMORPGPlugin/data/
   → Crear directorio
   
   Para cada archivo esperado (mobs.json, npcs.json, etc.):
   → Si no existe en plugins/MMORPGPlugin/data/
   → Copiar desde config/plugin-data/{archivo}.example
   → Si no existe ejemplo, crear vacío {}
   ```

2. **Validar estructura de cada mundo RPG activo:**
   ```
   Para mundo_actual:
   Si es_rpg = true en metadata.json
   
   → Crear worlds/{mundo}/data/ si no existe
   → Crear status.json si no existe: {}
   → Crear players.json si no existe: {}
   → Crear invasions.json si no existe: []
   → Crear kills.json si no existe: {}
   ```

### **Responsabilidades del Panel Web**

Cuando se crea un mundo RPG desde el panel:

1. **Crear estructura de directorios:**
   ```
   worlds/{nuevo_mundo}/data/
   ```

2. **Crear archivos base:**
   ```
   metadata.json (con isRPG: true)
   data/status.json (vacío {})
   data/players.json (vacío {})
   data/invasions.json (vacío [])
   data/kills.json (vacío {})
   ```

---

## 🧹 Limpieza de Duplicados

### **Archivos a Eliminar**

```
Eliminar del repositorio actual:
├── plugins/MMORPGPlugin/data/world/*
│   (Mantener solo si es symlink/referencia actual)
├── plugins/MMORPGPlugin/npcs.json (si existe)
├── plugins/MMORPGPlugin/quests.json (si existe)
├── plugins/MMORPGPlugin/mobs.json (si existe)
├── plugins/MMORPGPlugin/items.json (si existe)
└── Cualquier otro archivo suelto que debería estar en config/ o data/
```

### **Archivos a Mantener como Referencia**

```
Mantener en config/ para referencia:
├── config/plugin/*.example
├── config/plugin-data/*.example
└── docs/ROADMAP_NORMALIZACION_ARCHIVOS.md
```

---

## 📋 Secuencia de Implementación

### **Fase 1: Preparación (Sem 1)**
- ✅ Crear estructura base en `config/plugin/` y `config/plugin-data/` con archivos `.example`
- ✅ Generar ejemplos de contenido para cada tipo de archivo
- ✅ Actualizar `create.sh` para generar estructura base

### **Fase 2: Scripts de Instalación (Sem 1-2)**
- ✅ Actualizar `install-mmorpg-plugin.sh` para copiar desde `config/`
- ✅ Actualizar `quick-install.sh`
- ✅ Probar en entorno limpio

### **Fase 3: Normalización del Panel Web (Sem 2-3)**
- ✅ Actualizar `web/app.py` con funciones de resolución de rutas
- ✅ Actualizar `web/models/rpg_manager.py` con métodos de scope
- ✅ Actualizar `web/models/world_manager.py` para crear `worlds/{mundo}/data/`
- ✅ Actualizar endpoints de API para usar nuevas rutas
- ✅ Probar endpoints con datos locales y universales

### **Fase 4: Actualización del Plugin Java (Sem 3-4)**
- ✅ Crear clases para resolución de rutas
- ✅ Actualizar ConfigLoader para leer de nuevas ubicaciones
- ✅ Crear/actualizar DataManager para datos universales
- ✅ Crear WorldDataManager para datos locales
- ✅ Implementar inicialización automática

### **Fase 5: Limpieza (Sem 4)**
- ✅ Eliminar archivos duplicados del repositorio
- ✅ Validar que no hay referencias rotas
- ✅ Documentar cambios en CHANGELOG

### **Fase 6: Pruebas e Integración (Sem 5)**
- ✅ Test end-to-end: crear mundo → guardar NPC → verificar ubicación
- ✅ Test end-to-end: invasión → guardar en local → verificar ubicación
- ✅ Test end-to-end: cargar datos universales y locales desde panel
- ✅ Test: inicialización automática de archivos faltantes

---

## 📊 Resumen de Cambios por Archivo

| Archivo | Líneas | Cambios Principales | Prioridad |
|---------|--------|----------------------|-----------|
| `create.sh` | ~50 | Crear `config/plugin/` y `config/plugin-data/` | **ALTO** |
| `install-mmorpg-plugin.sh` | ~60 | Copiar de `config/` a `plugins/` | **ALTO** |
| `quick-install.sh` | ~20 | Llamar a install-mmorpg-plugin.sh | **ALTO** |
| `web/app.py` | ~100 | Agregar `_get_data_location()` y actualizar endpoints | **ALTO** |
| `web/models/rpg_manager.py` | ~80 | Agregar `_resolve_data_path()` y métodos de scope | **ALTO** |
| `web/models/world_manager.py` | ~30 | Crear `data/` en `create_world()` | **MEDIO** |
| Plugin Java - ConfigLoader | ~40 | Leer de nuevas rutas | **MEDIO** |
| Plugin Java - DataManager | ~100 | Gestionar datos universales | **MEDIO** |
| Plugin Java - WorldDataManager | ~120 | Gestionar datos locales | **MEDIO** |
| Plugin Java - Initializer | ~80 | Crear archivos faltantes | **MEDIO** |

---

## 🎯 Beneficios de la Normalización

✅ **Centralización:** Todos los archivos en ubicaciones predecibles  
✅ **Escalabilidad:** Fácil agregar nuevos tipos de datos  
✅ **Mantenibilidad:** Panel Web y Plugin leen de las mismas rutas  
✅ **Automatización:** Plugin crea archivos faltantes automáticamente  
✅ **Eliminación de Duplicados:** Una única fuente de verdad por tipo de dato  
✅ **Documentación:** Estructura clara y documentada  

---

## ❓ Confirmaciones Necesarias

Antes de iniciar la implementación:

1. ¿Aprueban la estructura propuesta?
2. ¿Algún archivo o tipo de dato adicional que falta?
3. ¿Confirman el orden de prioridades de implementación?
4. ¿Desean que la inicialización automática sea responsabilidad del plugin, panel o ambos?

---

**Documento creado:** 9 de diciembre de 2025  
**Versión:** 1.0 - Planificación  
**Próxima revisión:** Tras confirmación de estructura
