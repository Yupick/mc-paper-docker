package com.nightslayer.mmorpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nightslayer.mmorpg.api.RPGAdminAPI;
import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.bestiary.BestiaryManager;
import com.nightslayer.mmorpg.achievements.AchievementManager;
import com.nightslayer.mmorpg.classes.ClassManager;
import com.nightslayer.mmorpg.commands.ClassCommand;
import com.nightslayer.mmorpg.commands.QuestCommand;
import com.nightslayer.mmorpg.crafting.CraftingManager;
import com.nightslayer.mmorpg.enchanting.EnchantmentManager;
import com.nightslayer.mmorpg.pets.PetManager;
import com.nightslayer.mmorpg.economy.EconomyManager;
import com.nightslayer.mmorpg.economy.ShopManager;
import com.nightslayer.mmorpg.events.EventManager;
import com.nightslayer.mmorpg.dungeons.DungeonManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import com.nightslayer.mmorpg.invasions.InvasionManager;
import com.nightslayer.mmorpg.items.ItemManager;
import com.nightslayer.mmorpg.listeners.MobDeathListener;
import com.nightslayer.mmorpg.mobs.MobManager;
import com.nightslayer.mmorpg.ranks.RankManager;
import com.nightslayer.mmorpg.respawn.RespawnManager;
import com.nightslayer.mmorpg.npcs.NPCManager;
import com.nightslayer.mmorpg.quests.QuestManager;
import com.nightslayer.mmorpg.squads.SquadManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import java.io.File;
import java.util.logging.Level;

public class MMORPGPlugin extends JavaPlugin {
    
    private static MMORPGPlugin instance;
    private Gson gson;
    private WorldRPGManager worldRPGManager;
    private DataManager dataManager;
    private PathResolver pathResolver;
    private DataInitializer dataInitializer;
    private ClassManager classManager;
    private NPCManager npcManager;
    private MobManager mobManager;
    private ItemManager itemManager;
    private QuestManager questManager;
    private BestiaryManager bestiaryManager;
    private AchievementManager achievementManager;
    private RankManager rankManager;
    private InvasionManager invasionManager;
    private EventManager eventManager;
    private DungeonManager dungeonManager;
    private SquadManager squadManager;
    private CraftingManager craftingManager;
    private EnchantmentManager enchantmentManager;
    private PetManager petManager;
    private RespawnManager respawnManager;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private RPGAdminAPI adminAPI;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializar Gson
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        // Guardar configuración por defecto si no existe
        saveDefaultConfig();
        
        // Crear directorios de datos
        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Inicializar gestores
        languageManager = new LanguageManager(this);
        databaseManager = new DatabaseManager(this);
        worldRPGManager = new WorldRPGManager(this);
        dataManager = new DataManager(this);
        
        // Inicializar resolvedores de rutas
        pathResolver = new PathResolver(this);
        dataInitializer = new DataInitializer(this, pathResolver);
        
        economyManager = new EconomyManager(this);
        classManager = new ClassManager(this);
        shopManager = new ShopManager(this, economyManager);
        npcManager = new NPCManager(this);
        mobManager = new MobManager(this);
        itemManager = new ItemManager(this);
        bestiaryManager = new BestiaryManager(this);
        achievementManager = new AchievementManager(this);
        rankManager = new RankManager(this, achievementManager);
        invasionManager = new InvasionManager(this);
        eventManager = new EventManager(this, mobManager, economyManager);
        dungeonManager = new DungeonManager(this, mobManager, economyManager);
        squadManager = new SquadManager(this);
        craftingManager = new CraftingManager(this);
        enchantmentManager = new EnchantmentManager(this);
        petManager = new PetManager(this);
        questManager = new QuestManager(this, classManager);
        respawnManager = new RespawnManager(this, mobManager);
        adminAPI = new RPGAdminAPI(this);
        
        // Detectar mundos RPG
        detectRPGWorlds();
        
        // Registrar comandos
        getCommand("rpg").setExecutor(new RPGCommand(this));
        getCommand("class").setExecutor(new ClassCommand(classManager));
        getCommand("quest").setExecutor(new QuestCommand(questManager));
        
        // Registrar eventos de NPCs
        getServer().getPluginManager().registerEvents(npcManager, this);
        
        // Registrar listener de muerte de mobs custom
        getServer().getPluginManager().registerEvents(new MobDeathListener(this, mobManager, itemManager, bestiaryManager, achievementManager, invasionManager, eventManager), this);
        
        // Iniciar sincronización con panel web
        startWebPanelSync();
        
        // Spawnear NPCs
        npcManager.spawnAll();
        
        getLogger().info("MMORPGPlugin habilitado correctamente!");
        if (getConfig().getBoolean("plugin.debug", false)) {
            getLogger().info("Modo debug activado");
        }
    }
    
    @Override
    public void onDisable() {
                // Cerrar base de datos
                if (databaseManager != null) {
                    databaseManager.close();
                }
        
        // Guardar datos antes de desactivar
        if (dataManager != null) {
            dataManager.saveAllData();
        }
        
        if (classManager != null) {
            classManager.saveAll();
        }
        
        if (questManager != null) {
            questManager.saveAll();
        }
        
        if (npcManager != null) {
            npcManager.despawnAll();
            npcManager.saveAll();
        }
        
        // Detener respawn manager
        if (respawnManager != null) {
            respawnManager.shutdown();
        }

        if (bestiaryManager != null) {
            bestiaryManager.saveAll();
        }

        if (achievementManager != null) {
            achievementManager.saveAll();
        }

        if (rankManager != null) {
            rankManager.saveAll();
        }

        if (invasionManager != null) {
            invasionManager.shutdown();
        }

        if (eventManager != null) {
            eventManager.shutdown();
        }

        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }

        if (squadManager != null) {
            squadManager.shutdown();
        }

        if (craftingManager != null) {
            craftingManager.shutdown();
        }

        if (enchantmentManager != null) {
            enchantmentManager.shutdown();
        }
        
        getLogger().info("MMORPGPlugin deshabilitado correctamente!");
    }
    
    /**
     * Detecta qué mundos tienen el modo RPG activado
     */
    private void detectRPGWorlds() {
        String worldsBasePath = getConfig().getString("worlds.base-path", "/server/worlds");
        File worldsDir = new File(worldsBasePath);
        
        if (!worldsDir.exists()) {
            getLogger().warning("Directorio de mundos no encontrado: " + worldsBasePath);
            return;
        }
        
        int rpgWorldsCount = 0;
        
        // Iterar sobre los subdirectorios (mundos)
        File[] worldFolders = worldsDir.listFiles(File::isDirectory);
        if (worldFolders != null) {
            for (File worldFolder : worldFolders) {
                File metadataFile = new File(worldFolder, "metadata.json");
                
                if (metadataFile.exists()) {
                    WorldMetadata metadata = worldRPGManager.loadWorldMetadata(worldFolder.getName());
                    
                    if (metadata != null && metadata.isRPG()) {
                        worldRPGManager.registerRPGWorld(worldFolder.getName(), metadata);
                        rpgWorldsCount++;
                        
                        if (getConfig().getBoolean("plugin.debug", false)) {
                            getLogger().info("Mundo RPG detectado: " + worldFolder.getName());
                        }
                    }
                }
            }
        }
        
        getLogger().info("Detectados " + rpgWorldsCount + " mundos con modo RPG activado");
    }
    
    /**
     * Inicia la sincronización periódica con el panel web
     */
    private void startWebPanelSync() {
        if (!getConfig().getBoolean("web-panel.enabled", true)) {
            return;
        }
        
        int syncInterval = getConfig().getInt("web-panel.sync-interval", 30) * 20; // Convertir a ticks
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                dataManager.exportDataForWebPanel();
                adminAPI.exportStatus();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error al sincronizar con panel web", e);
            }
        }, syncInterval, syncInterval);
        
        // Procesar comandos API cada 5 segundos
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                adminAPI.processCommands();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error procesando comandos API", e);
            }
        }, 100L, 100L);
    }
    
    /**
     * Verifica si un mundo tiene el modo RPG activado
     */
    public boolean isRPGWorld(World world) {
        return worldRPGManager.isRPGWorld(world.getName());
    }
    
    /**
     * Verifica si un mundo (por nombre) tiene el modo RPG activado
     */
    public boolean isRPGWorld(String worldName) {
        return worldRPGManager.isRPGWorld(worldName);
    }
    
    // Getters
    public static MMORPGPlugin getInstance() {
        return instance;
    }
    
    public Gson getGson() {
        return gson;
    }
    
    public WorldRPGManager getWorldRPGManager() {
        return worldRPGManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public ClassManager getClassManager() {
        return classManager;
    }
    
    public NPCManager getNPCManager() {
        return npcManager;
    }
    
    public MobManager getMobManager() {
        return mobManager;
    }
    
    public RespawnManager getRespawnManager() {
        return respawnManager;
    }
    
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public RPGAdminAPI getAdminAPI() {
        return adminAPI;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public BestiaryManager getBestiaryManager() {
        return bestiaryManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public InvasionManager getInvasionManager() {
        return invasionManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public SquadManager getSquadManager() {
        return squadManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public DatabaseManager getDatabase() {
        return databaseManager;
    }
    
    public PathResolver getPathResolver() {
        return pathResolver;
    }
    
    public DataInitializer getDataInitializer() {
        return dataInitializer;
    }
}
