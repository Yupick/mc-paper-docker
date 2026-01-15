# 🔄 DIAGRAMA: Arquitectura de Datos Post-Migración SQLite

**Status:** Propuesta para revisión  
**Objetivo:** Visualizar cómo fluyen los datos entre Plugin, Web y Bases de Datos

---

## 📐 ARQUITECTURA ACTUAL (Problemática)

```
PLUGIN MMORPG                          WEB PANEL
    │                                      │
    ├─ SQLite (rpgdata.db)            ├─ Lee JSON
    │   ├─ players                     │   ├─ /config/data/
    │   ├─ quests                      │   ├─ /worlds/active/data/
    │   └─ economy                     │   └─ (DESACTUALIZADO)
    │
    ├─ Exporta a JSON                 └─ ❌ DESINCRONIZADO
    │   ├─ status.json                   ❌ Datos viejos
    │   ├─ players.json                  ❌ Sin cambios en tiempo real
    │   └─ (Limitado, no automático)
    │
    └─ CraftingManager (minecraft_rpg.db)
        └─ No exporta a JSON
```

---

## 📐 ARQUITECTURA PROPUESTA (SQLite Centralizado)

```
┌─────────────────────────────────────────────────────────────────┐
│                         MINECRAFT SERVER                        │
│  plugins/MMORPGPlugin/                                          │
│  ├── universal.db  ←──────────┐                                 │
│  │   ├── enchantments         │                                 │
│  │   ├── crafting_recipes     │  Inicializado por:             │
│  │   ├── items                │  1. Plugin (onEnable)          │
│  │   ├── mobs                 │  2. create.sh (primera vez)   │
│  │   ├── achievements_def     │                                 │
│  │   ├── ranks_def            │                                 │
│  │   └── events_templates     │                                 │
│  │                             │                                 │
│  └── data/                     │                                 │
│      ├── {world_slug}/        │                                 │
│      │   ├── {world_slug}.db  │←──┐  Inicializado por:        │
│      │   │   ├── players      │   │  1. Plugin (al entrar)     │
│      │   │   ├── quests       │   │  2. Web (crear mundo)      │
│      │   │   ├── npcs         │   │  3. ConfigManager          │
│      │   │   ├── squads       │   │                             │
│      │   │   └── economy      │   │                             │
│      │   │                     │   │                             │
│      │   └── metadata.json     │───┘  Datos no-RPG             │
│      │                         │                                 │
│      └── active → symlink ────┘                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                          ↑
                          │ (Lee/Escribe)
                          │
┌─────────────────────────────────────────────────────────────────┐
│                         WEB PANEL                               │
│  /web/                                                          │
│  ├── models/rpg_manager.py                                      │
│  │   ├── get_database(world_slug) → {world_slug}.db            │
│  │   ├── get_universal_database() → universal.db              │
│  │   └── query_rpg_data(sql)                                    │
│  │                                                              │
│  ├── app.py (rutas API)                                         │
│  │   ├── /api/rpg/status → Lee de {world_slug}.db             │
│  │   ├── /api/rpg/players → Lee de {world_slug}.db            │
│  │   ├── /api/rpg/quests → Lee de {world_slug}.db             │
│  │   └── /api/rpg/* → Todas leen de DB                         │
│  │                                                              │
│  └── templates/                                                 │
│      └── Muestran datos de DB (via rpg_manager)               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔀 FLUJO DE DATOS: 5 Escenarios Clave

### Escenario 1: Plugin Inicia

```
1. MMORPGPlugin.onEnable()
   ├─ DatabaseManager.init()
   │  ├─ Abre universal.db (si no existe, lo crea)
   │  └─ Crea tablas si no existen
   │
   ├─ Para cada mundo con RPG activado:
   │  ├─ DatabaseManager.initWorldDatabase(world_slug)
   │  ├─ Abre {world_slug}.db (si no existe, lo crea)
   │  ├─ Copia schema desde universal.db
   │  └─ Carga datos iniciales si es primera vez
   │
   └─ Managers cargan config:
      ├─ EnchantmentManager → Lee universal.db
      ├─ CraftingManager → Lee universal.db
      ├─ QuestManager → Lee {world_slug}.db
      ├─ NPCManager → Lee {world_slug}.db
      └─ ...
```

### Escenario 2: Jugador Cambia Clase

```
PLUGIN:
1. Player ejecuta /rpg class warrior
2. ClassManager.setPlayerClass(player, ClassType.WARRIOR)
3. DatabaseManager.executeUpdate(
   "UPDATE players SET class_type = ? WHERE uuid = ?"
   ) en {world_slug}.db
4. Cambio guardado en SQLite

WEB PANEL:
1. GET /api/rpg/status
2. rpg_manager.get_player_class(uuid)
3. SELECT class_type FROM players WHERE uuid = ?
4. Retorna "WARRIOR"

✅ Sincronizado en tiempo real
```

### Escenario 3: Admin Crea Quest desde Web

```
WEB PANEL:
1. POST /api/rpg/quests
2. {name: "Matar 10 orcos", ...}
3. rpg_manager.create_quest(data)
4. INSERT INTO quests VALUES (...) en {world_slug}.db

PLUGIN (próxima sincronización):
1. QuestManager.onReload()
2. DatabaseManager.query("SELECT * FROM quests")
3. Carga nueva quest desde DB
4. Disponible para jugadores

✅ Flujo web → plugin funciona
```

### Escenario 4: Crear Mundo Nuevo (RPG)

```
WEB PANEL:
1. POST /api/worlds (is_rpg=true)
2. WorldManager.create_world(name, is_rpg=true)
3. Crea carpeta en worlds/{name}/
4. Genera metadata.json
5. Llama a rpg_manager.init_world_database(slug)
   ├─ Crea plugins/MMORPGPlugin/data/{slug}/{slug}.db
   ├─ Copia schema desde universal.db
   ├─ Inserta datos iniciales (NPCs, quests base, etc)
   └─ Crea symlink worlds/{name}/data → plugins/...

PLUGIN:
1. Detecta nuevo mundo en próxima sincronización
2. Carga {world_slug}.db
3. Inicia sistemas RPG para ese mundo
4. Jugadores pueden entrar y jugar inmediatamente

✅ Integración web → plugin perfecta
```

### Escenario 5: Cambiar Datos RPG Globales

```
PLUGIN (Admin comando):
1. /rpg admin enchantment level_up fireball 2
2. EnchantmentManager.upgradeEnchantment("fireball", 2)
3. UPDATE enchantments SET level = 2 WHERE id = "fireball"
   en universal.db

WEB PANEL:
1. GET /api/rpg/enchantments
2. rpg_manager.get_universal_database()
3. SELECT * FROM enchantments
4. Muestra encantamientos actualizados

PLUGIN (Otro mundo):
1. Otro servidor/instancia del plugin
2. Carga universal.db
3. Ve el cambio automáticamente

✅ Datos globales sincronizados entre servidores
```

---

## 🔒 MANEJO DE CONCURRENCIA

```
┌─────────────────────────────────────────┐
│  Problema: Race Condition               │
│  Plugin escribe Y Web escribe al mismo  │
│  tiempo en la misma tabla               │
└─────────────────────────────────────────┘

Soluciones implementadas:

1. TRANSACCIONES SQLite
   Plugin: BEGIN TRANSACTION
           UPDATE players ...
           COMMIT
           
   Web: Espera a que Plugin termine
   
2. TIMESTAMPS (Conflict Resolution)
   Cada fila tiene: updated_at
   Si hay conflicto, gana la más reciente
   
3. LOCKS (Opcional)
   Players_lock = Mutex()
   
   Plugin:
       with Players_lock:
           UPDATE players ...
   
   Web:
       with Players_lock:
           UPDATE players ...

RECOMENDACIÓN:
- Usar transacciones SQLite + timestamps
- Simple, eficaz, sin complejidad extra
```

---

## 📊 COMPARATIVA: JSON vs SQLite

| Aspecto | JSON | SQLite |
|---------|------|--------|
| **Lectura (1000 registros)** | ~100ms | ~10ms |
| **Escritura** | Reescribir todo | Solo 1 fila |
| **Sincronización** | Archivo completo | Inmediata |
| **Queries complejas** | Imposible | Fácil (WHERE, JOIN) |
| **Backup** | Copiar archivo | dump/restore |
| **Transacciones** | No | Sí |
| **Tamaño en disco** | Mayor | Menor |
| **Panel web** | Siempre viejo | Siempre actual |

---

## 🔄 CICLO DE VIDA DE UN DATO

### Ejemplo: Obtener experiencia en una quest

**ANTES (JSON - PROBLEMA):**
```
1. Plugin: Jugador completa quest
   ├─ Suma XP en memoria
   ├─ Escribe en rpgdata.db
   └─ Exporta players.json (NO automático)

2. Web: Lee /config/data/players.json
   ├─ ❌ Archivo está viejo
   ├─ ❌ Falta XP de la quest
   └─ Muestra datos incorrectos

3. Jugador ve XP incorrecto en web (viejo)
```

**DESPUÉS (SQLite - SOLUCIÓN):**
```
1. Plugin: Jugador completa quest
   ├─ Suma XP en memoria
   ├─ UPDATE player_quests SET completed_at = NOW()
   ├─ UPDATE players SET experience = experience + 5000
   └─ COMMIT

2. Web (cualquier momento después):
   ├─ GET /api/rpg/players/{uuid}
   ├─ SELECT experience FROM players WHERE uuid = ?
   ├─ ✅ Obtiene valor ACTUAL de DB
   └─ Muestra XP correcto

3. Jugador ve XP correcto en tiempo real
```

---

## 📈 ESTRUCTURA DE CARPETAS FINAL

```
minecraft-server/
├── config/
│   ├── config.yml                          ✅ Configuración
│   ├── backup_config.json                  ✅ Backup config
│   ├── panel_config.json                   ✅ Web config
│   ├── server.properties                   ✅ Minecraft props
│   └── templates/                          ⏳ Opcional (para instalación)
│       ├── crafting_template.json
│       ├── enchanting_template.json
│       └── ...
│
├── plugins/
│   └── MMORPGPlugin.jar
│       └── (copia ejecutable)
│
├── plugins/MMORPGPlugin/  ← NUEVA UBICACIÓN
│   ├── universal.db                        ✅ Datos globales
│   └── data/
│       ├── world/                          ✅ Mundo 1
│       │   ├── world.db                    (Datos del mundo)
│       │   └── metadata.json               (Config del mundo)
│       ├── adventure/                      ✅ Mundo 2
│       │   ├── adventure.db
│       │   └── metadata.json
│       └── active → symlink                (Apunta al mundo activo)
│
├── worlds/
│   ├── world/
│   │   ├── level.dat                       (Minecraft data)
│   │   └── metadata.json                   (Solo config, NO datos RPG)
│   ├── adventure/
│   │   └── ...
│   └── active → symlink                    (Apunta al mundo activo)
│
├── web/
│   ├── app.py                              ✅ Conecta a DB
│   ├── models/
│   │   ├── rpg_manager.py                  ✅ Queries a DB
│   │   └── ...
│   └── ...
│
└── [otros directorios sin cambios]
```

---

## ✅ CHECKLIST PRE-IMPLEMENTACIÓN

### Análisis
- [ ] Roadmap aprobado
- [ ] Estructura de carpetas aprobada
- [ ] Plan de concurrencia aprobado

### Preparación
- [ ] Backup completo hecho
- [ ] Scripts de migración JSON → SQLite listos
- [ ] Tests unitarios preparados

### Implementación Plugin
- [ ] DatabaseManager refactorizado
- [ ] Schema universal.db definido
- [ ] Schema {world}.db definido
- [ ] Managers actualizados (8 cambios)
- [ ] Tests de lectura/escritura pasan

### Implementación Web
- [ ] rpg_manager.py refactorizado
- [ ] Endpoints API funcionan
- [ ] Pruebas de sincronización pasan

### Limpieza
- [ ] config/data/ eliminado
- [ ] Archivos obsoletos removidos
- [ ] Verifica que todo funciona

---

## 🎯 PREGUNTAS FINALES ANTES DE EMPEZAR

1. **¿Aprobamos esta arquitectura?**
   - ¿Algo a cambiar?
   - ¿Algo a aclarar?

2. **¿Implementamos manejo de concurrencia?**
   - SÍ: Con transacciones + timestamps
   - NO: Asumir que no hay race conditions

3. **¿Mantener templates JSON en config/?**
   - SÍ: Para facilitar personalización futura
   - NO: Eliminarlos cuando se migre a DB

4. **¿Velocidad de implementación?**
   - Rápido: Cambios mínimos
   - Completo: Con validaciones y tests

---

**Una vez aprobado este documento, procederemos con la codificación real.**
