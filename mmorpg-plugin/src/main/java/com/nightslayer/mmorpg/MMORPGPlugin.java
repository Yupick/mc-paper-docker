package com.nightslayer.mmorpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nightslayer.mmorpg.api.RPGAdminAPI;
import com.nightslayer.mmorpg.database.DatabaseManager;
import com.nightslayer.mmorpg.classes.ClassManager;
import com.nightslayer.mmorpg.commands.ClassCommand;
import com.nightslayer.mmorpg.commands.QuestCommand;
import com.nightslayer.mmorpg.economy.EconomyManager;
import com.nightslayer.mmorpg.economy.ShopManager;
import com.nightslayer.mmorpg.i18n.LanguageManager;
import com.nightslayer.mmorpg.npcs.NPCManager;
import com.nightslayer.mmorpg.quests.QuestManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import java.io.File;
import java.util.logging.Level;

public class MMORPGPlugin extends JavaPlugin {
    
    private static MMORPGPlugin instance;
    private Gson gson;
    private WorldRPGManager worldRPGManager;
    private DataManager dataManager;
    private ClassManager classManager;
    private NPCManager npcManager;
    private QuestManager questManager;
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
        economyManager = new EconomyManager(this);
        classManager = new ClassManager(this);
        shopManager = new ShopManager(this, economyManager);
        npcManager = new NPCManager(this);
        questManager = new QuestManager(this, classManager);
        adminAPI = new RPGAdminAPI(this);
        
        // Detectar mundos RPG
        detectRPGWorlds();
        
        // Registrar comandos
        getCommand("rpg").setExecutor(new RPGCommand(this));
        getCommand("class").setExecutor(new ClassCommand(classManager));
        getCommand("quest").setExecutor(new QuestCommand(questManager));
        
        // Registrar eventos de NPCs
        getServer().getPluginManager().registerEvents(npcManager, this);
        
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
}
