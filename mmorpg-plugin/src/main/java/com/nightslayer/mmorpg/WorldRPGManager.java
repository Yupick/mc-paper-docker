package com.nightslayer.mmorpg;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WorldRPGManager {
    
    private final MMORPGPlugin plugin;
    private final Map<String, WorldMetadata> rpgWorlds;
    private final Gson gson;
    
    public WorldRPGManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.rpgWorlds = new HashMap<>();
        this.gson = plugin.getGson();
    }
    
    /**
     * Carga la metadata de un mundo desde su archivo metadata.json
     */
    public WorldMetadata loadWorldMetadata(String worldSlug) {
        String worldsBasePath = plugin.getConfig().getString("worlds.base-path", "/server/worlds");
        File metadataFile = new File(worldsBasePath + "/" + worldSlug + "/metadata.json");
        
        if (!metadataFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(metadataFile)) {
            return gson.fromJson(reader, WorldMetadata.class);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al leer metadata de mundo: " + worldSlug, e);
            return null;
        }
    }
    
    /**
     * Registra un mundo como RPG activo
     */
    public void registerRPGWorld(String worldSlug, WorldMetadata metadata) {
        rpgWorlds.put(worldSlug, metadata);
        
        // Crear estructura de datos para este mundo si no existe
        File worldDataDir = new File(plugin.getDataFolder(), "data/" + worldSlug);
        if (!worldDataDir.exists()) {
            worldDataDir.mkdirs();
            
            // Crear archivos base
            createBaseDataFiles(worldSlug);
        }
    }
    
    /**
     * Crea archivos de datos base para un mundo RPG
     */
    private void createBaseDataFiles(String worldSlug) {
        File worldDataDir = new File(plugin.getDataFolder(), "data/" + worldSlug);
        
        // Crear status.json
        File statusFile = new File(worldDataDir, "status.json");
        if (!statusFile.exists()) {
            plugin.getDataManager().createDefaultStatusFile(worldSlug);
        }
        
        // Crear players.json
        File playersFile = new File(worldDataDir, "players.json");
        if (!playersFile.exists()) {
            plugin.getDataManager().createDefaultPlayersFile(worldSlug);
        }
        
        plugin.getLogger().info("Archivos de datos creados para mundo RPG: " + worldSlug);
    }
    
    /**
     * Verifica si un mundo tiene el modo RPG activado
     */
    public boolean isRPGWorld(String worldName) {
        return rpgWorlds.containsKey(worldName);
    }
    
    /**
     * Obtiene la metadata de un mundo RPG
     */
    public WorldMetadata getWorldMetadata(String worldSlug) {
        return rpgWorlds.get(worldSlug);
    }
    
    /**
     * Obtiene todos los mundos RPG registrados
     */
    public Map<String, WorldMetadata> getAllRPGWorlds() {
        return new HashMap<>(rpgWorlds);
    }
}
