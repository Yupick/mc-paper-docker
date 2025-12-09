# 🐲 Guía de Mobs Custom - Sistema MMORPG

## Introducción

El sistema de mobs custom permite crear entidades personalizadas con stats RPG (nivel, health, damage, defense, XP reward) y drops configurables. Esta guía describe la librería estándar de 21 mobs disponibles.

---

## 📊 Categorías de Mobs

### **Mobs Básicos** (Niveles 4-13)
Mobs con dificultad estándar, ideales para early-mid game.

| ID | Nombre | Tipo | Nivel | HP | Daño | Def | XP |
|---|---|---|---|---|---|---|---|
| `zombie_warrior` | Guerrero Zombie | ZOMBIE | 5 | 30 | 4.5 | 2.0 | 150 |
| `skeleton_archer` | Arquero Esqueleto | SKELETON | 6 | 24 | 5.0 | 1.0 | 175 |
| `creeper_explosive` | Creeper Explosivo | CREEPER | 4 | 20 | 8.0 | 0.5 | 120 |
| `spider_hunter` | Araña Cazadora | SPIDER | 4 | 22 | 3.5 | 1.5 | 110 |
| `slime_king` | Rey Slime | SLIME | 7 | 35 | 5.5 | 2.5 | 200 |
| `dark_witch` | Bruja Oscura | WITCH | 8 | 26 | 3.5 | 3.0 | 250 |
| `phantom_night_terror` | Terror Nocturno | PHANTOM | 8 | 28 | 6.5 | 1.0 | 220 |
| `enderman_shadow` | Enderman Sombrío | ENDERMAN | 9 | 40 | 7.0 | 3.0 | 300 |
| `ghast_phantom` | Ghast Fantasma | GHAST | 10 | 50 | 8.5 | 1.0 | 350 |
| `ice_golem` | Gólem de Hielo | IRON_GOLEM | 10 | 100 | 8.0 | 6.0 | 400 |
| `blaze_inferno` | Blaze Infernal | BLAZE | 11 | 45 | 9.0 | 2.5 | 450 |
| `piglin_brute_elite` | Piglin Bruto de Élite | PIGLIN_BRUTE | 12 | 80 | 10.0 | 5.0 | 500 |
| `wither_skeleton_knight` | Caballero Wither | WITHER_SKELETON | 13 | 60 | 11.0 | 4.0 | 550 |

### **Mobs de Élite** (Niveles 14-17)
Mobs con stats superiores y mejores drops, ideales para mid-late game.

| ID | Nombre | Tipo | Nivel | HP | Daño | Def | XP |
|---|---|---|---|---|---|---|---|
| `elite_ravager` | Asolador de Élite | RAVAGER | 14 | 120 | 13.0 | 7.0 | 800 |
| `elite_vindicator` | Vengador de Élite | VINDICATOR | 13 | 90 | 12.0 | 5.5 | 700 |
| `elite_evoker` | Evocador de Élite | EVOKER | 15 | 100 | 11.0 | 6.0 | 900 |
| `elite_guardian` | Guardián Antiguo de Élite | ELDER_GUARDIAN | 16 | 130 | 14.0 | 8.0 | 1000 |
| `elite_shulker` | Shulker de Élite | SHULKER | 17 | 110 | 10.0 | 9.0 | 1100 |

### **Bosses** (Niveles 18-20)
Mobs de jefe con stats extremos y drops legendarios. Requieren grupos para derrotar.

| ID | Nombre | Tipo | Nivel | HP | Daño | Def | XP |
|---|---|---|---|---|---|---|---|
| `necromancer_king` | Rey Necrómante | WITHER | 18 | 150 | 12.0 | 8.0 | 4000 |
| `arctic_titan` | Titán Ártico | IRON_GOLEM | 19 | 180 | 14.0 | 12.0 | 4500 |
| `corrupted_dragon` | Dragón Corrupto | ENDER_DRAGON | 20 | 200 | 15.0 | 10.0 | 5000 |

---

## 💎 Tabla de Drops por Mob

### Mobs Básicos

**Guerrero Zombie** (`zombie_warrior`)
- `ROTTEN_FLESH` x1-3 (100%)
- `IRON_INGOT` x0-1 (25%)

**Arquero Esqueleto** (`skeleton_archer`)
- `BONE` x1-5 (100%)
- `ARROW` x3-8 (60%)
- `STRING` x0-2 (30%)

**Creeper Explosivo** (`creeper_explosive`)
- `GUNPOWDER` x1-4 (100%)
- `TNT` x0-1 (15%)

**Araña Cazadora** (`spider_hunter`)
- `STRING` x2-5 (100%)
- `SPIDER_EYE` x1-2 (70%)

**Rey Slime** (`slime_king`)
- `SLIME_BALL` x3-8 (100%)
- `EMERALD` x0-1 (10%)

**Bruja Oscura** (`dark_witch`)
- `REDSTONE` x2-5 (50%)
- `GLOWSTONE_DUST` x1-3 (40%)
- `POTION` x1 (25%)
- `AMETHYST_SHARD` x0-1 (15%)

**Terror Nocturno** (`phantom_night_terror`)
- `PHANTOM_MEMBRANE` x1-3 (100%)
- `FEATHER` x2-5 (50%)

**Enderman Sombrío** (`enderman_shadow`)
- `ENDER_PEARL` x1-2 (100%)
- `OBSIDIAN` x0-1 (25%)

**Ghast Fantasma** (`ghast_phantom`)
- `GHAST_TEAR` x1-2 (100%)
- `GUNPOWDER` x2-4 (70%)

**Gólem de Hielo** (`ice_golem`)
- `IRON_INGOT` x3-8 (100%)
- `SNOWBALL` x10-20 (50%)
- `BLUE_ICE` x1-3 (30%)

**Blaze Infernal** (`blaze_inferno`)
- `BLAZE_ROD` x1-3 (100%)
- `FIRE_CHARGE` x2-5 (60%)

**Piglin Bruto de Élite** (`piglin_brute_elite`)
- `GOLD_INGOT` x3-7 (100%)
- `GOLDEN_AXE` x0-1 (30%)
- `CRYING_OBSIDIAN` x0-1 (20%)

**Caballero Wither** (`wither_skeleton_knight`)
- `BONE` x2-6 (100%)
- `COAL` x3-8 (80%)
- `WITHER_SKELETON_SKULL` x0-1 (5%)

### Mobs de Élite

**Asolador de Élite** (`elite_ravager`)
- `EMERALD` x2-5 (100%)
- `SADDLE` x0-1 (40%)
- `DIAMOND` x1-3 (30%)

**Vengador de Élite** (`elite_vindicator`)
- `EMERALD` x2-4 (100%)
- `IRON_AXE` x0-1 (50%)
- `TOTEM_OF_UNDYING` x0-1 (5%)

**Evocador de Élite** (`elite_evoker`)
- `EMERALD` x3-6 (100%)
- `TOTEM_OF_UNDYING` x1 (25%)
- `ENCHANTED_BOOK` x0-1 (40%)

**Guardián Antiguo de Élite** (`elite_guardian`)
- `PRISMARINE_SHARD` x5-10 (100%)
- `PRISMARINE_CRYSTALS` x3-7 (80%)
- `SPONGE` x1-2 (60%)
- `DIAMOND` x1-3 (40%)

**Shulker de Élite** (`elite_shulker`)
- `SHULKER_SHELL` x1-3 (100%)
- `ENDER_PEARL` x2-4 (70%)
- `DIAMOND` x1-2 (35%)

### Bosses

**Rey Necrómante** (`necromancer_king`) - Boss Nivel 18
- `NETHER_STAR` x1 (100%)
- `DIAMOND` x5-10 (100%)
- `GOLD_INGOT` x5-15 (90%)
- `ENCHANTED_GOLDEN_APPLE` x1-2 (60%)

**Titán Ártico** (`arctic_titan`) - Boss Nivel 19
- `IRON_BLOCK` x3-6 (100%)
- `DIAMOND` x4-8 (100%)
- `BLUE_ICE` x5-10 (100%)
- `NETHERITE_SCRAP` x1-3 (40%)

**Dragón Corrupto** (`corrupted_dragon`) - Boss Nivel 20
- `DRAGON_BREATH` x1 (100%)
- `DIAMOND` x3-8 (100%)
- `EMERALD` x2-5 (80%)
- `NETHERITE_INGOT` x1-2 (50%)
- `DRAGON_EGG` x0-1 (10%)

---

## 🎮 Configuración de Mobs

### Formato JSON

Cada mob se define en `/plugins/MMORPGPlugin/data/mobs.json` (universal) o `/plugins/MMORPGPlugin/data/<world>/mobs.json` (local) con esta estructura:

```json
{
  "mob_id": {
    "displayName": "Nombre Visible",
    "entityType": "TIPO_ENTIDAD",
    "level": 10,
    "health": 50.0,
    "damage": 8.0,
    "defense": 5.0,
    "xpReward": 500,
    "isBoss": false,
    "spawnLocation": null,
    "drops": [
      "ITEM,minCant,maxCant,probabilidad%"
    ]
  }
}
```

### Parámetros

- **mob_id**: Identificador único (minúsculas, guiones bajos)
- **displayName**: Nombre que aparece sobre el mob
- **entityType**: Tipo de entidad Minecraft (ZOMBIE, SKELETON, etc.)
- **level**: Nivel RPG del mob (1-100)
- **health**: Puntos de vida (1 corazón = 2 HP)
- **damage**: Daño por ataque
- **defense**: Reducción de daño recibido
- **xpReward**: Experiencia otorgada al morir
- **isBoss**: `true` para mostrar barra de boss en pantalla
- **spawnLocation**: `"world,x,y,z"` o `null` para spawn manual
- **drops**: Array de strings formato `"ITEM,min,max,probability"`

### Tipos de Entidad Disponibles

```
ZOMBIE, SKELETON, CREEPER, SPIDER, SLIME, WITCH, PHANTOM, ENDERMAN, 
GHAST, IRON_GOLEM, BLAZE, PIGLIN_BRUTE, WITHER_SKELETON, RAVAGER, 
VINDICATOR, EVOKER, ELDER_GUARDIAN, SHULKER, WITHER, ENDER_DRAGON
```

---

## 🛠️ Administración desde Panel Web

### Crear Mob

1. Accede al panel web: `http://localhost:5000`
2. Ve al tab **RPG** → **Mobs**
3. Click en **Crear Mob**
4. Rellena formulario:
   - ID único
   - Nombre visible
   - Tipo de entidad
   - Stats (level, HP, damage, defense, XP)
   - Boss (sí/no)
   - Ubicación spawn (opcional)
   - Drops (formato: `ITEM,min,max,prob%`)
5. Seleccionar **Scope**:
   - **Local**: Solo en mundo actual
   - **Global**: Disponible en todos los mundos
6. Click **Crear Mob**

### Editar/Eliminar

- **Editar**: Click botón amarillo (lápiz) en la tabla
- **Eliminar**: Click botón rojo (papelera) y confirmar

---

## 🎯 Estrategias de Uso

### Early Game (Niveles 1-5)
- `spider_hunter` (Lv 4)
- `creeper_explosive` (Lv 4)
- `zombie_warrior` (Lv 5)

### Mid Game (Niveles 6-10)
- `skeleton_archer` (Lv 6)
- `slime_king` (Lv 7)
- `dark_witch` (Lv 8)
- `phantom_night_terror` (Lv 8)
- `enderman_shadow` (Lv 9)
- `ghast_phantom` (Lv 10)
- `ice_golem` (Lv 10)

### Late Game (Niveles 11-17)
- `blaze_inferno` (Lv 11)
- `piglin_brute_elite` (Lv 12)
- `wither_skeleton_knight` (Lv 13)
- `elite_vindicator` (Lv 13)
- `elite_ravager` (Lv 14)
- `elite_evoker` (Lv 15)
- `elite_guardian` (Lv 16)
- `elite_shulker` (Lv 17)

### End Game - Bosses (Niveles 18-20)
- `necromancer_king` (Lv 18) - Grupo 3-5 jugadores
- `arctic_titan` (Lv 19) - Grupo 4-6 jugadores
- `corrupted_dragon` (Lv 20) - Grupo 5-10 jugadores

---

## 📈 Balanceo y Best Practices

### Fórmulas de Referencia

**HP Base**: `level × 2.5 + 10`
**Damage Base**: `level × 0.7 + 2`
**Defense Base**: `level × 0.4`
**XP Reward Base**: `level × 30`

**Multiplicadores**:
- Mobs básicos: x1.0
- Mobs de élite: x1.5
- Bosses: x3.0

### Drops Recomendados

- Items comunes: 100% probabilidad, cantidad baja
- Items raros: 20-50% probabilidad
- Items épicos: 5-15% probabilidad
- Items legendarios: <5% probabilidad (solo bosses)

### Ubicaciones de Spawn

**Bosses**: Definir `spawnLocation` específica en estructuras especiales
**Élites**: Usar sistema de spawn por bioma
**Básicos**: Spawn aleatorio en mundo

---

## 🔧 Comandos Administrativos (Próximamente)

```
/rpg mob spawn <mob_id> [world] [x] [y] [z]
/rpg mob list
/rpg mob info <mob_id>
/rpg mob delete <mob_id>
/rpg mob reload
```

---

## 📝 Notas Técnicas

- Los mobs se cargan desde JSON al iniciar el servidor
- Los drops se procesan al morir el mob
- La XP se otorga automáticamente al jugador que mata
- Los bosses muestran barra de salud en la parte superior de la pantalla
- El sistema soporta scope local (por mundo) y universal (global)

---

**Última actualización**: 4 de diciembre de 2025
**Versión del sistema**: 1.0.0
**Total de mobs**: 21 (10 básicos + 5 élite + 3 bosses + 3 especiales)
