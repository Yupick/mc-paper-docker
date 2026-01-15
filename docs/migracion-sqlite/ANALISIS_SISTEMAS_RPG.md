# 📊 Análisis: Qué está en BD vs JSON

## Resumen Ejecutivo

**Migrados a SQLite** (3/20):
- ✅ **Mascotas** (Pets)
- ✅ **Eventos** (Events)
- ✅ **Respawn** (Respawn Templates)

**FUERA de SQLite** (17/20):
- ❌ Achievements (Logros)
- ❌ Bestiary (Bestiario)
- ❌ Classes (Clases de jugadores)
- ❌ Crafting Recipes (Recetas de fabricación)
- ❌ Dungeons (Mazmorras)
- ❌ Enchantments (Encantamientos)
- ❌ Items (Objetos custom)
- ❌ Invasions (Invasiones/Eventos especiales)
- ❌ Mobs Personalizados (Custom Mobs)
- ❌ NPCs (Personajes no-jugador)
- ❌ Quests (Misiones)
- ❌ Ranks (Rangos de jugadores)
- ❌ Shop (Sistema de tienda)
- ❌ Squads (Grupos/Guildas)
- ❌ Spawns (Puntos de spawn)
- ❌ Language (Idiomas/Localización)
- ❌ Economy (Economía, monedas)

---

## 📋 Detalles por Manager

### ✅ MIGRADOS A SQLite

#### 1. **PetManager** ✅ (Mascotas)
```
Archivo JSON: config/data/pets.json (JsonArray con 10 pets)
Tablas en BD: pets, pet_mounts, pet_abilities, pet_settings, player_pets
Status: ✅ COMPLETO
  - Lee desde: PetRepository (universal.db)
  - Fallback: JSON si BD vacía
  - Datos migrados: 10 mascotas
  - Método: PetManager.loadConfig() → PetRepository.getAllPets()
```

#### 2. **EventManager** ✅ (Eventos/Temporadas)
```
Archivo JSON: config/data/events.json (JsonArray con 6 eventos)
Tablas en BD: events, event_state
Status: ✅ COMPLETO
  - Lee desde: EventRepository (universal.db)
  - Fallback: JSON si BD vacía
  - Datos migrados: 6 eventos (Halloween, Navidad, Pascua, etc.)
  - Método: EventManager.loadConfig() → EventRepository.getAllEvents()
```

#### 3. **RespawnManager** ✅ (Zonas de Respawn)
```
Archivo JSON: config/respawn_config.json (Object con 1 zona)
Tablas en BD: respawn_templates, respawn_zones
Status: ✅ COMPLETO
  - Lee desde: RespawnRepository (universal.db)
  - Fallback: JSON si BD vacía
  - Datos migrados: 1 template (default respawn point)
  - Método: RespawnManager.loadConfig() → RespawnRepository.getAllRespawnTemplates()
```

---

### ❌ AÚN EN JSON (Pendientes de Migración)

#### 1. **AchievementManager** ❌ (Logros)
```
Archivo JSON: config/data/achievements.json
Tipo: JsonArray con definiciones de logros
Tabla preparada: achievements_definitions, player_achievements
Método carga: AchievementManager.loadConfigFromJSON()
Status: ❌ SIN MIGRACIÓN
```

#### 2. **BestiaryManager** ❌ (Bestiario)
```
Archivo JSON: config/data/bestiary.json
Tipo: JsonObject con categorías y tier rewards
Tabla preparada: bestiary (creada/verificada)
Método carga: BestiaryManager.loadFromConfig()
Status: ❌ SIN MIGRACIÓN
```

#### 3. **ClassManager** ❌ (Clases RPG)
```
Archivo JSON: config/data/classes.json (per-world)
Tipo: JsonObject con propiedades de clases
Tabla preparada: NO EXISTE
Método carga: ClassManager.loadClasses()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
```

#### 4. **CraftingManager** ❌ (Recetas)
```
Archivo JSON: config/crafting_config.json
Tipo: JsonObject con recipes: [] (vacío), crafting_stations
Tabla preparada: crafting_recipes (existe pero vacía)
Método carga: CraftingManager.loadRecipesFromConfig()
Status: ❌ SIN DATOS (recetas vacías en JSON)
Logs: "Loaded 0 recipes"
```

#### 5. **DungeonManager** ❌ (Mazmorras)
```
Archivo JSON: config/data/dungeons.json
Tipo: JsonObject con definiciones de mazmorras
Tabla preparada: dungeons (creada pero nunca usada)
Método carga: DungeonManager.loadDungeonsFromConfig()
Status: ❌ SIN MIGRACIÓN
Logs: "Cargadas 3 mazmorras" (desde JSON)
```

#### 6. **EnchantmentManager** ❌ (Encantamientos)
```
Archivo JSON: config/data/enchantments.json
Tipo: JsonObject con encantamientos custom
Tabla preparada: enchantments (existe)
Método carga: EnchantmentManager.loadEnchantments()
Status: ❌ SIN MIGRACIÓN + SIN DATOS
Logs: "No enchantments found in config"
```

#### 7. **ItemManager** ❌ (Objetos Custom)
```
Archivo JSON: config/data/items.json
Tipo: JsonArray con 15 items RPG custom
Tabla preparada: NO EXISTE
Método carga: ItemManager.loadItemsFromConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
Logs: "Cargados 15 items RPG"
```

#### 8. **InvasionManager** ❌ (Invasiones)
```
Archivo JSON: config/data/invasions.json
Tipo: JsonObject con configuración de invasiones
Tabla preparada: NO EXISTE
Método carga: InvasionManager.loadInvasionConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
Logs: "Loaded 1 invasion configurations"
```

#### 9. **MobManager** ❌ (Mobs Personalizados)
```
Archivo JSON: config/data/mobs.json
Tipo: JsonObject con definiciones de mobs custom
Tabla preparada: NO EXISTE
Método carga: MobManager.loadCustomMobs()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
Logs: "Cargados 0 mobs personalizados"
```

#### 10. **NPCManager** ❌ (NPCs)
```
Archivo JSON: config/data/npcs.json
Tipo: JsonObject con definiciones de NPCs
Tabla preparada: NO EXISTE
Método carga: NPCManager.loadNPCsFromConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
Logs: "Registrados X NPCs"
```

#### 11. **QuestManager** ❌ (Misiones)
```
Archivo JSON: config/data/quests.json
Tipo: JsonObject con definiciones de misiones
Tabla preparada: NO EXISTE
Método carga: QuestManager.loadQuestsFromConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
Logs: "Cargadas X misiones"
```

#### 12. **RankManager** ❌ (Rangos)
```
Archivo JSON: config/data/ranks.json
Tipo: JsonObject con definiciones de rangos
Tabla preparada: NO EXISTE
Método carga: RankManager.loadRanksFromConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
```

#### 13. **SquadManager** ❌ (Guildas/Grupos)
```
Archivo JSON: config/squad_config.json
Tipo: JsonObject con configuración de escuadras
Tabla preparada: squads (creada pero nunca usada)
Método carga: SquadManager.loadSquadConfig()
Status: ❌ SIN MIGRACIÓN
Logs: "Configuración de escuadras cargada desde JSON"
```

#### 14. **ShopManager** ❌ (Tienda)
```
Archivo JSON: config/api/shop.json (si existe)
Tipo: JsonObject con items de tienda
Tabla preparada: NO EXISTE
Método carga: ShopManager.loadShopConfig()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
```

#### 15. **SpawnManager** ❌ (Puntos de Spawn)
```
Archivo JSON: config/*.json (per-world spawns)
Tipo: JsonObject con ubicaciones de spawn
Tabla preparada: NO EXISTE
Método carga: SpawnManager.loadSpawns()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
```

#### 16. **EconomyManager** ❌ (Economía/Monedas)
```
Archivo JSON: config/economy.json (si existe)
Tipo: JsonObject con configuración de economía
Tabla preparada: player_economy (no usada)
Método carga: EconomyManager.loadConfig()
Status: ❌ SIN MIGRACIÓN + PARCIAL
```

#### 17. **LanguageManager** ❌ (Localización)
```
Archivo JSON: config/lang/*.json
Tipo: JsonObject con strings traducidos
Tabla preparada: NO EXISTE
Método carga: LanguageManager.loadLanguage()
Status: ❌ SIN MIGRACIÓN + SIN TABLA SQL
```

---

## 📊 Tabla Resumida

| Manager | JSON File | Status | Tabla SQL | Datos | Notas |
|---------|-----------|--------|-----------|-------|-------|
| **Pets** | data/pets.json | ✅ | 5 tablas | 10 | Completamente migrado |
| **Events** | data/events.json | ✅ | 2 tablas | 6 | Completamente migrado |
| **Respawn** | respawn_config.json | ✅ | 2 tablas | 1 | Completamente migrado |
| Achievements | data/achievements.json | ❌ | SÍ (vac) | - | Tabla preparada |
| Bestiary | data/bestiary.json | ❌ | SÍ (vac) | - | Tabla preparada |
| Classes | data/classes.json | ❌ | NO | - | Necesita tabla |
| Crafting | crafting_config.json | ❌ | SÍ (vac) | 0 | Tabla existe, sin datos |
| Dungeons | data/dungeons.json | ❌ | SÍ (vac) | - | Tabla preparada, 3 de JSON |
| Enchantments | data/enchantments.json | ❌ | SÍ (vac) | 0 | Tabla preparada, sin datos |
| Items | data/items.json | ❌ | NO | 15 | Necesita tabla, cargados de JSON |
| Invasions | data/invasions.json | ❌ | NO | 1 | Necesita tabla, 1 de JSON |
| Mobs | data/mobs.json | ❌ | NO | 0 | Necesita tabla |
| NPCs | data/npcs.json | ❌ | NO | - | Necesita tabla |
| Quests | data/quests.json | ❌ | NO | - | Necesita tabla |
| Ranks | data/ranks.json | ❌ | NO | - | Necesita tabla |
| Shop | api/shop.json | ❌ | NO | - | Necesita tabla |
| Squads | squad_config.json | ❌ | SÍ (vac) | - | Tabla preparada |
| Spawns | *.json (per-world) | ❌ | NO | - | Necesita tabla |
| Economy | economy.json | ❌ | SÍ (vac) | - | Tabla preparada |
| Language | lang/*.json | ❌ | NO | - | Necesita tabla |

---

## 🔍 Análisis Detallado

### Categoría 1: Completamente Migrados ✅ (3)
- PetManager
- EventManager
- RespawnManager

**Características:**
- Tienen Repository personalizado
- Usan `DatabaseManager.getConnection()`
- Fallback a JSON
- Datos persistidos en universal.db

### Categoría 2: Tablas Preparadas, Sin Usar ⚠️ (8)
- Achievements
- Bestiary
- Crafting
- Dungeons
- Enchantments
- Squads
- Economy
- (Respawn_zones adicional)

**Características:**
- Tablas creadas en `createTablesSQLite()` de DatabaseMigration.java
- Sin Repository implementado
- Sin migración automática
- Datos siguen en JSON
- Listos para ser migrados

### Categoría 3: Falta Tabla SQL ❌ (7)
- Classes
- Items
- Invasions
- Mobs
- NPCs
- Quests
- Ranks
- Shop
- Spawns
- Language

**Características:**
- Sin tablas en universal.db
- Datos solo en JSON
- Necesitan primero crear tablas SQL
- Luego implementar Repositories
- Luego migración automática

---

## 🚀 Roadmap de Migración (Fase 2)

### Prioridad 1: Usa Tablas Existentes (Más fácil)
1. **Achievements** (tabla exists)
2. **Crafting** (tabla exists, pero vacía)
3. **Bestiary** (tabla exists)
4. **Enchantments** (tabla exists)
5. **Squads** (tabla exists)

### Prioridad 2: Crear Tablas + Migrar
1. **Items** (15 en JSON)
2. **Mobs** (0 en JSON actualmente)
3. **NPCs** (muchos en JSON)
4. **Quests** (muchos en JSON)

### Prioridad 3: Backend Complejo
1. **Classes** (per-player, histórico)
2. **Ranks** (con historial)
3. **Spawns** (per-world)
4. **Language** (traduciones)
5. **Shop** (economy integration)

---

## 💾 Archivos JSON Actuales

```
config/
├── data/
│   ├── achievements.json ❌
│   ├── bestiary.json ❌
│   ├── crafting.json (vacío)
│   ├── dungeons.json ❌
│   ├── enchantments.json ❌
│   ├── events.json ✅ (6 eventos migrados)
│   ├── invasions.json ❌
│   ├── items.json ❌ (15 items)
│   ├── mobs.json ❌
│   ├── npcs.json ❌
│   ├── pets.json ✅ (10 pets migrados)
│   ├── quests.json ❌
│   └── ranks.json ❌
├── crafting_config.json ❌ (recipes: [])
├── respawn_config.json ✅ (1 template migrado)
├── squad_config.json ❌
├── api/
│   ├── status.json
│   └── shop.json ❌
└── lang/
    ├── es.json (Español)
    ├── en.json (Inglés)
    └── ... ❌
```

---

## 📝 Conclusión

### Estado Actual:
- **Fase 1 (Completada)**: 3/20 sistemas migrados a SQLite ✅
- **Fase 2 (Pendiente)**: 17/20 sistemas sin migración
- **Tablas preparadas**: 8 tablas sin usar
- **Sin tablas**: 9 sistemas necesitan tablas

### Recomendación Inmediata:
1. Migrar los 8 managers con tablas existentes (rápido)
2. Crear tablas para los 9 restantes
3. Implementar Repositories
4. Crear migraciones automáticas

### Impacto:
- 📁 **JSON a BD**: Mejor rendimiento, persistencia, consistencia
- 🔄 **Sincronización**: Más fácil con BD centralizada
- 🌍 **Multi-mundo**: Mejor soporte con BD por mundo
- 🔒 **Seguridad**: Control de transacciones

---

**Generado el**: 4 de Enero 2026  
**Base de datos**: SQLite universal.db (268 KB, 30 tablas)  
**Cobertura actual**: 15% de los sistemas RPG
