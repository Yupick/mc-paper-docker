# 📋 Plan de Implementación Modular - Sistema MMORPG

## Metodología de Desarrollo

Para cada funcionalidad editable por el usuario, se creará:
1. **Backend Java** - Clases modelo y managers
2. **Archivo de Configuración JSON** - Datos editables
3. **API REST** - Endpoints para CRUD
4. **Pestaña en Panel Web** - UI para administración
5. **Documentación** - Guías y ejemplos

---

## 🎯 MÓDULO 1: Sistema de Progresión Avanzada

### **Etapa 1.1: Bestiario y Enciclopedia**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/bestiary/
├── Bestiary.java           # Modelo de bestiario del jugador
├── BestiaryEntry.java      # Entrada individual de mob en bestiario
├── BestiaryManager.java    # Gestor principal
└── BestiaryReward.java     # Modelo de recompensas
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/bestiary_config.json
{
  "discoveryRewards": {
    "firstKill": 100,          # XP por primera kill
    "tier1": 250,              # 25 kills
    "tier2": 500,              # 50 kills  
    "tier3": 1000,             # 100 kills
    "tier4": 2500              # 500 kills
  },
  "categories": {
    "undead": {
      "name": "No-muertos",
      "mobs": ["zombie_elite", "skeleton_archer"],
      "completionReward": {
        "title": "Cazador de No-muertos",
        "item": "legendary_sword",
        "xp": 5000
      }
    },
    "beasts": {
      "name": "Bestias",
      "mobs": ["spider_giant", "wolf_alpha"],
      "completionReward": {
        "title": "Domador de Bestias",
        "item": "beast_tamer_staff",
        "xp": 5000
      }
    }
  },
  "progressThresholds": [0, 25, 50, 75, 100, 500, 1000]
}
```

#### API REST
```python
# En /web/app.py

# GET - Obtener bestiario de jugador
@app.route('/api/rpg/bestiary/<player>', methods=['GET'])

# GET - Obtener configuración de bestiario
@app.route('/api/rpg/bestiary/config', methods=['GET'])

# PUT - Actualizar configuración de bestiario
@app.route('/api/rpg/bestiary/config', methods=['PUT'])

# GET - Estadísticas globales de bestiario
@app.route('/api/rpg/bestiary/stats', methods=['GET'])

# POST - Crear nueva categoría de bestiario
@app.route('/api/rpg/bestiary/category', methods=['POST'])

# DELETE - Eliminar categoría
@app.route('/api/rpg/bestiary/category/<id>', methods=['DELETE'])
```

#### **Nueva Pestaña: BESTIARIO** 📚
```
/web/templates/bestiary_panel.html
/web/static/bestiary.js
/web/static/bestiary.css
```

**Características de la pestaña:**
- **Sección 1: Configuración Global**
  - Input fields para XP rewards por tier
  - Toggle para habilitar/deshabilitar bestiario
  - Configuración de thresholds

- **Sección 2: Categorías**
  - Grid de categorías existentes (cards)
  - Botón "Nueva Categoría"
  - Por categoría: nombre, lista de mobs, recompensas
  - Botón editar/eliminar

- **Sección 3: Estadísticas en Vivo**
  - Top 10 jugadores por % completado
  - Categoría más popular
  - Total de descubrimientos hoy/semana/mes
  - Gráfico de progreso global

- **Sección 4: Vista de Jugador**
  - Buscador por nombre de jugador
  - Mostrar bestiario completo del jugador
  - Progress bars por categoría
  - Lista de mobs descubiertos/faltantes

---

### **Etapa 1.2: Achievements/Trofeos**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/achievements/
├── Achievement.java        # Modelo de achievement
├── AchievementCategory.java # Enum de categorías
├── AchievementManager.java  # Gestor principal
├── AchievementReward.java   # Recompensas
└── PlayerAchievements.java  # Logros del jugador
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/achievements_config.json
{
  "achievements": {
    "first_blood": {
      "name": "Primera Sangre",
      "description": "Mata tu primer mob",
      "category": "kills",
      "icon": "DIAMOND_SWORD",
      "secret": false,
      "requirements": {
        "type": "kill_any",
        "count": 1
      },
      "rewards": {
        "points": 10,
        "xp": 100,
        "title": "Novato",
        "broadcast": false
      }
    },
    "mob_hunter": {
      "name": "Cazador de Mobs",
      "description": "Mata 100 mobs",
      "category": "kills",
      "requirements": {
        "type": "kill_any",
        "count": 100
      },
      "rewards": {
        "points": 50,
        "xp": 1000,
        "item": "hunter_bow"
      }
    },
    "boss_slayer": {
      "name": "Asesino de Jefes",
      "description": "Mata tu primer boss",
      "category": "bosses",
      "requirements": {
        "type": "kill_boss",
        "count": 1
      },
      "rewards": {
        "points": 100,
        "xp": 5000,
        "title": "Slayer",
        "broadcast": true
      }
    }
  },
  "categories": ["kills", "bosses", "collection", "speed", "secret"],
  "pointsShop": {
    "cosmetic_wings": {
      "cost": 500,
      "item": "elytra",
      "enchantments": {"DURABILITY": 3}
    }
  }
}
```

#### API REST
```python
# GET - Listar todos los achievements
@app.route('/api/rpg/achievements', methods=['GET'])

# GET - Achievements de un jugador
@app.route('/api/rpg/achievements/<player>', methods=['GET'])

# POST - Crear nuevo achievement
@app.route('/api/rpg/achievements', methods=['POST'])

# PUT - Editar achievement
@app.route('/api/rpg/achievements/<id>', methods=['PUT'])

# DELETE - Eliminar achievement
@app.route('/api/rpg/achievements/<id>', methods=['DELETE'])

# GET - Tienda de puntos
@app.route('/api/rpg/achievements/shop', methods=['GET'])

# PUT - Actualizar tienda
@app.route('/api/rpg/achievements/shop', methods=['PUT'])
```

#### **Nueva Pestaña: LOGROS** 🏆
```
/web/templates/achievements_panel.html
/web/static/achievements.js
/web/static/achievements.css
```

**Características de la pestaña:**
- **Sección 1: Gestión de Logros**
  - Tabla de todos los achievements
  - Columnas: Nombre, Categoría, Requisitos, Recompensas, Secreto (checkbox)
  - Botón "Nuevo Logro"
  - Modal para crear/editar con formulario completo
  - Filtros por categoría

- **Sección 2: Tienda de Puntos**
  - Lista de items disponibles con su costo en puntos
  - Botón "Agregar Item a Tienda"
  - Edit/Delete por item

- **Sección 3: Estadísticas**
  - Logro más desbloqueado
  - Jugador con más puntos
  - Últimos 10 logros desbloqueados (live feed)
  - Distribución por categoría (gráfico pie)

---

### **Etapa 1.3: Rangos y Títulos**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/ranks/
├── Rank.java              # Modelo de rango
├── RankManager.java       # Gestor de rangos
├── Title.java             # Modelo de título
└── TitleManager.java      # Gestor de títulos
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/ranks_config.json
{
  "ranks": [
    {
      "id": "novato",
      "name": "Novato",
      "requiredKills": 0,
      "prefix": "&7[Novato]",
      "permissions": [],
      "tabColor": "GRAY"
    },
    {
      "id": "guerrero",
      "name": "Guerrero",
      "requiredKills": 500,
      "prefix": "&e[Guerrero]",
      "permissions": ["rpg.zone.intermediate"],
      "tabColor": "YELLOW"
    },
    {
      "id": "leyenda",
      "name": "Leyenda",
      "requiredKills": 5000,
      "prefix": "&6[Leyenda]",
      "permissions": ["rpg.zone.legendary", "rpg.fly"],
      "tabColor": "GOLD"
    }
  ],
  "titles": [
    {
      "id": "dragon_slayer",
      "name": "Asesino de Dragones",
      "displayName": "&4⚔ Asesino de Dragones ⚔",
      "requirement": "kill_boss_dragon",
      "unlocked": false
    },
    {
      "id": "undead_hunter",
      "name": "Cazador de No-muertos",
      "displayName": "&2☠ Cazador de No-muertos ☠",
      "requirement": "complete_bestiary_undead",
      "unlocked": false
    }
  ]
}
```

#### API REST
```python
# GET - Listar rangos
@app.route('/api/rpg/ranks', methods=['GET'])

# POST - Crear rango
@app.route('/api/rpg/ranks', methods=['POST'])

# PUT - Editar rango
@app.route('/api/rpg/ranks/<id>', methods=['PUT'])

# DELETE - Eliminar rango
@app.route('/api/rpg/ranks/<id>', methods=['DELETE'])

# GET - Listar títulos
@app.route('/api/rpg/titles', methods=['GET'])

# POST - Crear título
@app.route('/api/rpg/titles', methods=['POST'])

# PUT - Editar título
@app.route('/api/rpg/titles/<id>', methods=['PUT'])

# DELETE - Eliminar título
@app.route('/api/rpg/titles/<id>', methods=['DELETE'])

# GET - Leaderboard
@app.route('/api/rpg/leaderboard', methods=['GET'])
```

#### **Nueva Pestaña: RANGOS & TÍTULOS** 👑
```
/web/templates/ranks_panel.html
/web/static/ranks.js
/web/static/ranks.css
```

**Características de la pestaña:**
- **Sección 1: Gestión de Rangos**
  - Tabla de rangos ordenada por kills requeridos
  - Columnas: Nombre, Kills Req., Prefix, Permisos, Color TAB
  - Drag & drop para reordenar
  - Preview del prefix con colores
  - Botón "Nuevo Rango"

- **Sección 2: Gestión de Títulos**
  - Grid de títulos con cards
  - Preview del título formateado
  - Requisito para desbloquear
  - Toggle locked/unlocked
  - Botón "Nuevo Título"

- **Sección 3: Leaderboards**
  - Top 10 kills totales
  - Top por mob específico (selector)
  - Top XP ganado
  - Auto-refresh cada 30 segundos

---

## 🎪 MÓDULO 2: Contenido Dinámico y Eventos

### **Etapa 2.1: Sistema de Invasiones**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/invasions/
├── Invasion.java          # Modelo de invasión
├── InvasionManager.java   # Gestor y scheduler
├── InvasionWave.java      # Oleada individual
└── InvasionReward.java    # Recompensas
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/invasions_config.json
{
  "invasions": {
    "zombie_horde": {
      "name": "Horda Zombie",
      "enabled": true,
      "schedule": "RANDOM",  # RANDOM, HOURLY, DAILY, MANUAL
      "interval": 3600,      # Segundos entre invasiones
      "warningTime": 300,    # 5 min warning
      "duration": 900,       # 15 min duration
      "worlds": ["mundo", "mmorpg"],
      "waves": [
        {
          "waveNumber": 1,
          "mobs": [
            {"id": "zombie_elite", "count": 5}
          ],
          "spawnDelay": 0
        },
        {
          "waveNumber": 2,
          "mobs": [
            {"id": "zombie_elite", "count": 8},
            {"id": "zombie_bruja", "count": 2}
          ],
          "spawnDelay": 60
        },
        {
          "waveNumber": 5,
          "mobs": [
            {"id": "zombie_boss", "count": 1}
          ],
          "spawnDelay": 240,
          "isBoss": true
        }
      ],
      "rewards": {
        "participation": {
          "minKills": 5,
          "xp": 500,
          "coins": 100
        },
        "completion": {
          "xp": 2000,
          "coins": 500,
          "items": ["invasion_trophy"]
        },
        "mvp": {
          "xp": 5000,
          "coins": 1000,
          "items": ["legendary_sword", "mvp_crown"]
        }
      }
    }
  },
  "globalSettings": {
    "enabled": true,
    "broadcastMessages": true,
    "xpMultiplier": 2.0,
    "maxConcurrentInvasions": 2
  }
}
```

#### API REST
```python
# GET - Listar invasiones
@app.route('/api/rpg/invasions', methods=['GET'])

# POST - Crear invasión
@app.route('/api/rpg/invasions', methods=['POST'])

# PUT - Editar invasión
@app.route('/api/rpg/invasions/<id>', methods=['PUT'])

# DELETE - Eliminar invasión
@app.route('/api/rpg/invasions/<id>', methods=['DELETE'])

# POST - Forzar inicio de invasión
@app.route('/api/rpg/invasions/<id>/start', methods=['POST'])

# POST - Detener invasión activa
@app.route('/api/rpg/invasions/<id>/stop', methods=['POST'])

# GET - Estado actual de invasiones
@app.route('/api/rpg/invasions/active', methods=['GET'])

# GET - Historial de invasiones
@app.route('/api/rpg/invasions/history', methods=['GET'])
```

#### **Nueva Pestaña: INVASIONES** 🚨
```
/web/templates/invasions_panel.html
/web/static/invasions.js
/web/static/invasions.css
```

**Características de la pestaña:**
- **Sección 1: Invasiones Activas** (Live)
  - Card por invasión activa mostrando:
    - Tiempo restante (countdown)
    - Oleada actual / total
    - Participantes online
    - Kills totales
    - Botón "Detener Invasión"
  - Auto-refresh cada 5 segundos

- **Sección 2: Gestión de Invasiones**
  - Tabla de invasiones configuradas
  - Toggle enabled/disabled
  - Columnas: Nombre, Schedule, Mundos, Oleadas, Recompensas
  - Botón "Nueva Invasión"
  - Botón "Forzar Inicio" por invasión

- **Sección 3: Editor de Invasión** (Modal)
  - Configuración general (nombre, schedule, mundos)
  - Editor de oleadas (agregar/quitar oleadas)
  - Por oleada: mobs, cantidad, delay
  - Configuración de recompensas (participation, completion, MVP)

- **Sección 4: Historial**
  - Tabla de invasiones pasadas
  - Columnas: Fecha, Invasión, Participantes, Kills, Resultado
  - Filtros por fecha/invasión
  - Exportar a CSV

---

### **Etapa 2.2: Eventos Temáticos**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/events/
├── ThematicEvent.java     # Modelo de evento
├── EventManager.java      # Gestor de eventos
├── EventReward.java       # Recompensas de evento
└── EventQuest.java        # Quests de evento
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/events_config.json
{
  "events": {
    "halloween_2025": {
      "name": "Halloween 2025",
      "enabled": true,
      "startDate": "2025-10-25T00:00:00",
      "endDate": "2025-11-01T23:59:59",
      "theme": "HALLOWEEN",
      "description": "Evento especial de Halloween con mobs espectrales",
      "mobs": [
        {
          "id": "ghost_zombie",
          "spawnWeight": 0.3,
          "exclusive": true
        },
        {
          "id": "pumpkin_king",
          "spawnWeight": 0.05,
          "exclusive": true,
          "isBoss": true
        }
      ],
      "items": [
        {
          "id": "halloween_candy",
          "dropChance": 0.8
        },
        {
          "id": "cursed_pumpkin",
          "dropChance": 0.1
        }
      ],
      "eventZones": [
        {
          "world": "mundo",
          "x": 0, "y": 100, "z": 0,
          "radius": 50,
          "decoration": "PUMPKINS_AND_COBWEBS"
        }
      ],
      "quests": [
        "halloween_candy_collector",
        "pumpkin_king_slayer"
      ],
      "shop": {
        "enabled": true,
        "currency": "halloween_candy",
        "items": {
          "witch_hat": {"cost": 50},
          "ghost_sword": {"cost": 100}
        }
      },
      "rewards": {
        "participation": {
          "minPlaytime": 3600,
          "xp": 5000,
          "title": "Celebrante de Halloween"
        }
      }
    }
  }
}
```

#### API REST
```python
# GET - Listar eventos
@app.route('/api/rpg/events', methods=['GET'])

# GET - Evento activo actual
@app.route('/api/rpg/events/active', methods=['GET'])

# POST - Crear evento
@app.route('/api/rpg/events', methods=['POST'])

# PUT - Editar evento
@app.route('/api/rpg/events/<id>', methods=['PUT'])

# DELETE - Eliminar evento
@app.route('/api/rpg/events/<id>', methods=['DELETE'])

# POST - Activar evento manualmente
@app.route('/api/rpg/events/<id>/activate', methods=['POST'])

# POST - Desactivar evento
@app.route('/api/rpg/events/<id>/deactivate', methods=['POST'])
```

#### **Nueva Pestaña: EVENTOS** 🎉
```
/web/templates/events_panel.html
/web/static/events.js
/web/static/events.css
```

**Características de la pestaña:**
- **Sección 1: Evento Activo**
  - Card destacado del evento actual
  - Countdown hasta finalización
  - Participantes únicos
  - Estadísticas en vivo (kills de mobs de evento, items dropeados)
  - Botón "Desactivar Evento"

- **Sección 2: Calendario de Eventos**
  - Vista de calendario mensual
  - Eventos próximos marcados
  - Al click: detalles del evento
  - Botón "Nuevo Evento"

- **Sección 3: Gestión de Eventos**
  - Tabla de todos los eventos
  - Columnas: Nombre, Fechas, Estado, Mobs, Items
  - Toggle enabled/disabled
  - Botón "Activar Ahora" (override fechas)

- **Sección 4: Editor de Evento** (Modal)
  - Configuración general (nombre, fechas, descripción)
  - Selección de mobs exclusivos
  - Configuración de items de evento
  - Definir zonas decoradas
  - Quests asociadas
  - Configurar shop de evento

---

### **Etapa 2.3: Jefes de Cuadrilla**

#### Componentes Backend
```
/mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/squads/
├── Squad.java             # Modelo de cuadrilla
├── SquadManager.java      # Gestor de cuadrillas
├── SquadMember.java       # Miembro individual
└── SquadAI.java           # Comportamiento cooperativo
```

#### Configuración Editable
```json
/plugins/MMORPGPlugin/data/squads_config.json
{
  "squads": {
    "bandit_squad": {
      "name": "Cuadrilla de Bandidos",
      "enabled": true,
      "members": [
        {
          "mobId": "bandit_tank",
          "role": "TANK",
          "position": "FRONT"
        },
        {
          "mobId": "bandit_archer",
          "role": "DPS",
          "position": "BACK"
        },
        {
          "mobId": "bandit_rogue",
          "role": "DPS",
          "position": "FLANK"
        },
        {
          "mobId": "bandit_healer",
          "role": "SUPPORT",
          "position": "BACK"
        }
      ],
      "ai": {
        "cooperation": true,
        "protectWeakest": true,
        "focusFire": true,
        "enrageOnDeath": true,
        "enrageDamageBonus": 1.5
      },
      "spawnLocations": [
        {"world": "mundo", "x": 100, "y": 70, "z": 100}
      ],
      "spawnConditions": {
        "manual": false,
        "event": "invasion",
        "respawn": false
      },
      "rewards": {
        "xpBonus": 2.5,
        "guaranteedDrop": {
          "type": "SET",
          "items": ["bandit_helmet", "bandit_chestplate", "bandit_leggings", "bandit_boots"]
        },
        "achievement": "squad_wiper"
      }
    }
  }
}
```

#### API REST
```python
# GET - Listar cuadrillas
@app.route('/api/rpg/squads', methods=['GET'])

# POST - Crear cuadrilla
@app.route('/api/rpg/squads', methods=['POST'])

# PUT - Editar cuadrilla
@app.route('/api/rpg/squads/<id>', methods=['PUT'])

# DELETE - Eliminar cuadrilla
@app.route('/api/rpg/squads/<id>', methods=['DELETE'])

# POST - Spawnear cuadrilla manualmente
@app.route('/api/rpg/squads/<id>/spawn', methods=['POST'])

# GET - Cuadrillas activas
@app.route('/api/rpg/squads/active', methods=['GET'])
```

#### **Nueva Pestaña: CUADRILLAS** 🛡️
```
/web/templates/squads_panel.html
/web/static/squads.js
/web/static/squads.css
```

**Características de la pestaña:**
- **Sección 1: Cuadrillas Activas**
  - Lista de cuadrillas spawneadas
  - Por cuadrilla: ubicación, miembros vivos/muertos, tiempo vivo
  - Botón "Teleport Admin" (comando)

- **Sección 2: Gestión de Cuadrillas**
  - Grid de cuadrillas configuradas
  - Card por cuadrilla mostrando:
    - Nombre y composición visual (iconos de roles)
    - Configuración de IA
    - Condiciones de spawn
    - Recompensas
  - Botón "Nueva Cuadrilla"
  - Botón "Spawn Ahora" por cuadrilla

- **Sección 3: Editor de Cuadrilla** (Modal)
  - Configuración general
  - Builder de composición:
    - Agregar miembro (selector de mob + rol + posición)
    - Drag & drop para reordenar
    - Preview visual de formación
  - Configuración de IA (checkboxes y sliders)
  - Spawn locations (mapa interactivo)
  - Configuración de recompensas

---

## 🔗 MÓDULO 3: Integración y Expansión

### **Etapa 3.1: Sistema de Crafting**

#### **Nueva Pestaña: CRAFTEO** ⚒️
```json
/plugins/MMORPGPlugin/data/crafting_recipes.json
```

**Endpoints:**
- GET/POST/PUT/DELETE `/api/rpg/crafting/recipes`

**Pestaña incluye:**
- Editor de recetas con grid 3x3
- Selector de ingredientes
- Configuración de output (item + cantidad + stats)
- Lista de recetas existentes

---

### **Etapa 3.2: Encantamientos Custom**

#### **Nueva Pestaña: ENCANTAMIENTOS** ✨
```json
/plugins/MMORPGPlugin/data/enchantments_config.json
```

**Endpoints:**
- GET/POST/PUT/DELETE `/api/rpg/enchantments`

**Pestaña incluye:**
- Lista de encantamientos custom
- Configuración de niveles y efectos
- Incompatibilidades
- Probabilidades de drop

---

### **Etapa 3.3: Mascotas y Monturas**

#### **Nueva Pestaña: MASCOTAS** 🐾
```json
/plugins/MMORPGPlugin/data/pets_config.json
/plugins/MMORPGPlugin/data/mounts_config.json
```

**Endpoints:**
- GET/POST/PUT/DELETE `/api/rpg/pets`
- GET/POST/PUT/DELETE `/api/rpg/mounts`

**Pestaña incluye:**
- Dos sub-tabs: Mascotas | Monturas
- Configuración de stats
- Drops de bosses
- Sistema de niveles

---

### **Etapa 3.4: Dungeons**

#### **Nueva Pestaña: DUNGEONS** 🏰
```json
/plugins/MMORPGPlugin/data/dungeons_config.json
```

**Endpoints:**
- GET/POST/PUT/DELETE `/api/rpg/dungeons`

**Pestaña incluye:**
- Configuración de dungeons
- Templates de generación
- Tabla de loot
- Instancias activas (monitor)

---

## ⚙️ MÓDULO 4: Optimización y Producción

### **Etapa 4.1-4.4: No requieren pestañas**
Estas etapas son de testing, optimización y documentación, no tienen configuración editable por el usuario.

---

## 📊 Resumen de Nuevas Pestañas

| # | Pestaña | Módulo | Configuración JSON | Endpoints API |
|---|---------|--------|-------------------|---------------|
| 1 | **Bestiario** | M1.1 | bestiary_config.json | 6 endpoints |
| 2 | **Logros** | M1.2 | achievements_config.json | 6 endpoints |
| 3 | **Rangos & Títulos** | M1.3 | ranks_config.json | 8 endpoints |
| 4 | **Invasiones** | M2.1 | invasions_config.json | 8 endpoints |
| 5 | **Eventos** | M2.2 | events_config.json | 7 endpoints |
| 6 | **Cuadrillas** | M2.3 | squads_config.json | 6 endpoints |
| 7 | **Crafteo** | M3.1 | crafting_recipes.json | 4 endpoints |
| 8 | **Encantamientos** | M3.2 | enchantments_config.json | 4 endpoints |
| 9 | **Mascotas** | M3.3 | pets_config.json + mounts_config.json | 8 endpoints |
| 10 | **Dungeons** | M3.4 | dungeons_config.json | 4 endpoints |

**TOTAL: 10 NUEVAS PESTAÑAS + 61 ENDPOINTS**

---

## 🎯 Orden de Implementación

Seguiremos este orden estricto:

1. **MÓDULO 1 - Etapa 1.1:** Bestiario → Pestaña + Backend + API
2. **MÓDULO 1 - Etapa 1.2:** Logros → Pestaña + Backend + API
3. **MÓDULO 1 - Etapa 1.3:** Rangos & Títulos → Pestaña + Backend + API
4. **MÓDULO 2 - Etapa 2.1:** Invasiones → Pestaña + Backend + API
5. **MÓDULO 2 - Etapa 2.2:** Eventos → Pestaña + Backend + API
6. **MÓDULO 2 - Etapa 2.3:** Cuadrillas → Pestaña + Backend + API
7. **MÓDULO 3 - Etapa 3.1:** Crafteo → Pestaña + Backend + API
8. **MÓDULO 3 - Etapa 3.2:** Encantamientos → Pestaña + Backend + API
9. **MÓDULO 3 - Etapa 3.3:** Mascotas → Pestaña + Backend + API
10. **MÓDULO 3 - Etapa 3.4:** Dungeons → Pestaña + Backend + API
11. **MÓDULO 4:** Testing y Optimización

---

## ✅ Checklist por Etapa

Para cada etapa, completaremos en orden:

- [ ] Crear clases Java (modelo + manager)
- [ ] Crear archivo de configuración JSON con ejemplos
- [ ] Implementar API REST endpoints
- [ ] Crear template HTML de la pestaña
- [ ] Crear JavaScript con lógica de UI
- [ ] Crear CSS para estilos
- [ ] Integrar pestaña en menú RPG principal
- [ ] Testear CRUD completo
- [ ] Documentar en guía correspondiente
- [ ] Commit con mensaje descriptivo

---

**¿Listo para comenzar con MÓDULO 1 - Etapa 1.1: Bestiario?**
