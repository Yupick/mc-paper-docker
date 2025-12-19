# 📚 ETAPA 4 - Sistema Integrado de Mobs, Loot y Respawn

## Tabla de Contenidos
1. [Visión General](#visión-general)
2. [Componentes Principales](#componentes-principales)
3. [Configuración](#configuración)
4. [API REST](#api-rest)
5. [Ejemplos de Uso](#ejemplos-de-uso)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Visión General

**Etapa 4** integra 4 subsistemas principales para crear un ecosistema completo de mobs RPG:

1. **Spawn Command** - Spawning manual de mobs con `/rpg mob spawn`
2. **Loot System** - Items RPG con raridades que dropean automáticamente
3. **Kill Tracking** - Dashboard web con estadísticas de kills
4. **Respawn System** - Zonas de respawn automático configurables

### Características Principales

| Característica | Descripción |
|---|---|
| **Mobs Personalizados** | Stats customizables (health, damage, defense, level) |
| **Sistema de Items** | 16 items RPG con atributos y 4 raridades |
| **Auto-Drops** | Items dropean automáticamente en kills |
| **Respawn por Zonas** | Farmeos, dungeons, y arenas con respawn automático |
| **UI Web** | Dashboards para kills y respawn management |
| **Tracking Persistente** | Estadísticas guardadas en JSON |

---

## Componentes Principales

### 1. Mobs Personalizados

**Archivo:** `plugins/MMORPGPlugin/data/mobs.json`

```json
{
  "zombie_elite": {
    "name": "Zombie Elite",
    "entityType": "ZOMBIE",
    "health": 50,
    "damage": 8,
    "defense": 3,
    "level": 10,
    "experienceReward": 250,
    "isBoss": false,
    "drops": [
      {
        "itemType": "iron_sword",
        "minAmount": 1,
        "maxAmount": 1,
        "dropChance": 0.35
      }
    ]
  }
}
```

**Stats disponibles:**
- `health`: Puntos de vida del mob
- `damage`: Daño que inflinge
- `defense`: Armadura (reducción de daño)
- `level`: Nivel para escalado de dificultad
- `experienceReward`: XP que da al morir

### 2. Sistema de Items

**Archivo:** `plugins/MMORPGPlugin/data/items.json`

```json
{
  "rarities": {
    "COMMON": {
      "dropChance": 1.0,
      "attributeMultiplier": 1.0,
      "color": "#FFFFFFff"
    },
    "RARE": {
      "dropChance": 0.35,
      "attributeMultiplier": 1.3,
      "color": "#00D4FFff"
    },
    "EPIC": {
      "dropChance": 0.08,
      "attributeMultiplier": 1.6,
      "color": "#AA00FFff"
    },
    "LEGENDARY": {
      "dropChance": 0.01,
      "attributeMultiplier": 2.0,
      "color": "#FFD700ff"
    }
  },
  "items": {
    "iron_sword": {
      "name": "Iron Sword",
      "material": "IRON_SWORD",
      "rarity": "COMMON",
      "attributes": {
        "damage": 8
      }
    }
  }
}
```

**Raridades disponibles:**
- **COMMON** (100% drop) - 1.0x atributos
- **RARE** (35% drop) - 1.3x atributos
- **EPIC** (8% drop) - 1.6x atributos
- **LEGENDARY** (1% drop) - 2.0x atributos

### 3. Zonas de Respawn

**Archivo:** `plugins/MMORPGPlugin/data/respawn_config.json`

```json
{
  "respawnZones": {
    "farmeo_zombies": {
      "name": "Zona de Farmeo - Zombies",
      "type": "farmeo",
      "world": "mundo",
      "mobIds": ["zombie_elite", "zombie_bruja"],
      "spawnLocations": [
        {"x": 0, "y": 100, "z": 0},
        {"x": 50, "y": 100, "z": 50}
      ],
      "maxMobs": 10,
      "respawnInterval": 60,
      "enabled": true
    }
  },
  "globalSettings": {
    "respawnEnabled": true,
    "checkInterval": 20,
    "logRespawns": true
  }
}
```

**Tipos de Zona:**
- **farmeo** - Granja de recursos/XP
- **dungeon** - Dungeon con dificultad progresiva
- **boss_arena** - Arena para bosses especiales

---

## Configuración

### Agregar un Nuevo Mob

1. **Editar `mobs.json`:**
```json
{
  "skeleton_archer": {
    "name": "Skeleton Archer",
    "entityType": "SKELETON",
    "health": 20,
    "damage": 12,
    "defense": 2,
    "level": 8,
    "experienceReward": 150,
    "isBoss": false,
    "drops": [
      {
        "itemType": "arrow",
        "minAmount": 5,
        "maxAmount": 15,
        "dropChance": 0.8
      }
    ]
  }
}
```

2. **Verificar en servidor:**
```
/rpg mob spawn skeleton_archer mundo 0 100 0
```

### Agregar Nuevo Item RPG

1. **Editar `items.json`:**
```json
{
  "items": {
    "legendary_axe": {
      "name": "Legendary Axe",
      "material": "DIAMOND_AXE",
      "rarity": "LEGENDARY",
      "attributes": {
        "damage": 15,
        "defense": 3
      },
      "enchantments": {
        "SHARPNESS": 3,
        "KNOCKBACK": 2
      }
    }
  }
}
```

2. **Agregar a drop de mob:**
```json
{
  "itemType": "legendary_axe",
  "minAmount": 1,
  "maxAmount": 1,
  "dropChance": 0.02
}
```

### Crear Nueva Zona de Respawn

1. **Editar `respawn_config.json`:**
```json
{
  "respawnZones": {
    "dungeon_new": {
      "name": "Dungeon Oscuro",
      "type": "dungeon",
      "world": "dungeon_world",
      "mobIds": ["skeleton_warrior", "spider_giant"],
      "spawnLocations": [
        {"x": 100, "y": 50, "z": 100},
        {"x": 150, "y": 50, "z": 100},
        {"x": 100, "y": 50, "z": 150}
      ],
      "maxMobs": 15,
      "respawnInterval": 45,
      "enabled": true
    }
  }
}
```

2. **Acceder a dashboard:** `http://localhost:8080/respawn`

---

## API REST

### Kill Tracking

```http
GET /api/rpg/stats/kills?player=PlayerName&mob=zombie_elite
```

**Response:**
```json
{
  "success": true,
  "kills": [
    {
      "playerName": "PlayerName",
      "mobName": "Zombie Elite",
      "xpReward": 250,
      "world": "mundo",
      "timestamp": "2025-12-04T17:30:00"
    }
  ],
  "summary": {
    "playerStats": {
      "PlayerName": {
        "totalKills": 15,
        "totalXpGained": 3750,
        "lastKillTime": "2025-12-04T17:30:00",
        "killsByMob": {
          "zombie_elite": 10,
          "skeleton_archer": 5
        }
      }
    }
  }
}
```

### Estadísticas por Mob

```http
GET /api/rpg/stats/mobs
```

**Response:**
```json
{
  "success": true,
  "mobStats": {
    "zombie_elite": {
      "totalKills": 25,
      "totalXpDropped": 6250,
      "playersKilled": ["PlayerOne", "PlayerTwo"],
      "averageXpPerKill": 250
    }
  }
}
```

### Timeline

```http
GET /api/rpg/stats/timeline?player=PlayerName
```

**Response:**
```json
{
  "success": true,
  "timeline": [
    {"date": "2025-12-01", "kills": 5, "xp": 1250},
    {"date": "2025-12-02", "kills": 8, "xp": 2000},
    {"date": "2025-12-03", "kills": 12, "xp": 3000}
  ]
}
```

### Respawn Zones

```http
GET /api/rpg/respawn/zones
```

```http
PUT /api/rpg/respawn/zones/farmeo_zombies
Content-Type: application/json

{
  "enabled": true,
  "maxMobs": 12,
  "respawnInterval": 50
}
```

---

## Ejemplos de Uso

### Ejemplo 1: Sistema Completo de Farmeo

**Objetivo:** Crear una granja de zombies donde jugadores ganan XP y items

**Paso 1: Configurar Mob**
```json
{
  "farm_zombie": {
    "name": "Farm Zombie",
    "entityType": "ZOMBIE",
    "health": 30,
    "damage": 5,
    "defense": 1,
    "level": 5,
    "experienceReward": 100,
    "drops": [
      {
        "itemType": "rotten_flesh",
        "minAmount": 1,
        "maxAmount": 3,
        "dropChance": 0.9
      },
      {
        "itemType": "iron_sword",
        "minAmount": 1,
        "maxAmount": 1,
        "dropChance": 0.2
      }
    ]
  }
}
```

**Paso 2: Crear Zona**
```json
{
  "respawnZones": {
    "zombie_farm": {
      "name": "Zombie Farm",
      "type": "farmeo",
      "world": "mundo",
      "mobIds": ["farm_zombie"],
      "spawnLocations": [
        {"x": -200, "y": 70, "z": 0},
        {"x": -180, "y": 70, "z": 0},
        {"x": -200, "y": 70, "z": 20}
      ],
      "maxMobs": 8,
      "respawnInterval": 30,
      "enabled": true
    }
  }
}
```

**Paso 3: Verificar en Panel**
- Ir a `http://localhost:8080/respawn`
- Ver zona "Zombie Farm"
- Matar zombies y ver stats en `http://localhost:8080/kills`

---

### Ejemplo 2: Dungeon Progresivo

**Objetivo:** Dungeon con mobs cada vez más fuertes

```json
{
  "skeleton_weak": {
    "name": "Skeleton Débil",
    "entityType": "SKELETON",
    "health": 15,
    "damage": 5,
    "level": 3,
    "experienceReward": 75
  },
  "skeleton_strong": {
    "name": "Skeleton Fuerte",
    "entityType": "SKELETON",
    "health": 40,
    "damage": 10,
    "level": 8,
    "experienceReward": 250
  },
  "dungeon_boss": {
    "name": "Skeleton King",
    "entityType": "SKELETON",
    "health": 100,
    "damage": 15,
    "level": 15,
    "experienceReward": 1000,
    "isBoss": true,
    "drops": [
      {
        "itemType": "legendary_sword",
        "dropChance": 0.5
      }
    ]
  }
}
```

```json
{
  "respawnZones": {
    "dark_dungeon": {
      "name": "Dark Dungeon",
      "type": "dungeon",
      "world": "dungeon",
      "mobIds": ["skeleton_weak", "skeleton_strong", "dungeon_boss"],
      "spawnLocations": [
        {"x": 0, "y": 30, "z": 0},
        {"x": 50, "y": 30, "z": 50}
      ],
      "maxMobs": 20,
      "respawnInterval": 60,
      "enabled": true
    }
  }
}
```

---

## Best Practices

### 1. Balance de Mobs

```
Regla general: XP = health * 10

Ejemplo:
- Mob débil (10 HP) → 100 XP
- Mob fuerte (50 HP) → 500 XP
- Boss (150 HP) → 1500 XP
```

### 2. Probabilidades de Drop

```
COMMON:    100% drop chance → Siempre
RARE:       35% drop chance → 1/3 veces
EPIC:        8% drop chance → 1/12 veces
LEGENDARY:   1% drop chance → 1/100 veces
```

### 3. Configuración de Zonas

| Tipo | maxMobs | respawnInterval | Uso |
|---|---|---|---|
| Farmeo | 6-10 | 30-60s | Grinding afk seguro |
| Dungeon | 10-20 | 45-90s | Desafío equilibrado |
| Boss | 1-2 | 300-600s | Encuentros especiales |

### 4. Atributos de Items

```json
{
  "attributes": {
    "damage": 8,        // Daño adicional
    "defense": 3,       // Reducción de daño
    "health": 5,        // Vida adicional
    "speed": 0.1,       // Velocidad (0-1 scale)
    "knockback": 2      // Knockback adicional
  }
}
```

### 5. Naming Convention

```
Mobs:       snake_case (zombie_elite, spider_giant)
Items:      snake_case (iron_sword, diamond_helmet)
Zones:      snake_case (farmeo_zombies, dungeon_dark)
```

---

## Troubleshooting

### Problema: Los mobs no spawnean

**Solución:**
1. Verificar que el mundo existe: `/world list`
2. Verificar coordenadas en respawn_config.json
3. Comprobar que spawn location tiene Y válido
4. Ver logs del servidor: `docker logs minecraft-paper | grep MMORPG`

### Problema: No dropean items

**Solución:**
1. Verificar que `dropChance` > 0
2. Asegurar que el itemId existe en items.json
3. Revisar que el mob tiene drops configurados
4. Checar logs: `[MMORPG] Item RPG no encontrado: ...`

### Problema: Respawn muy lento

**Solución:**
1. Reducir `respawnInterval` en respawn_config.json
2. Aumentar `maxMobs` si se pueden agregar más
3. Verificar que hay suficientes spawnLocations
4. Reducir checkInterval en globalSettings (mínimo 5)

### Problema: Panel web no muestra estadísticas

**Solución:**
1. Verificar que servidor web está corriendo
2. Checar que panel está en localhost:8080
3. Revisar kills_tracker.json existe en /web/
4. Ir a panel → F12 → Console para ver errores

### Problema: Zona deshabilitada no se habilita

**Solución:**
1. Editar respawn_config.json manualmente
2. Cambiar `"enabled": false` a `"enabled": true`
3. Reiniciar servidor Minecraft
4. O usar UI web → toggle button en zona

---

## Comandos Útiles

```bash
# Spawnar mob manual
/rpg mob spawn zombie_elite mundo 0 100 0

# Ver stats de jugador
# (Disponible en panel web: /kills)

# Administrar respawn
# (Disponible en panel web: /respawn)

# Ver logs del plugin
docker logs minecraft-paper | grep MMORPG

# Acceder a dashboards
# Panel de kills: http://localhost:8080/kills
# Panel de respawn: http://localhost:8080/respawn
```

---

## Notas Importantes

⚠️ **IMPORTANTE:** 
- Los cambios en JSON requieren reinicio del servidor
- Usar respawn_config.json para configuración persistente
- Hacer backup de data/mobs.json y data/items.json
- El respawn se activa cada 1 segundo (optimizado)

✅ **VERIFICACIÓN:**
```bash
# Compilar plugin
mvn clean package

# Copiar JAR
cp mmorpg-plugin/target/mmorpg-plugin-1.0.0.jar plugins/

# Reiniciar servidor
docker-compose down && docker-compose up -d

# Ver en logs
docker logs minecraft-paper | tail -50
```

---

**Documentación Etapa 4** • Proyecto: Minecraft MMORPG • Última actualización: 4 de Diciembre 2025
