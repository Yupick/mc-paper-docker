-- ============================================================================
-- MIGRACIÓN COMPLETA A SQLITE - ESQUEMA UNIVERSAL + POR MUNDO
-- Fecha: 3 de enero de 2026
-- ============================================================================

-- ============================================================================
-- PARTE 1: UNIVERSAL.DB - Definiciones globales compartidas
-- ============================================================================

-- Tabla: PETS (Definiciones de mascotas disponibles)
CREATE TABLE IF NOT EXISTS pets (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('WOLF', 'HORSE', 'PARROT', 'CAT', 'BEE', 'FROG', 'AXOLOTL', 'ALLAY')),
    rarity TEXT CHECK(rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    description TEXT,
    
    -- Base stats JSON: {"health": 20, "damage": 5, "speed": 0.3, "jump": 0.5}
    base_stats TEXT,
    
    -- Item emoji/icon
    icon TEXT,
    
    -- Costo de adopción
    adoption_cost_coins INTEGER DEFAULT 0,
    
    -- Montura asociada (FK a pet_mounts)
    mount_id TEXT,
    
    -- Habilidades disponibles (JSON array de ability IDs: ["ability_1", "ability_2"])
    abilities_ids TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(mount_id) REFERENCES pet_mounts(id)
);

-- Tabla: PET_MOUNTS (Definiciones de monturas)
CREATE TABLE IF NOT EXISTS pet_mounts (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    
    -- Stats JSON: {"speed": 0.5, "jump_height": 1.0, "health": 30}
    stats TEXT,
    
    icon TEXT,
    cost_coins INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: PET_ABILITIES (Definiciones de habilidades)
CREATE TABLE IF NOT EXISTS pet_abilities (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    
    -- Tipo: PASSIVE, ACTIVE, COMBAT
    type TEXT CHECK(type IN ('PASSIVE', 'ACTIVE', 'COMBAT')),
    
    -- Config JSON: {"cooldown": 30, "range": 20, "damage": 10}
    config TEXT,
    
    icon TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: PET_SETTINGS (Configuración global de mascotas)
CREATE TABLE IF NOT EXISTS pet_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    
    -- Ej: {"key": "max_pets_per_player", "value": "5"}
    -- Ej: {"key": "pet_experience_multiplier", "value": "1.5"}
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para pets
CREATE INDEX IF NOT EXISTS idx_pets_type ON pets(type);
CREATE INDEX IF NOT EXISTS idx_pets_rarity ON pets(rarity);

-- ============================================================================

-- Tabla: EVENTS (Definiciones de eventos globales)
CREATE TABLE IF NOT EXISTS events (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    
    start_date TEXT NOT NULL,  -- ISO 8601: "2025-12-24T00:00:00"
    end_date TEXT NOT NULL,
    
    -- Estado del evento
    enabled INTEGER DEFAULT 1,
    auto_start INTEGER DEFAULT 1,
    
    -- Mobs especiales (JSON array: [{"mob_id": "event_skeleton", "spawn_rate": 0.3, "drops": {...}}])
    mobs_config TEXT,
    
    -- Drops únicos (JSON array: [{"item_id": "christmas_gift", "probability": 0.05, "quantity": 1}])
    drops_config TEXT,
    
    -- Zonas donde aparecen (JSON array: ["world", "nether"])
    zones TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para eventos
CREATE INDEX IF NOT EXISTS idx_events_enabled ON events(enabled);
CREATE INDEX IF NOT EXISTS idx_events_dates ON events(start_date, end_date);

-- ============================================================================

-- Tabla: RESPAWN_TEMPLATES (Definiciones de zonas de respawn por mundo)
CREATE TABLE IF NOT EXISTS respawn_templates (
    id TEXT PRIMARY KEY,
    world TEXT NOT NULL,
    
    name TEXT NOT NULL,
    type TEXT CHECK(type IN ('SPAWN_POINT', 'ZONE', 'DUNGEON_ENTRANCE')),
    
    -- Ubicación/área JSON: {"x": 100, "y": 64, "z": 100, "radius": 20}
    location TEXT NOT NULL,
    
    -- Lista de mobs que spawnan (JSON array: ["mob_1", "mob_2"])
    mob_ids TEXT,
    
    max_mobs INTEGER DEFAULT 10,
    respawn_interval_seconds INTEGER DEFAULT 30,
    
    enabled INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_respawn_world ON respawn_templates(world);
CREATE INDEX IF NOT EXISTS idx_respawn_enabled ON respawn_templates(enabled);

-- ============================================================================

-- Tabla: ENCHANTMENTS (Definiciones de encantamientos únicos del RPG)
CREATE TABLE IF NOT EXISTS enchantments (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    
    -- Nivel máximo del encantamiento
    max_level INTEGER DEFAULT 1,
    
    -- Config JSON: {"damage_bonus": 5, "speed_bonus": 0.5, "armor_bonus": 2}
    config TEXT,
    
    -- Puede combinarse con otros (1=sí, 0=no)
    combinable INTEGER DEFAULT 1,
    
    icon TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================

-- Tabla: CRAFTING_RECIPES (Definiciones de recetas de crafteo)
CREATE TABLE IF NOT EXISTS crafting_recipes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    
    -- Ingredientes (JSON): [{"item_id": "diamond", "quantity": 2}, ...]
    ingredients TEXT NOT NULL,
    
    -- Resultado (JSON): {"item_id": "diamond_sword", "quantity": 1}
    result TEXT NOT NULL,
    
    -- Requisito de nivel
    required_level INTEGER DEFAULT 0,
    
    -- Estación de crafteo (ej: "crafting_table", "furnace", "anvil")
    station_type TEXT,
    
    -- Tiempo de crafteo en segundos
    craft_time_seconds INTEGER DEFAULT 0,
    
    -- Costo de material adicional (monedas RPG)
    cost_coins INTEGER DEFAULT 0,
    
    enabled INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================

-- Tabla: ACHIEVEMENTS_DEFINITIONS (Definiciones de logros)
CREATE TABLE IF NOT EXISTS achievements_definitions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    
    -- Categoría
    category TEXT CHECK(category IN ('COMBAT', 'PROGRESSION', 'EXPLORATION', 'SOCIAL', 'ECONOMY', 'PETS')),
    
    -- Rareza visual
    rarity TEXT CHECK(rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    
    points INTEGER DEFAULT 0,
    
    -- Tipo de trigger y requirements (JSON)
    -- Ej: {"type": "KILL_MOB", "mobId": "creeper", "count": 50}
    requirements TEXT NOT NULL,
    
    -- Recompensas (JSON)
    -- Ej: {"xp": 500, "money": 1000, "title": "Creeper Slayer", "item_id": "special_sword"}
    rewards TEXT,
    
    icon TEXT,
    
    hidden INTEGER DEFAULT 0,
    broadcast_on_complete INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_achievements_category ON achievements_definitions(category);
CREATE INDEX IF NOT EXISTS idx_achievements_rarity ON achievements_definitions(rarity);

-- ============================================================================

-- Tabla: RANKS_DEFINITIONS (Definiciones de rangos del plugin)
CREATE TABLE IF NOT EXISTS ranks_definitions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    
    display_name TEXT,  -- Nombre colorido/formateado
    description TEXT,
    
    -- Nivel mínimo requerido
    min_level INTEGER DEFAULT 0,
    
    -- Beneficios (JSON)
    -- Ej: {"experience_multiplier": 1.2, "coin_multiplier": 1.5, "permissions": ["fly", "speed"]}
    benefits TEXT,
    
    icon TEXT,
    
    color TEXT,  -- Color para display
    
    priority INTEGER DEFAULT 0,  -- Mayor número = más prioritario
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================

-- Tabla: ITEMS_DEFINITIONS (Definiciones de items especiales)
CREATE TABLE IF NOT EXISTS items_definitions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    
    display_name TEXT,  -- Nombre colorido
    description TEXT,
    
    -- Tipo: WEAPON, ARMOR, CONSUMABLE, MISC, QUEST, PET
    item_type TEXT NOT NULL,
    
    -- Material base de Minecraft
    base_material TEXT,
    
    -- Rareza
    rarity TEXT CHECK(rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    
    -- Stats (JSON): {"damage": 10, "armor": 5, "speed": 0.2}
    stats TEXT,
    
    -- Costo en monedas (0 = sin venta)
    sell_price INTEGER DEFAULT 0,
    buy_price INTEGER DEFAULT 0,
    
    -- Límite de stack
    max_stack INTEGER DEFAULT 64,
    
    -- Lore/descripción extra (JSON array)
    lore TEXT,
    
    icon TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_items_type ON items_definitions(item_type);
CREATE INDEX IF NOT EXISTS idx_items_rarity ON items_definitions(rarity);

-- ============================================================================
-- PARTE 2: {WORLD}.DB - Datos locales por mundo (instancias)
-- ============================================================================

-- Tabla: PLAYER_PETS (Mascotas adoptadas por jugador)
CREATE TABLE IF NOT EXISTS player_pets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    pet_definition_id TEXT NOT NULL,
    
    -- Nombre customizado de la mascota
    pet_name TEXT,
    
    -- Estado: ACTIVE, INACTIVE, DEAD
    status TEXT DEFAULT 'ACTIVE',
    
    -- Estadísticas de instancia (JSON)
    -- Ej: {"health": 18, "experience": 2000, "level": 5}
    stats TEXT,
    
    -- Habilidades aprendidas (JSON array: ["ability_1", "ability_2"])
    learned_abilities TEXT,
    
    -- Fecha de adopción
    adopted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(pet_definition_id) REFERENCES pets(id),
    UNIQUE(player_uuid, pet_definition_id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_player_pets_uuid ON player_pets(player_uuid);
CREATE INDEX IF NOT EXISTS idx_player_pets_status ON player_pets(status);

-- ============================================================================

-- Tabla: RESPAWN_ZONES (Zonas de respawn en el mundo actual)
CREATE TABLE IF NOT EXISTS respawn_zones (
    id TEXT PRIMARY KEY,
    template_id TEXT,  -- Referencia a respawn_templates
    
    world TEXT NOT NULL,
    
    name TEXT NOT NULL,
    type TEXT,
    
    -- Ubicación actual (puede diferir del template)
    location TEXT NOT NULL,
    
    -- Mobs activos en la zona
    mob_ids TEXT,
    
    max_mobs INTEGER,
    respawn_interval_seconds INTEGER,
    
    -- Última vez que respawneó
    last_respawn TIMESTAMP,
    
    enabled INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(template_id) REFERENCES respawn_templates(id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_respawn_zones_world ON respawn_zones(world);
CREATE INDEX IF NOT EXISTS idx_respawn_zones_enabled ON respawn_zones(enabled);

-- ============================================================================

-- Tabla: EVENT_STATE (Estado de eventos en el mundo actual)
CREATE TABLE IF NOT EXISTS event_state (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL,
    world TEXT NOT NULL,
    
    -- PENDING, ACTIVE, ENDED
    status TEXT DEFAULT 'PENDING',
    
    -- Datos dinámicos (JSON)
    -- Ej: {"participants": 45, "total_kills": 1200, "active_mobs": 15}
    data TEXT,
    
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(event_id) REFERENCES events(id),
    UNIQUE(event_id, world)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_event_state_world ON event_state(world);
CREATE INDEX IF NOT EXISTS idx_event_state_status ON event_state(status);

-- ============================================================================

-- Tabla: PLAYER_ACHIEVEMENTS (Progreso de logros por jugador)
-- (Nota: Esta tabla ya debería existir en universal.db)
CREATE TABLE IF NOT EXISTS player_achievements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    achievement_id TEXT NOT NULL,
    
    progress INTEGER DEFAULT 0,
    completed INTEGER DEFAULT 0,
    completed_at TIMESTAMP,
    
    UNIQUE(player_uuid, achievement_id),
    FOREIGN KEY(achievement_id) REFERENCES achievements_definitions(id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_player_achievements_uuid ON player_achievements(player_uuid);
CREATE INDEX IF NOT EXISTS idx_player_achievements_completed ON player_achievements(completed);

-- ============================================================================

-- Tabla: PLAYER_ENCHANTED_ITEMS (Items encantados por jugador)
CREATE TABLE IF NOT EXISTS player_enchanted_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    
    -- Item base
    item_id TEXT NOT NULL,
    
    -- Nombre customizado
    display_name TEXT,
    
    -- Encantamientos aplicados (JSON array con niveles)
    -- Ej: [{"enchantment_id": "sharpness", "level": 3}, {"enchantment_id": "unbreaking", "level": 2}]
    enchantments TEXT,
    
    -- Durabilidad actual/máxima
    durability_current INTEGER,
    durability_max INTEGER,
    
    -- NBT data extra (JSON)
    nbt_data TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(item_id) REFERENCES items_definitions(id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_enchanted_items_player ON player_enchanted_items(player_uuid);

-- ============================================================================

-- Tabla: PLAYER_CRAFTING_HISTORY (Historial de crafteos)
CREATE TABLE IF NOT EXISTS player_crafting_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    recipe_id TEXT NOT NULL,
    
    quantity INTEGER DEFAULT 1,
    
    crafted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY(recipe_id) REFERENCES crafting_recipes(id)
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_crafting_history_player ON player_crafting_history(player_uuid);
CREATE INDEX IF NOT EXISTS idx_crafting_history_recipe ON player_crafting_history(recipe_id);

-- ============================================================================

-- Tabla: NPC_INSTANCES (NPCs spawnados en el mundo)
CREATE TABLE IF NOT EXISTS npc_instances (
    id TEXT PRIMARY KEY,
    world TEXT NOT NULL,
    
    -- Referencia a definición (si existe)
    npc_definition_id TEXT,
    
    name TEXT NOT NULL,
    description TEXT,
    
    -- Ubicación (JSON)
    location TEXT NOT NULL,
    
    -- Tipos: MERCHANT, QUESTGIVER, TRAINER, BOSS, MINION
    npc_type TEXT,
    
    -- Diálogos/interacciones (JSON)
    dialogues TEXT,
    
    -- Loot drops si es NPC de combate (JSON)
    drops TEXT,
    
    enabled INTEGER DEFAULT 1,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_npcs_world ON npc_instances(world);
CREATE INDEX IF NOT EXISTS idx_npcs_type ON npc_instances(npc_type);

-- ============================================================================
-- FIN DEL ESQUEMA
-- ============================================================================
