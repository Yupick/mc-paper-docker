# MMORPGPlugin - Documentación de API

## Descripción General

MMORPGPlugin es un plugin completo de MMORPG para servidores Minecraft PaperMC 1.21.1 que proporciona:

- **Sistema de clases RPG**: 6 clases jugables con habilidades únicas
- **Sistema de quests**: Misiones con objetivos y recompensas
- **Sistema de NPCs**: NPCs interactivos para quests y comercio
- **Sistema de economía**: Integración con Vault + monedas RPG internas
- **Sistema de tiendas**: 4 tiendas NPCs con items especializados
- **Panel web administrativo**: API REST para administración
- **Multilenguaje**: Soporte para español e inglés
- **Base de datos SQLite**: Persistencia eficiente de datos
- **Sistema de auditoría**: Logging completo de acciones
- **Eventos personalizados**: Extensibilidad para otros plugins

---

## Arquitectura del Plugin

### Componentes Principales

```
MMORPGPlugin
├── Classes System (classes/)
│   ├── ClassManager
│   ├── ClassType
│   ├── PlayerClass
│   ├── ClassAbility
│   └── AbilityCooldown
├── Quest System (quests/)
│   ├── QuestManager
│   ├── Quest
│   ├── QuestObjective
│   └── QuestStatus
├── NPC System (npcs/)
│   ├── NPCManager
│   ├── CustomNPC
│   └── NPCType
├── Economy System (economy/)
│   ├── EconomyManager
│   ├── ShopManager
│   └── ShopItem
├── Database (database/)
│   └── DatabaseManager
├── Internationalization (i18n/)
│   └── LanguageManager
├── Events (events/)
│   ├── RPGClassAssignedEvent
│   ├── QuestCompletedEvent
│   ├── NPCInteractEvent
│   ├── PlayerLevelUpEvent
│   ├── QuestAcceptedEvent
│   └── EconomyTransactionEvent
├── Audit (audit/)
│   └── AuditLogger
└── API (api/)
    └── RPGAdminAPI
```

---

## Guía de Uso de API

### 1. Sistema de Clases

#### Obtener el ClassManager

```java
ClassManager classManager = MMORPGPlugin.getInstance().getClassManager();
```

#### Asignar una clase a un jugador

```java
Player player = ...;
ClassType classType = ClassType.WARRIOR;

classManager.assignClass(player, classType);
```

#### Obtener la clase de un jugador

```java
PlayerClass playerClass = classManager.getPlayerClass(player);

if (playerClass != null) {
    ClassType type = playerClass.getClassType();
    int level = playerClass.getLevel();
    int exp = playerClass.getExperience();
}
```

#### Usar una habilidad

```java
ClassAbility ability = playerClass.getAbility("SHIELD_BASH");
if (ability != null) {
    ability.execute(player);
}
```

#### Clases Disponibles

| Clase | HP Base | Maná Base | Características |
|-------|---------|-----------|-----------------|
| `WARRIOR` | 120 | 50 | Tank, alto daño físico |
| `MAGE` | 80 | 200 | DPS mágico, control |
| `ARCHER` | 90 | 80 | DPS a distancia |
| `ASSASSIN` | 85 | 110 | Alto crítico, sigilo |
| `CLERIC` | 95 | 180 | Healer, soporte |
| `PALADIN` | 115 | 140 | Tank/Healer híbrido |

---

### 2. Sistema de Quests

#### Obtener el QuestManager

```java
QuestManager questManager = MMORPGPlugin.getInstance().getQuestManager();
```

#### Crear una quest

```java
Quest quest = new Quest(
    "quest_001",              // ID único
    "Slime Hunter",           // Nombre
    "Kill 10 slimes",         // Descripción
    1,                        // Nivel mínimo
    "npc_villager_1",         // ID del NPC que da la quest
    QuestDifficulty.EASY      // Dificultad
);

// Agregar objetivos
quest.addObjective(new QuestObjective(
    "obj_1",
    QuestObjectiveType.KILL,
    "SLIME",
    10
));

// Agregar recompensas
quest.addReward(new QuestReward(
    QuestRewardType.EXPERIENCE,
    100.0
));

questManager.registerQuest(quest);
```

#### Aceptar una quest

```java
Player player = ...;
String questId = "quest_001";

questManager.acceptQuest(player, questId);
```

#### Actualizar progreso

```java
questManager.updateObjectiveProgress(
    player,
    "quest_001",
    "obj_1",
    1  // Incremento
);
```

#### Completar una quest

```java
questManager.completeQuest(player, "quest_001");
```

---

### 3. Sistema de NPCs

#### Obtener el NPCManager

```java
NPCManager npcManager = MMORPGPlugin.getInstance().getNPCManager();
```

#### Crear un NPC

```java
Location location = ...;

CustomNPC npc = new CustomNPC(
    "npc_villager_1",         // ID único
    "Merchant Bob",           // Nombre
    NPCType.VILLAGER,         // Tipo
    location
);

npc.setQuestId("quest_001");  // Asignar quest
npc.setDialogue("Hello traveler!");

npcManager.registerNPC(npc);
npcManager.spawnNPC(npc);
```

#### Tipos de NPC

- `VILLAGER`: Comerciante / Quest giver
- `GUARD`: NPC de combate / Protector
- `TRAINER`: Entrenador de habilidades
- `QUESTGIVER`: Especializado en dar quests

---

### 4. Sistema de Economía

#### Obtener el EconomyManager

```java
EconomyManager economyManager = MMORPGPlugin.getInstance().getEconomyManager();
```

#### Operaciones de economía

```java
Player player = ...;

// Obtener balance
double balance = economyManager.getBalance(player);

// Depositar dinero
economyManager.deposit(player, 100.0);

// Retirar dinero
if (economyManager.has(player, 50.0)) {
    economyManager.withdraw(player, 50.0);
}

// Transferir entre jugadores
Player target = ...;
economyManager.transfer(player, target, 25.0);
```

#### Sistema de Tiendas

```java
ShopManager shopManager = MMORPGPlugin.getInstance().getShopManager();

// Comprar item
ShopItem item = ...;
shopManager.buyItem(player, item, 1);

// Vender item
ItemStack itemStack = ...;
shopManager.sellItem(player, itemStack, 5);

// Obtener tienda
Map<String, ShopItem> generalShop = shopManager.getShop("general");
```

---

### 5. Base de Datos SQLite

#### Obtener el DatabaseManager

```java
DatabaseManager dbManager = MMORPGPlugin.getInstance().getDatabaseManager();
```

#### Ejecutar consultas

```java
// INSERT/UPDATE de forma asíncrona
dbManager.executeUpdateAsync(
    "INSERT INTO players (uuid, name, class_type, level) VALUES (?, ?, ?, ?)",
    player.getUniqueId().toString(),
    player.getName(),
    "WARRIOR",
    1
).thenAccept(rowsAffected -> {
    // Operación completada
});

// SELECT
ResultSet rs = dbManager.executeQuery(
    "SELECT * FROM players WHERE uuid = ?",
    player.getUniqueId().toString()
);

if (rs != null && rs.next()) {
    String classType = rs.getString("class_type");
    int level = rs.getInt("level");
}
```

#### Backup de la base de datos

```java
dbManager.backup();  // Crea backup automático con timestamp
```

---

### 6. Sistema de Internacionalización

#### Obtener el LanguageManager

```java
LanguageManager langManager = MMORPGPlugin.getInstance().getLanguageManager();
```

#### Obtener mensajes traducidos

```java
Player player = ...;

// Mensaje simple
String message = langManager.getMessage(player, "classes.choose_success", "Warrior");
player.sendMessage(message);

// Mensaje con múltiples placeholders
String questMsg = langManager.getMessage(
    player,
    "quests.objective_progress",
    "Kill Slimes",
    5,
    10
);
```

#### Cambiar idioma del jugador

```java
langManager.setPlayerLanguage(player, "en_US");  // Cambiar a inglés
langManager.setPlayerLanguage(player, "es_ES");  // Cambiar a español
```

#### Idiomas disponibles

- `es_ES` - Español (predeterminado)
- `en_US` - English

---

### 7. Sistema de Eventos

El plugin dispara eventos personalizados que otros plugins pueden escuchar:

#### RPGClassAssignedEvent

```java
@EventHandler
public void onClassAssigned(RPGClassAssignedEvent event) {
    Player player = event.getPlayer();
    ClassType classType = event.getClassType();
    
    // Cancelar si es necesario
    if (someCondition) {
        event.setCancelled(true);
    }
}
```

#### QuestCompletedEvent

```java
@EventHandler
public void onQuestComplete(QuestCompletedEvent event) {
    Player player = event.getPlayer();
    Quest quest = event.getQuest();
    
    player.sendMessage("¡Felicidades por completar: " + quest.getName() + "!");
}
```

#### PlayerLevelUpEvent

```java
@EventHandler
public void onLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    int oldLevel = event.getOldLevel();
    int newLevel = event.getNewLevel();
    
    // Dar recompensa especial cada 5 niveles
    if (newLevel % 5 == 0) {
        giveSpecialReward(player);
    }
}
```

#### NPCInteractEvent

```java
@EventHandler
public void onNPCInteract(NPCInteractEvent event) {
    Player player = event.getPlayer();
    CustomNPC npc = event.getNpc();
    
    // Custom logic
}
```

#### EconomyTransactionEvent

```java
@EventHandler
public void onTransaction(EconomyTransactionEvent event) {
    Player player = event.getPlayer();
    double amount = event.getAmount();
    
    // Log transaction
}
```

---

### 8. Sistema de Auditoría

#### Obtener el AuditLogger

```java
AuditLogger auditLogger = new AuditLogger(plugin);
```

#### Registrar acciones

```java
// Acción de clase
auditLogger.logClassAction(
    player,
    AuditSeverity.MEDIUM,
    "Clase asignada: WARRIOR"
);

// Acción de quest
auditLogger.logQuestAction(
    player,
    AuditSeverity.LOW,
    "Quest 'quest_001' completada"
);

// Transacción económica
auditLogger.logEconomyAction(
    player,
    AuditSeverity.HIGH,
    "Compra de Diamond Sword por 1000 monedas"
);

// Error
auditLogger.logError(
    "Error cargando NPC",
    exception
);
```

#### Obtener estadísticas

```java
String stats = auditLogger.getStatistics(7);  // Últimos 7 días
System.out.println(stats);
```

---

### 9. API REST (Web Admin)

El plugin expone una API REST para administración web:

#### Endpoints Disponibles

**Quests:**

- `GET /api/rpg/quests` - Listar todas las quests
- `POST /api/rpg/quest/create` - Crear nueva quest
- `PUT /api/rpg/quest/update` - Actualizar quest
- `DELETE /api/rpg/quest/delete?id=quest_001` - Eliminar quest

**NPCs:**

- `GET /api/rpg/npcs` - Listar todos los NPCs
- `POST /api/rpg/npc/create` - Crear nuevo NPC
- `DELETE /api/rpg/npc/delete?id=npc_001` - Eliminar NPC

#### Usar la API desde Java

```java
RPGAdminAPI adminAPI = MMORPGPlugin.getInstance().getAdminAPI();

// Crear quest vía JSON
String questJson = """
{
    "id": "quest_002",
    "name": "Dragon Slayer",
    "description": "Kill the dragon",
    "minLevel": 10,
    "difficulty": "HARD"
}
""";

String result = adminAPI.processCreateQuest(questJson);
```

---

## Configuración

El plugin se configura mediante `config.yml`:

```yaml
plugin:
  debug: false
  
worlds:
  base-path: /server/worlds
  
web-panel:
  enabled: true
  sync-interval: 30  # segundos
  
economy:
  vault-enabled: true
  starting-balance: 100.0
  
language:
  default: es_ES
```

---

## Dependencias

- **PaperMC**: 1.21.1-R0.1-SNAPSHOT (required)
- **Vault API**: 1.7 (optional, para economía)
- **Gson**: 2.10.1 (incluido)
- **SQLite JDBC**: 3.44.1.0 (incluido)

---

## Permisos

```yaml
mmorpg.class.choose: Permite elegir clase
mmorpg.class.info: Ver información de clases
mmorpg.quest.accept: Aceptar quests
mmorpg.quest.complete: Completar quests
mmorpg.admin.reload: Recargar plugin
mmorpg.admin.quest: Administrar quests
mmorpg.admin.npc: Administrar NPCs
```

---

## Soporte y Desarrollo

- **Versión**: 1.0.0
- **Java**: 21 LTS
- **Minecraft**: 1.21.1 (Paper)
- **License**: Propietario

---

## Ejemplos Completos

### Crear un Sistema de Clanes

```java
public class ClanIntegration implements Listener {
    
    @EventHandler
    public void onClassAssigned(RPGClassAssignedEvent event) {
        Player player = event.getPlayer();
        
        // Dar bonus de clan según clase
        if (isInClan(player)) {
            ClassType type = event.getClassType();
            giveClanBonus(player, type);
        }
    }
    
    @EventHandler
    public void onQuestComplete(QuestCompletedEvent event) {
        // Dar puntos de clan
        addClanPoints(event.getPlayer(), 10);
    }
}
```

### Sistema de Dungeons

```java
public class DungeonManager {
    private MMORPGPlugin plugin;
    
    public void createDungeon(String name, Location entrance) {
        // Crear NPC de entrada
        CustomNPC dungeonNPC = new CustomNPC(
            "dungeon_" + name,
            "Dungeon Portal",
            NPCType.GUARD,
            entrance
        );
        
        plugin.getNPCManager().registerNPC(dungeonNPC);
        
        // Crear quest de dungeon
        Quest dungeonQuest = new Quest(
            "dungeon_" + name,
            "Clear " + name,
            "Defeat all enemies",
            10,
            dungeonNPC.getId(),
            QuestDifficulty.HARD
        );
        
        plugin.getQuestManager().registerQuest(dungeonQuest);
    }
}
```

---

**Fin de la documentación de API**
