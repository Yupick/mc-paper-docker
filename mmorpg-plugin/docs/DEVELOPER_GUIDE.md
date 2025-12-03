# MMORPGPlugin - Guía del Desarrollador

## Tabla de Contenidos

1. [Introducción](#introducción)
2. [Configuración del Entorno](#configuración-del-entorno)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Compilación](#compilación)
5. [Arquitectura](#arquitectura)
6. [Extender el Plugin](#extender-el-plugin)
7. [Testing](#testing)
8. [Mejores Prácticas](#mejores-prácticas)
9. [Contribuir](#contribuir)

---

## Introducción

MMORPGPlugin es un plugin modular de MMORPG para Minecraft que implementa sistemas completos de RPG incluyendo clases, quests, NPCs, economía y más.

### Características Principales

- ✅ **6 clases RPG** con habilidades únicas
- ✅ **Sistema de quests** con objetivos y recompensas
- ✅ **NPCs interactivos** con diálogos y comercio
- ✅ **Economía dual** (Vault + monedas internas)
- ✅ **Base de datos SQLite** para persistencia
- ✅ **Multilenguaje** (ES/EN)
- ✅ **API REST** para panel web
- ✅ **Sistema de eventos** extensible
- ✅ **Auditoría completa** de acciones

---

## Configuración del Entorno

### Requisitos

- **Java Development Kit (JDK)**: 21 LTS
- **Maven**: 3.9+
- **Docker**: Para entorno de desarrollo containerizado
- **IDE**: IntelliJ IDEA / Eclipse / VS Code

### Instalación Local

```bash
# Clonar el repositorio
git clone <repository-url>
cd mc-paper/mmorpg-plugin

# Compilar con Maven
mvn clean package

# El JAR generado estará en target/mmorpg-plugin-1.0.0.jar
```

### Usando Docker

```bash
# Desde el directorio mc-paper
bash scripts/build-mmorpg-plugin.sh

# El plugin se copiará automáticamente a plugins/MMORPGPlugin.jar
```

---

## Estructura del Proyecto

```
mmorpg-plugin/
├── src/main/java/com/nightslayer/mmorpg/
│   ├── MMORPGPlugin.java              # Clase principal del plugin
│   ├── classes/                        # Sistema de clases
│   │   ├── ClassManager.java
│   │   ├── ClassType.java
│   │   ├── PlayerClass.java
│   │   ├── ClassAbility.java
│   │   └── AbilityCooldown.java
│   ├── quests/                         # Sistema de quests
│   │   ├── QuestManager.java
│   │   ├── Quest.java
│   │   ├── QuestObjective.java
│   │   ├── QuestReward.java
│   │   └── QuestStatus.java
│   ├── npcs/                           # Sistema de NPCs
│   │   ├── NPCManager.java
│   │   ├── CustomNPC.java
│   │   └── NPCType.java
│   ├── economy/                        # Sistema de economía
│   │   ├── EconomyManager.java
│   │   ├── ShopManager.java
│   │   └── ShopItem.java
│   ├── database/                       # Persistencia SQLite
│   │   └── DatabaseManager.java
│   ├── i18n/                           # Internacionalización
│   │   └── LanguageManager.java
│   ├── events/                         # Eventos personalizados
│   │   ├── RPGClassAssignedEvent.java
│   │   ├── QuestCompletedEvent.java
│   │   ├── NPCInteractEvent.java
│   │   ├── PlayerLevelUpEvent.java
│   │   ├── QuestAcceptedEvent.java
│   │   └── EconomyTransactionEvent.java
│   ├── audit/                          # Sistema de auditoría
│   │   └── AuditLogger.java
│   ├── api/                            # API REST
│   │   └── RPGAdminAPI.java
│   ├── commands/                       # Comandos del juego
│   │   ├── ClassCommand.java
│   │   ├── QuestCommand.java
│   │   └── RPGCommand.java
│   └── listeners/                      # Event listeners
│       └── PlayerListener.java
├── src/main/resources/
│   ├── plugin.yml                      # Configuración del plugin
│   ├── config.yml                      # Configuración por defecto
│   └── lang/                           # Archivos de idioma
│       ├── es_ES.yml
│       └── en_US.yml
├── pom.xml                             # Configuración Maven
├── docs/                               # Documentación
│   ├── API_REFERENCE.md
│   └── DEVELOPER_GUIDE.md
└── README.md
```

---

## Compilación

### Compilación Estándar

```bash
mvn clean package
```

### Compilación sin Tests

```bash
mvn clean package -DskipTests
```

### Compilación con Docker

```bash
bash scripts/build-mmorpg-plugin.sh
```

Este script:
1. Compila el plugin usando Maven en un contenedor Docker
2. Copia el JAR resultante a `plugins/MMORPGPlugin.jar`
3. Limpia archivos temporales

---

## Arquitectura

### Patrón de Diseño

El plugin utiliza una arquitectura modular basada en **Manager Pattern**:

```
MMORPGPlugin (Core)
    ↓
Managers (Business Logic)
    ↓
Data Layer (Database/JSON)
```

### Flujo de Datos

```
Player Action
    ↓
Command Handler
    ↓
Manager (ClassManager, QuestManager, etc.)
    ↓
Event System (Fire custom events)
    ↓
Database/Storage
    ↓
Audit Logger
```

### Gestores Principales

#### ClassManager

Responsable de:
- Asignar clases a jugadores
- Gestionar niveles y experiencia
- Manejar habilidades y cooldowns
- Persistir datos de clases

```java
public class ClassManager {
    private Map<UUID, PlayerClass> playerClasses;
    private MMORPGPlugin plugin;
    
    public void assignClass(Player player, ClassType type) {
        // Implementación
    }
}
```

#### QuestManager

Responsable de:
- Registrar y gestionar quests
- Trackear progreso de objetivos
- Distribuir recompensas
- Persistir estado de quests

```java
public class QuestManager {
    private Map<String, Quest> quests;
    private Map<UUID, List<Quest>> activeQuests;
    
    public void acceptQuest(Player player, String questId) {
        // Implementación
    }
}
```

#### DatabaseManager

Responsable de:
- Conexión a SQLite
- Ejecutar queries (sync/async)
- Crear/gestionar tablas
- Backups automáticos

```java
public class DatabaseManager {
    private Connection connection;
    private ExecutorService executor;
    
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        // Implementación
    }
}
```

---

## Extender el Plugin

### Agregar una Nueva Clase

1. **Agregar enum en ClassType.java:**

```java
public enum ClassType {
    // Clases existentes...
    NECROMANCER(75, 250, 30, 90, 40, 35); // HP, Mana, Str, Int, Agi, Def
    
    // Constructor y getters...
}
```

2. **Crear habilidades en ClassAbility.java:**

```java
public class ClassAbility {
    public void executeEffect(Player player, ClassType classType) {
        switch (this.id) {
            case "SUMMON_SKELETON":
                summonSkeleton(player);
                break;
            // Otras habilidades...
        }
    }
    
    private void summonSkeleton(Player player) {
        Location loc = player.getLocation();
        Skeleton skeleton = (Skeleton) player.getWorld().spawnEntity(loc, EntityType.SKELETON);
        // Configurar skeleton como aliado
    }
}
```

3. **Registrar habilidades en ClassManager:**

```java
private void registerDefaultAbilities() {
    // Necromancer abilities
    abilities.put("SUMMON_SKELETON", new ClassAbility(
        "SUMMON_SKELETON",
        "Summon Skeleton",
        "Summons a skeleton minion",
        100.0,  // Mana cost
        30      // Cooldown
    ));
}
```

### Agregar Nuevo Tipo de Quest Objective

1. **Agregar tipo en QuestObjectiveType.java:**

```java
public enum QuestObjectiveType {
    KILL,
    COLLECT,
    DELIVER,
    EXPLORE,        // Nuevo
    CRAFT          // Nuevo
}
```

2. **Implementar lógica en QuestManager:**

```java
public void updateObjectiveProgress(Player player, String questId, String objectiveId, int amount) {
    Quest quest = getActiveQuest(player, questId);
    QuestObjective objective = quest.getObjective(objectiveId);
    
    switch (objective.getType()) {
        case EXPLORE:
            handleExploreObjective(player, objective, amount);
            break;
        case CRAFT:
            handleCraftObjective(player, objective, amount);
            break;
    }
}
```

3. **Agregar listener para eventos:**

```java
@EventHandler
public void onPlayerCraft(CraftItemEvent event) {
    Player player = (Player) event.getWhoClicked();
    ItemStack item = event.getCurrentItem();
    
    // Actualizar progreso de quests con objetivo CRAFT
    questManager.checkCraftObjectives(player, item);
}
```

### Crear un Nuevo Manager

Ejemplo: **PetManager** para sistema de mascotas

```java
package com.nightslayer.mmorpg.pets;

import org.bukkit.entity.Player;
import java.util.*;

public class PetManager {
    private MMORPGPlugin plugin;
    private Map<UUID, Pet> activePets;
    
    public PetManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.activePets = new HashMap<>();
        loadPets();
    }
    
    public void spawnPet(Player player, PetType type) {
        Pet pet = new Pet(player, type);
        pet.spawn();
        activePets.put(player.getUniqueId(), pet);
        
        // Fire custom event
        PetSpawnedEvent event = new PetSpawnedEvent(player, pet);
        Bukkit.getPluginManager().callEvent(event);
    }
    
    public void despawnPet(Player player) {
        Pet pet = activePets.remove(player.getUniqueId());
        if (pet != null) {
            pet.despawn();
        }
    }
    
    private void loadPets() {
        // Cargar desde base de datos
    }
    
    public void saveAll() {
        // Guardar en base de datos
    }
}
```

Luego registrar en **MMORPGPlugin.java:**

```java
private PetManager petManager;

@Override
public void onEnable() {
    // Otros managers...
    petManager = new PetManager(this);
}

public PetManager getPetManager() {
    return petManager;
}
```

---

## Testing

### Unit Tests

Crear tests en `src/test/java/`:

```java
public class ClassManagerTest {
    private ClassManager classManager;
    private MMORPGPlugin plugin;
    
    @Before
    public void setUp() {
        plugin = mock(MMORPGPlugin.class);
        classManager = new ClassManager(plugin);
    }
    
    @Test
    public void testAssignClass() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        
        classManager.assignClass(player, ClassType.WARRIOR);
        
        PlayerClass playerClass = classManager.getPlayerClass(player);
        assertNotNull(playerClass);
        assertEquals(ClassType.WARRIOR, playerClass.getClassType());
    }
}
```

### Ejecutar Tests

```bash
mvn test
```

---

## Mejores Prácticas

### 1. Uso de Async para Operaciones de I/O

```java
// ❌ MAL - Bloquea el thread principal
public void savePlayer(Player player) {
    databaseManager.executeUpdate("UPDATE players SET ...");
}

// ✅ BIEN - Operación asíncrona
public void savePlayer(Player player) {
    databaseManager.executeUpdateAsync("UPDATE players SET ...")
        .thenAccept(rows -> {
            plugin.getLogger().info("Player saved: " + rows + " rows affected");
        });
}
```

### 2. Manejo de Eventos

```java
// ✅ Fire eventos personalizados para extensibilidad
public void completeQuest(Player player, Quest quest) {
    // Lógica de completar quest...
    
    // Fire event para que otros plugins puedan escuchar
    QuestCompletedEvent event = new QuestCompletedEvent(player, quest);
    Bukkit.getPluginManager().callEvent(event);
    
    if (!event.isCancelled()) {
        giveRewards(player, quest);
    }
}
```

### 3. Internacionalización

```java
// ❌ MAL - Texto hardcodeado
player.sendMessage("You have chosen the Warrior class!");

// ✅ BIEN - Usando LanguageManager
String message = languageManager.getMessage(
    player,
    "classes.choose_success",
    "Warrior"
);
player.sendMessage(message);
```

### 4. Validación de Datos

```java
public void acceptQuest(Player player, String questId) {
    // Validaciones
    if (player == null) {
        throw new IllegalArgumentException("Player cannot be null");
    }
    
    Quest quest = quests.get(questId);
    if (quest == null) {
        player.sendMessage(langManager.getMessage(player, "quests.not_found"));
        return;
    }
    
    if (getActiveQuests(player).size() >= MAX_ACTIVE_QUESTS) {
        player.sendMessage(langManager.getMessage(player, "quests.quest_full"));
        return;
    }
    
    // Lógica...
}
```

### 5. Logging y Auditoría

```java
public void buyItem(Player player, ShopItem item, int amount) {
    double cost = item.getPrice() * amount;
    
    if (economyManager.withdraw(player, cost)) {
        // Dar items...
        
        // Log de auditoría
        auditLogger.logEconomyAction(
            player,
            AuditSeverity.MEDIUM,
            String.format("Compra: %dx %s por %.2f", amount, item.getName(), cost)
        );
    }
}
```

---

## Contribuir

### Flujo de Trabajo Git

```bash
# 1. Crear rama para feature
git checkout -b feature/pet-system

# 2. Hacer cambios y commits
git add .
git commit -m "feat: Add pet spawning system"

# 3. Push y crear PR
git push origin feature/pet-system
```

### Convenciones de Código

- **Indentación**: 4 espacios
- **Naming**:
  - Clases: `PascalCase`
  - Métodos: `camelCase`
  - Constantes: `UPPER_SNAKE_CASE`
- **JavaDoc**: Obligatorio para métodos públicos

```java
/**
 * Asigna una clase RPG a un jugador
 * 
 * @param player El jugador al que asignar la clase
 * @param classType El tipo de clase a asignar
 * @throws IllegalStateException si el jugador ya tiene una clase
 */
public void assignClass(Player player, ClassType classType) {
    // Implementación
}
```

---

## Roadmap Futuro

### Fase 5: Contenido Avanzado (Próximamente)
- [ ] Sistema de Dungeons con generación procedural
- [ ] Sistema de Raids con 10+ jugadores
- [ ] Boss fights con mecánicas especiales
- [ ] Sistema de crafting avanzado
- [ ] Sistema de encantamientos custom

### Fase 6: Optimización y Escalado
- [ ] Migración a Redis para cache distribuido
- [ ] Sharding de base de datos
- [ ] Optimización de rendimiento (1000+ jugadores)
- [ ] Clustering multi-servidor

---

## Recursos Adicionales

- [PaperMC API Documentation](https://papermc.io/javadocs)
- [Bukkit/Spigot API Reference](https://hub.spigotmc.org/javadocs/spigot/)
- [SQLite Documentation](https://www.sqlite.org/docs.html)
- [Vault API Documentation](https://github.com/MilkBowl/VaultAPI)

---

**Happy Coding! 🎮**
