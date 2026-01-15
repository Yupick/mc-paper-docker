# 🚀 PLAN DE IMPLEMENTACIÓN FINAL - Migración SQLite

**Fecha:** 27 de diciembre de 2025  
**Status:** APROBADO - Listo para comenzar  

---

## 📐 ARQUITECTURA FINAL APROBADA

### Estructura de Directorios

```
minecraft-server/
├── config/
│   ├── config.yml                     ✅ Configuración general
│   ├── backup_config.json             ✅ Config backup
│   ├── panel_config.json              ✅ Config web
│   ├── server.properties              ✅ Minecraft props
│   └── templates/                     ✅ NUEVO - Templates JSON
│       ├── items_template.json
│       ├── mobs_template.json
│       ├── enchantments_template.json
│       ├── achievements_template.json
│       ├── crafting_template.json
│       ├── events_template.json
│       ├── pets_template.json
│       └── dungeons_template.json
│
├── plugins/
│   ├── MMORPGPlugin.jar               (Ejecutable)
│   └── MMORPGPlugin/                  (Carpeta del plugin)
│       └── data/
│           └── universal.db           ✅ Base de datos UNIVERSAL
│               ├── items
│               ├── mobs
│               ├── enchantments
│               ├── achievements_def
│               ├── crafting_recipes
│               ├── events_templates
│               ├── pets_def
│               └── dungeons_def
│
└── worlds/
    ├── active/ → symlink a mundo activo
    │   └── data/
    │       └── {world_name}.db        ✅ Base de datos LOCAL (creado por WEB)
    │           ├── players
    │           ├── player_abilities
    │           ├── quests
    │           ├── quest_objectives
    │           ├── player_quests
    │           ├── npcs
    │           ├── spawns
    │           ├── squads
    │           ├── squad_members
    │           ├── economy_transactions
    │           ├── dungeons_active
    │           ├── invasions_active
    │           ├── crafting_history
    │           └── enchanted_items
    │
    └── {world_name}/
        ├── level.dat                  (Minecraft data)
        ├── metadata.json              (Solo config, NO datos RPG)
        └── data/
            └── {world_name}.db        (Mismo que arriba)
```

---

## 🔄 DECISIONES ARQUITECTÓNICAS CONFIRMADAS

### ✅ 1. Creación de {world_name}.db
**Decisión:** Copiar schema + datos iniciales desde universal.db

```
Cuando web activa RPG:
1. Copia estructura de tablas desde universal.db
2. Inserta datos iniciales (NPCs base, quests iniciales)
3. Crea tablas específicas del mundo
```

### ✅ 2. Templates en config/templates/
**Decisión:** Mantener templates JSON para personalización fácil

```
Uso:
- Plugin lee templates SOLO la primera vez
- Carga datos en universal.db
- Templates NO se vuelven a leer (immutable)
- Admin puede editar templates antes de crear universal.db
```

### ✅ 3. Sincronización en Tiempo Real
**Decisión:** Base de datos compartida + timestamps

```
Mecanismo:
- Plugin y Web escriben en mismas tablas
- Cada registro tiene: updated_at (timestamp)
- Conflictos: gana el más reciente
- Transacciones SQLite evitan corrupción
```

### ✅ 4. Acceso Web a Bases de Datos
**Decisión:** universal.db (solo lectura) + {world_name}.db (lectura/escritura)

```
Web puede:
- Leer de universal.db (items, mobs, etc.)
- Leer/escribir en {world_name}.db (jugadores, quests, etc.)
- NO modificar universal.db (solo plugin)
```

### ✅ 5. Creación de {world_name}.db
**Decisión:** Web crea cuando activa RPG (PATCH toggle-rpg)

```
Flujo:
1. Admin crea mundo en web (POST /api/worlds)
2. Admin activa RPG (PATCH /api/worlds/{id}/toggle-rpg)
3. Web ejecuta:
   - Crea worlds/{world_name}/data/
   - Crea {world_name}.db
   - Copia schema de universal.db
   - Inserta datos iniciales
```

### ✅ 6. Plugin Crea universal.db
**Decisión:** Plugin crea al iniciar por primera vez

```
Flujo (onEnable):
1. Verifica si existe plugins/MMORPGPlugin/data/universal.db
2. Si NO existe:
   - Lee templates de config/templates/
   - Crea universal.db
   - Crea tablas
   - Inserta datos desde templates
3. Si existe:
   - Solo abre conexión
```

### ✅ 7. Autoridad de Datos Universales
**Decisión:** Plugin es autoridad, web solo lee

```
Modificaciones a universal.db:
- Solo plugin puede escribir (admin commands)
- Web solo lee (para mostrar en panel)
- Evita conflictos de sincronización
```

---

## 📋 FASES DE IMPLEMENTACIÓN DETALLADAS

### 📦 FASE 0: PREPARACIÓN (30 minutos)

#### 0.1 Crear Backup Completo
```bash
cd /home/mkd/contenedores/mc-paper-docker
tar -czf backup_pre_migracion_$(date +%Y%m%d_%H%M%S).tar.gz \
    config/ plugins/ worlds/ web/
```

#### 0.2 Crear config/templates/
```bash
mkdir -p config/templates
```

#### 0.3 Mover Templates Actuales
```bash
# Mover templates JSON existentes
mv config/crafting_config.json config/templates/crafting_template.json
mv config/enchanting_config.json config/templates/enchanting_template.json
mv config/enchantments_config.json config/templates/enchantments_template.json
mv config/events_config.json config/templates/events_template.json
mv config/pets_config.json config/templates/pets_template.json
mv config/dungeons_config.json config/templates/dungeons_template.json
mv config/respawn_config.json config/templates/respawn_template.json
mv config/squad_config.json config/templates/squad_template.json
```

#### 0.4 Crear Templates Faltantes
```bash
# Estos deben existir para que plugin cargue universal.db
touch config/templates/items_template.json
touch config/templates/mobs_template.json
touch config/templates/achievements_template.json
```

---

### 🧹 FASE 1: LIMPIEZA (1 hora)

#### 1.1 Eliminar Carpetas Obsoletas
```bash
cd /home/mkd/contenedores/mc-paper-docker/config

# Eliminar datos obsoletos
rm -rf data/              # 308 KB de JSON desactualizados
rm -rf MMORPGPlugin/      # Duplicado
rm -rf api/               # Datos viejos
rm -rf npcs/              # Datos viejos
rm -rf pets/              # Vacío o viejo
rm -rf plugin-data/       # Obsoleto
rm -rf quest-progress/    # Obsoleto
rm -rf classes/           # Obsoleto
rm -rf plugin/            # Duplicado
```

#### 1.2 Verificar create.sh
```bash
# Asegurar que create.sh sigue funcionando después de limpieza
./create.sh --dry-run  # (Si existe esta opción)
```

#### 1.3 Resultado Esperado
```
config/
├── config.yml
├── backup_config.json
├── panel_config.json
├── server.properties
└── templates/          ← NUEVA ESTRUCTURA
    └── [8 templates JSON]
```

---

### 🗄️ FASE 2: DISEÑO DE BASES DE DATOS (2 horas)

#### 2.1 Schema para universal.db

```sql
-- plugins/MMORPGPlugin/data/universal.db

-- Items globales
CREATE TABLE items (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    type TEXT NOT NULL,
    rarity TEXT,
    stats_json TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Mobs globales
CREATE TABLE mobs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    health REAL,
    damage REAL,
    level INTEGER,
    drops_json TEXT,
    abilities_json TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Encantamientos
CREATE TABLE enchantments (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    max_level INTEGER,
    rarity TEXT,
    applicable_items_json TEXT,
    effects_json TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Recetas de crafteo
CREATE TABLE crafting_recipes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    ingredients_json TEXT,
    result_item TEXT,
    result_amount INTEGER,
    requires_station TEXT,
    unlocked_by_default BOOLEAN DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Definición de logros
CREATE TABLE achievements_def (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category TEXT,
    points INTEGER,
    requirements_json TEXT,
    rewards_json TEXT,
    icon TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Templates de eventos
CREATE TABLE events_templates (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT,
    config_json TEXT,
    rewards_json TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Definición de mascotas
CREATE TABLE pets_def (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    rarity TEXT,
    stats_json TEXT,
    abilities_json TEXT,
    evolution_json TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Templates de dungeons
CREATE TABLE dungeons_def (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    min_level INTEGER,
    max_level INTEGER,
    min_players INTEGER,
    max_players INTEGER,
    rooms_json TEXT,
    rewards_json TEXT,
    enabled BOOLEAN DEFAULT 1,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Índices
CREATE INDEX idx_items_type ON items(type);
CREATE INDEX idx_items_rarity ON items(rarity);
CREATE INDEX idx_mobs_level ON mobs(level);
CREATE INDEX idx_enchantments_rarity ON enchantments(rarity);
```

#### 2.2 Schema para {world_name}.db

```sql
-- worlds/{world_name}/data/{world_name}.db

-- Jugadores
CREATE TABLE players (
    uuid TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    class_type TEXT,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    health REAL,
    max_health REAL,
    mana REAL,
    max_mana REAL,
    skill_points INTEGER DEFAULT 0,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    last_login INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Habilidades de jugadores
CREATE TABLE player_abilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    ability_id TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    last_used INTEGER DEFAULT 0,
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid),
    UNIQUE(player_uuid, ability_id)
);

-- Quests del mundo
CREATE TABLE quests (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    difficulty TEXT,
    min_level INTEGER,
    npc_giver_id TEXT,
    exp_reward INTEGER,
    money_reward REAL,
    skill_points_reward INTEGER,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Objetivos de quests
CREATE TABLE quest_objectives (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quest_id TEXT NOT NULL,
    objective_id TEXT NOT NULL,
    type TEXT NOT NULL,
    target TEXT,
    amount INTEGER,
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (quest_id) REFERENCES quests(id),
    UNIQUE(quest_id, objective_id)
);

-- Progreso de quests por jugador
CREATE TABLE player_quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    quest_id TEXT NOT NULL,
    status TEXT NOT NULL,
    accepted_at INTEGER,
    completed_at INTEGER,
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid),
    FOREIGN KEY (quest_id) REFERENCES quests(id),
    UNIQUE(player_uuid, quest_id)
);

-- Progreso de objetivos por jugador
CREATE TABLE player_quest_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    quest_id TEXT NOT NULL,
    objective_id TEXT NOT NULL,
    progress INTEGER DEFAULT 0,
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid),
    FOREIGN KEY (quest_id) REFERENCES quests(id),
    UNIQUE(player_uuid, quest_id, objective_id)
);

-- NPCs del mundo
CREATE TABLE npcs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    yaw REAL,
    pitch REAL,
    quest_id TEXT,
    dialogue TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Spawns del mundo
CREATE TABLE spawns (
    id TEXT PRIMARY KEY,
    mob_id TEXT NOT NULL,
    world TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    radius REAL DEFAULT 10,
    respawn_time INTEGER DEFAULT 300,
    max_spawns INTEGER DEFAULT 1,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Escuadras/Guilds
CREATE TABLE squads (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    leader_uuid TEXT NOT NULL,
    level INTEGER DEFAULT 1,
    experience INTEGER DEFAULT 0,
    max_members INTEGER DEFAULT 10,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (leader_uuid) REFERENCES players(uuid)
);

-- Miembros de escuadras
CREATE TABLE squad_members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    squad_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    role TEXT DEFAULT 'member',
    joined_at INTEGER DEFAULT (strftime('%s','now')),
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (squad_id) REFERENCES squads(id),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid),
    UNIQUE(player_uuid)
);

-- Transacciones de economía
CREATE TABLE economy_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    amount REAL NOT NULL,
    balance_before REAL NOT NULL,
    balance_after REAL NOT NULL,
    description TEXT,
    timestamp INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Dungeons activas
CREATE TABLE dungeons_active (
    id TEXT PRIMARY KEY,
    dungeon_def_id TEXT NOT NULL,
    world TEXT NOT NULL,
    status TEXT DEFAULT 'active',
    started_at INTEGER DEFAULT (strftime('%s','now')),
    completed_at INTEGER,
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Participantes en dungeons
CREATE TABLE dungeon_participants (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dungeon_id TEXT NOT NULL,
    player_uuid TEXT NOT NULL,
    joined_at INTEGER DEFAULT (strftime('%s','now')),
    left_at INTEGER,
    updated_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (dungeon_id) REFERENCES dungeons_active(id),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Invasiones activas
CREATE TABLE invasions_active (
    id TEXT PRIMARY KEY,
    world TEXT NOT NULL,
    wave INTEGER DEFAULT 1,
    status TEXT DEFAULT 'active',
    started_at INTEGER DEFAULT (strftime('%s','now')),
    completed_at INTEGER,
    updated_at INTEGER DEFAULT (strftime('%s','now'))
);

-- Historial de crafteo
CREATE TABLE crafting_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    recipe_id TEXT NOT NULL,
    started_at INTEGER DEFAULT (strftime('%s','now')),
    completed_at INTEGER,
    ingredients_used TEXT,
    result_item TEXT,
    result_amount INTEGER,
    experience_earned INTEGER,
    coins_earned INTEGER,
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Recetas desbloqueadas por jugador
CREATE TABLE unlocked_recipes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    recipe_id TEXT NOT NULL,
    unlocked_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid),
    UNIQUE(player_uuid, recipe_id)
);

-- Items encantados
CREATE TABLE enchanted_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    item_data TEXT NOT NULL,
    enchantments_json TEXT NOT NULL,
    experience_invested INTEGER,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Historial de encantamientos
CREATE TABLE enchantment_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    enchantment_id TEXT NOT NULL,
    item_type TEXT NOT NULL,
    level INTEGER,
    cost_xp INTEGER,
    cost_coins INTEGER,
    status TEXT,
    created_at INTEGER DEFAULT (strftime('%s','now')),
    completed_at INTEGER,
    FOREIGN KEY (player_uuid) REFERENCES players(uuid)
);

-- Índices para performance
CREATE INDEX idx_players_class ON players(class_type);
CREATE INDEX idx_players_level ON players(level);
CREATE INDEX idx_player_quests_status ON player_quests(player_uuid, status);
CREATE INDEX idx_economy_player ON economy_transactions(player_uuid);
CREATE INDEX idx_squad_members_player ON squad_members(player_uuid);
CREATE INDEX idx_spawns_world ON spawns(world);
CREATE INDEX idx_npcs_world ON npcs(world);
```

---

### 🔧 FASE 3: REFACTORIZAR PLUGIN (8-10 horas)

#### 3.1 Modificar DatabaseManager.java

**Ubicación:** `mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/database/DatabaseManager.java`

**Cambios principales:**

```java
public class DatabaseManager {
    private final MMORPGPlugin plugin;
    private Connection universalConnection;  // Nueva conexión
    private Map<String, Connection> worldConnections;  // Múltiples mundos
    private final ExecutorService executor;
    
    // Rutas
    private final File universalDbFile;  // plugins/MMORPGPlugin/data/universal.db
    
    public DatabaseManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(2);
        this.worldConnections = new ConcurrentHashMap<>();
        
        // Ruta a universal.db
        File pluginDataDir = new File(plugin.getDataFolder(), "data");
        pluginDataDir.mkdirs();
        this.universalDbFile = new File(pluginDataDir, "universal.db");
        
        initializeUniversalDatabase();
    }
    
    /**
     * Inicializa universal.db desde templates
     */
    private void initializeUniversalDatabase() {
        try {
            boolean isFirstRun = !universalDbFile.exists();
            
            String url = "jdbc:sqlite:" + universalDbFile.getAbsolutePath();
            universalConnection = DriverManager.getConnection(url);
            
            if (isFirstRun) {
                plugin.getLogger().info("Primera ejecución: creando universal.db...");
                createUniversalTables();
                loadTemplatesIntoUniversal();
            } else {
                plugin.getLogger().info("universal.db encontrado, cargando...");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error inicializando universal.db: " + e.getMessage());
        }
    }
    
    /**
     * Crea tablas en universal.db
     */
    private void createUniversalTables() {
        // Ejecutar schema de universal.db (definido en Fase 2.1)
        // ... CREATE TABLE items, mobs, etc.
    }
    
    /**
     * Carga datos desde config/templates/ en universal.db
     */
    private void loadTemplatesIntoUniversal() {
        File templatesDir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "config/templates");
        
        if (!templatesDir.exists()) {
            plugin.getLogger().warning("Carpeta config/templates/ no encontrada");
            return;
        }
        
        // Cargar items_template.json
        loadItemsTemplate(new File(templatesDir, "items_template.json"));
        
        // Cargar mobs_template.json
        loadMobsTemplate(new File(templatesDir, "mobs_template.json"));
        
        // Cargar enchantments_template.json
        loadEnchantmentsTemplate(new File(templatesDir, "enchantments_template.json"));
        
        // Cargar crafting_template.json
        loadCraftingTemplate(new File(templatesDir, "crafting_template.json"));
        
        // ... otros templates
        
        plugin.getLogger().info("Templates cargados en universal.db");
    }
    
    /**
     * Obtiene conexión para un mundo específico
     */
    public Connection getWorldConnection(String worldSlug) {
        return worldConnections.computeIfAbsent(worldSlug, this::createWorldConnection);
    }
    
    /**
     * Crea conexión para un mundo
     */
    private Connection createWorldConnection(String worldSlug) {
        try {
            File worldsDir = new File(plugin.getServer().getWorldContainer(), "worlds");
            File worldDataDir = new File(worldsDir, worldSlug + "/data");
            File worldDbFile = new File(worldDataDir, worldSlug + ".db");
            
            if (!worldDbFile.exists()) {
                plugin.getLogger().warning("DB para mundo " + worldSlug + " no existe. Web debe crearlo.");
                return null;
            }
            
            String url = "jdbc:sqlite:" + worldDbFile.getAbsolutePath();
            return DriverManager.getConnection(url);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error conectando a DB de mundo " + worldSlug);
            return null;
        }
    }
    
    /**
     * Obtiene conexión universal (solo lectura para managers)
     */
    public Connection getUniversalConnection() {
        return universalConnection;
    }
    
    // ... resto de métodos (executeQuery, executeUpdate, etc.)
}
```

#### 3.2 Actualizar Managers (8 archivos)

**Lista de managers a actualizar:**

1. **EnchantmentManager** → Lee de universal.db
2. **CraftingManager** → Lee recetas de universal.db, guarda historial en {world}.db
3. **ItemManager** → Lee de universal.db
4. **MobManager** → Lee de universal.db
5. **QuestManager** → Lee/escribe en {world}.db
6. **NPCManager** → Lee/escribe en {world}.db
7. **ClassManager** → Lee/escribe en {world}.db
8. **EconomyManager** → Lee/escribe en {world}.db
9. **EventManager** → Lee templates de universal.db, instancias en {world}.db
10. **SquadManager** → Lee/escribe en {world}.db
11. **InvasionManager** → Lee/escribe en {world}.db
12. **AchievementManager** → Definiciones en universal.db, instancias en {world}.db

**Ejemplo: EnchantmentManager.java**

```java
public class EnchantmentManager {
    private final MMORPGPlugin plugin;
    private final Map<String, RPGEnchantment> enchantments;
    
    public EnchantmentManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.enchantments = new ConcurrentHashMap<>();
        loadEnchantments();
    }
    
    private void loadEnchantments() {
        try {
            Connection conn = plugin.getDatabase().getUniversalConnection();
            String sql = "SELECT * FROM enchantments";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    // ... parsear y crear RPGEnchantment
                    enchantments.put(id, enchantment);
                }
            }
            
            plugin.getLogger().info("Cargados " + enchantments.size() + " encantamientos desde universal.db");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando encantamientos: " + e.getMessage());
        }
    }
    
    // Guardar historial en {world}.db
    public void saveEnchantmentHistory(Player player, String enchantmentId, ...) {
        String worldSlug = player.getWorld().getName();
        Connection conn = plugin.getDatabase().getWorldConnection(worldSlug);
        
        // INSERT INTO enchantment_history ...
    }
}
```

#### 3.3 Eliminar DataManager.java

Este archivo ya no es necesario porque no exportamos a JSON.

```bash
rm mmorpg-plugin/src/main/java/com/nightslayer/mmorpg/DataManager.java
```

#### 3.4 Actualizar MMORPGPlugin.java

Remover referencias a DataManager y actualizar onEnable():

```java
@Override
public void onEnable() {
    instance = this;
    
    // Inicializar Gson
    gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Guardar configuración por defecto
    saveDefaultConfig();
    
    // Inicializar gestores
    languageManager = new LanguageManager(this);
    databaseManager = new DatabaseManager(this);  // Ahora crea universal.db
    worldRPGManager = new WorldRPGManager(this);
    
    // YA NO: dataManager = new DataManager(this);  ← ELIMINAR
    
    // Resto de managers...
    economyManager = new EconomyManager(this);
    classManager = new ClassManager(this);
    // ...
    
    getLogger().info("MMORPGPlugin habilitado con SQLite 100%");
}
```

---

### 🌐 FASE 4: REFACTORIZAR WEB (4-6 horas)

#### 4.1 Modificar rpg_manager.py

**Ubicación:** `web/models/rpg_manager.py`

**Cambios completos:**

```python
import sqlite3
import os
from pathlib import Path
from typing import Optional, Dict, List
import json

class RPGManager:
    """
    Gestiona acceso a bases de datos SQLite del plugin MMORPG
    """
    
    def __init__(self, base_path: str = None, worlds_path: str = None):
        if base_path is None:
            current_dir = Path(__file__).resolve().parent.parent
            self.base_path = current_dir
        else:
            self.base_path = Path(base_path)
        
        self.worlds_path = Path(worlds_path) if worlds_path else self.base_path / "worlds"
        
        # Ruta a universal.db
        self.universal_db_path = self.base_path / "plugins" / "MMORPGPlugin" / "data" / "universal.db"
    
    def get_universal_db_connection(self) -> Optional[sqlite3.Connection]:
        """
        Obtiene conexión a universal.db (solo lectura)
        """
        if not self.universal_db_path.exists():
            print(f"universal.db no existe: {self.universal_db_path}")
            return None
        
        try:
            conn = sqlite3.connect(str(self.universal_db_path))
            conn.row_factory = sqlite3.Row  # Acceso por nombre de columna
            return conn
        except Exception as e:
            print(f"Error conectando a universal.db: {e}")
            return None
    
    def get_world_db_connection(self, world_slug: str) -> Optional[sqlite3.Connection]:
        """
        Obtiene conexión a {world_slug}.db (lectura/escritura)
        """
        world_db_path = self.worlds_path / world_slug / "data" / f"{world_slug}.db"
        
        if not world_db_path.exists():
            print(f"DB del mundo no existe: {world_db_path}")
            return None
        
        try:
            conn = sqlite3.connect(str(world_db_path))
            conn.row_factory = sqlite3.Row
            return conn
        except Exception as e:
            print(f"Error conectando a {world_slug}.db: {e}")
            return None
    
    def create_world_database(self, world_slug: str) -> bool:
        """
        Crea {world_slug}.db cuando se activa RPG
        """
        world_data_dir = self.worlds_path / world_slug / "data"
        world_data_dir.mkdir(parents=True, exist_ok=True)
        
        world_db_path = world_data_dir / f"{world_slug}.db"
        
        if world_db_path.exists():
            print(f"BD del mundo ya existe: {world_db_path}")
            return True
        
        try:
            # Crear BD
            conn = sqlite3.connect(str(world_db_path))
            cursor = conn.cursor()
            
            # Copiar schema desde universal.db
            self._create_world_schema(cursor)
            
            # Insertar datos iniciales
            self._insert_world_initial_data(cursor, world_slug)
            
            conn.commit()
            conn.close()
            
            print(f"BD del mundo creada: {world_db_path}")
            return True
            
        except Exception as e:
            print(f"Error creando BD del mundo: {e}")
            return False
    
    def _create_world_schema(self, cursor):
        """
        Crea todas las tablas para {world_slug}.db
        """
        # Schema completo de Fase 2.2
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                class_type TEXT,
                level INTEGER DEFAULT 1,
                experience INTEGER DEFAULT 0,
                health REAL,
                max_health REAL,
                mana REAL,
                max_mana REAL,
                skill_points INTEGER DEFAULT 0,
                created_at INTEGER DEFAULT (strftime('%s','now')),
                last_login INTEGER DEFAULT (strftime('%s','now')),
                updated_at INTEGER DEFAULT (strftime('%s','now'))
            )
        """)
        
        # ... todas las demás tablas del schema
        # (player_abilities, quests, quest_objectives, etc.)
        
        # Crear índices
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_players_class ON players(class_type)")
        # ... otros índices
    
    def _insert_world_initial_data(self, cursor, world_slug: str):
        """
        Inserta datos iniciales en {world_slug}.db
        """
        # Ejemplo: Insertar NPCs iniciales
        cursor.execute("""
            INSERT OR IGNORE INTO npcs (id, name, type, world, x, y, z)
            VALUES ('starter_npc', 'Guía del Aventurero', 'quest_giver', ?, 0, 64, 0)
        """, (world_slug,))
        
        # Insertar quests iniciales
        cursor.execute("""
            INSERT OR IGNORE INTO quests (id, name, description, difficulty, min_level)
            VALUES ('welcome_quest', 'Bienvenido al RPG', 'Tu primera misión', 'EASY', 1)
        """)
        
        # ... otros datos iniciales
    
    # Métodos de consulta
    def get_items(self) -> List[Dict]:
        """Lee items desde universal.db"""
        conn = self.get_universal_db_connection()
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM items")
            items = [dict(row) for row in cursor.fetchall()]
            conn.close()
            return items
        except Exception as e:
            print(f"Error obteniendo items: {e}")
            return []
    
    def get_players(self, world_slug: str) -> List[Dict]:
        """Lee jugadores desde {world_slug}.db"""
        conn = self.get_world_db_connection(world_slug)
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM players")
            players = [dict(row) for row in cursor.fetchall()]
            conn.close()
            return players
        except Exception as e:
            print(f"Error obteniendo jugadores: {e}")
            return []
    
    def get_quests(self, world_slug: str) -> List[Dict]:
        """Lee quests desde {world_slug}.db"""
        conn = self.get_world_db_connection(world_slug)
        if not conn:
            return []
        
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM quests")
            quests = [dict(row) for row in cursor.fetchall()]
            conn.close()
            return quests
        except Exception as e:
            print(f"Error obteniendo quests: {e}")
            return []
    
    # ... más métodos según necesites
```

#### 4.2 Actualizar world_manager.py

**Ubicación:** `web/models/world_manager.py`

Agregar llamada a `rpg_manager.create_world_database()` cuando se activa RPG:

```python
def toggle_rpg(self, world_name: str, enable: bool) -> bool:
    """
    Activa/desactiva RPG en un mundo
    """
    world = self.get_world(world_name)
    if not world:
        return False
    
    # Actualizar metadata
    world.metadata['isRPG'] = enable
    world.save_metadata()
    
    # Si se activa RPG, crear base de datos
    if enable:
        world_slug = world.metadata.get('slug', world_name)
        if self.rpg_manager:
            success = self.rpg_manager.create_world_database(world_slug)
            if not success:
                print(f"Error: No se pudo crear BD para {world_slug}")
                return False
            print(f"BD creada para mundo RPG: {world_slug}")
    
    return True
```

#### 4.3 Actualizar Rutas API en app.py

**Ubicación:** `web/app.py`

Modificar rutas RPG para usar SQLite:

```python
@app.route('/api/rpg/items')
@login_required
def get_rpg_items():
    """Obtiene items desde universal.db"""
    try:
        items = rpg_manager.get_items()
        return jsonify({'success': True, 'items': items})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/rpg/players/<world_slug>')
@login_required
def get_rpg_players(world_slug):
    """Obtiene jugadores desde {world_slug}.db"""
    try:
        players = rpg_manager.get_players(world_slug)
        return jsonify({'success': True, 'players': players})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

@app.route('/api/rpg/quests/<world_slug>')
@login_required
def get_rpg_quests(world_slug):
    """Obtiene quests desde {world_slug}.db"""
    try:
        quests = rpg_manager.get_quests(world_slug)
        return jsonify({'success': True, 'quests': quests})
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

# ... más rutas según necesites
```

---

### 🗂️ FASE 5: REORGANIZAR DIRECTORIOS (1 hora)

#### 5.1 Consolidar Bases de Datos Existentes

```bash
# Opcional: Migrar datos de rpgdata.db, minecraft_rpg.db, squads.db
# a las nuevas estructuras si hay datos valiosos

# Por ahora, solo mover a backup
mkdir -p config/db_backup
mv config/rpgdata.db config/db_backup/
mv config/minecraft_rpg.db config/db_backup/
mv config/squads.db config/db_backup/
```

#### 5.2 Verificar Estructura Final

```bash
tree -L 3 config/ plugins/MMORPGPlugin/ worlds/
```

Resultado esperado:
```
config/
├── config.yml
├── backup_config.json
├── panel_config.json
├── server.properties
└── templates/
    ├── items_template.json
    ├── mobs_template.json
    └── ... (8 templates)

plugins/MMORPGPlugin/
└── data/
    └── universal.db

worlds/
├── active/ → (symlink)
│   └── data/
│       └── {world_name}.db
└── {world_name}/
    ├── level.dat
    ├── metadata.json
    └── data/
        └── {world_name}.db
```

---

### 🧪 FASE 6: TESTING (4-6 horas)

#### 6.1 Testing Plugin

**Test 1: Plugin crea universal.db**
```
1. Detener servidor
2. Eliminar plugins/MMORPGPlugin/data/universal.db
3. Iniciar servidor
4. Verificar logs: "creando universal.db..."
5. Verificar que archivo existe
6. Verificar tablas con: sqlite3 universal.db ".tables"
```

**Test 2: Plugin carga templates**
```
1. Verificar que config/templates/ tiene archivos
2. Iniciar plugin (primera vez)
3. Verificar logs: "Templates cargados..."
4. Consultar: sqlite3 universal.db "SELECT COUNT(*) FROM items"
5. Debe haber registros
```

**Test 3: Managers leen de universal.db**
```
1. Ejecutar /rpg enchantments list
2. Debe mostrar encantamientos desde DB
3. Ejecutar /rpg items list
4. Debe mostrar items desde DB
```

#### 6.2 Testing Web

**Test 4: Web crea {world_name}.db**
```
1. Crear mundo nuevo en panel web
2. Activar RPG (toggle)
3. Verificar logs web: "BD creada para mundo..."
4. Verificar que worlds/{world}/data/{world}.db existe
5. Verificar tablas con: sqlite3 {world}.db ".tables"
```

**Test 5: Web lee de universal.db**
```
1. GET /api/rpg/items
2. Debe retornar lista de items
3. Verificar que coincide con DB
```

**Test 6: Web lee de {world}.db**
```
1. GET /api/rpg/players/{world_slug}
2. Debe retornar lista de jugadores
3. Crear jugador en-game
4. Refrescar endpoint, debe aparecer
```

#### 6.3 Testing Sincronización

**Test 7: Cambios en plugin se ven en web**
```
1. Jugador completa quest en-game
2. Refrescar panel web
3. Quest debe aparecer como completada
4. Tiempo de sincronización: < 10 segundos
```

**Test 8: Cambios en web se ven en plugin**
```
1. Admin crea NPC desde panel web
2. Jugador entra al mundo
3. NPC debe estar spawneado
4. Tiempo de sincronización: < 30 segundos
```

#### 6.4 Testing de Mundos Múltiples

**Test 9: Dos mundos con RPG**
```
1. Crear mundo1 con RPG
2. Crear mundo2 con RPG
3. Verificar que cada uno tiene su .db
4. Jugador en mundo1 → datos en mundo1.db
5. Jugador en mundo2 → datos en mundo2.db
6. No debe haber cross-contamination
```

---

## 📊 RESULTADO FINAL

### Estructura Completa
```
minecraft-server/
├── config/
│   ├── config.yml
│   ├── backup_config.json
│   ├── panel_config.json
│   ├── server.properties
│   └── templates/              ✅ NUEVA
│       ├── items_template.json
│       ├── mobs_template.json
│       ├── enchantments_template.json
│       ├── achievements_template.json
│       ├── crafting_template.json
│       ├── events_template.json
│       ├── pets_template.json
│       └── dungeons_template.json
│
├── plugins/
│   └── MMORPGPlugin/
│       └── data/
│           └── universal.db    ✅ CREADO POR PLUGIN
│
└── worlds/
    ├── active/ → (symlink)
    │   └── data/
    │       └── {world}.db      ✅ CREADO POR WEB
    └── {world_name}/
        └── data/
            └── {world}.db
```

### Cambios de Código

| Archivo | Cambios | Estado |
|---------|---------|--------|
| DatabaseManager.java | Refactorizado (2 conexiones) | ✅ |
| EnchantmentManager.java | Lee universal.db | ✅ |
| CraftingManager.java | Lee universal.db | ✅ |
| ItemManager.java | Lee universal.db | ✅ |
| MobManager.java | Lee universal.db | ✅ |
| QuestManager.java | Lee/escribe {world}.db | ✅ |
| NPCManager.java | Lee/escribe {world}.db | ✅ |
| ClassManager.java | Lee/escribe {world}.db | ✅ |
| EconomyManager.java | Lee/escribe {world}.db | ✅ |
| SquadManager.java | Lee/escribe {world}.db | ✅ |
| DataManager.java | ELIMINADO | ✅ |
| rpg_manager.py | Refactorizado (SQLite) | ✅ |
| world_manager.py | Crea {world}.db | ✅ |
| app.py | Rutas usan SQLite | ✅ |

### Flujos Verificados

✅ Plugin crea universal.db al iniciar (primera vez)  
✅ Plugin carga templates en universal.db  
✅ Web crea {world}.db al activar RPG  
✅ Plugin lee datos universales de universal.db  
✅ Plugin lee/escribe datos locales en {world}.db  
✅ Web lee datos universales de universal.db  
✅ Web lee/escribe datos locales en {world}.db  
✅ Sincronización en tiempo real funciona  
✅ Múltiples mundos con sus propias DBs  

---

## 🚀 COMENZAR IMPLEMENTACIÓN

Una vez aprobado este plan:

```bash
# Paso 1: Backup
cd /home/mkd/contenedores/mc-paper-docker
tar -czf backup_pre_sqlite_$(date +%Y%m%d_%H%M%S).tar.gz config/ plugins/ worlds/ web/

# Paso 2: Fase 0 - Preparación
mkdir -p config/templates
# Mover templates...

# Paso 3: Fase 1 - Limpieza
rm -rf config/data/ config/MMORPGPlugin/ config/api/ # etc.

# Paso 4: Fases 2-6 - Desarrollo
# Modificar código según este plan
```

---

**¿Listo para comenzar con Fase 0?**
