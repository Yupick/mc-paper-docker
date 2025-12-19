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
    private final RPGPathResolver pathResolver;
    
    public WorldRPGManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.rpgWorlds = new HashMap<>();
        this.gson = plugin.getGson();
        this.pathResolver = new RPGPathResolver(plugin);
    }
    
    /**
     * Obtiene el path resolver
     */
    public RPGPathResolver getPathResolver() {
        return pathResolver;
    }
    
    /**
     * Carga la metadata de un mundo desde su archivo metadata.json
     */
    public WorldMetadata loadWorldMetadata(String worldName) {
        String worldsBasePath = plugin.getConfig().getString("worlds.base-path", "/server/worlds");
        File metadataFile = new File(worldsBasePath + "/" + worldName + "/metadata.json");
        
        if (!metadataFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(metadataFile)) {
            return gson.fromJson(reader, WorldMetadata.class);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al leer metadata de mundo: " + worldName, e);
            return null;
        }
    }
    
    /**
     * Registra un mundo como RPG activo
     * Crea estructura: worlds/{worldName}/data/ para archivos locales
     */
    public void registerRPGWorld(String worldName, WorldMetadata metadata) {
        rpgWorlds.put(worldName, metadata);
        
        // Crear directorio de datos locales del mundo si no existe
        if (pathResolver.ensureWorldDataDirExists(worldName)) {
            // Crear archivos base locales
            createBaseDataFiles(worldName);
            plugin.getLogger().info("Mundo RPG registrado: " + worldName);
        } else {
            plugin.getLogger().warning("No se pudo crear directorio de datos para: " + worldName);
        }
    }
    
    /**
     * Crea archivos de datos base para un mundo RPG
     * Archivos locales en worlds/{worldName}/data/
     */
    private void createBaseDataFiles(String worldName) {
        // Archivos locales del mundo
        String[] localFiles = {"status.json", "players.json", "npcs.json", "quests.json", "spawns.json", "dungeons.json"};
        
        for (String filename : localFiles) {
            File dataFile = pathResolver.getLocalFile(worldName, filename);
            if (!dataFile.exists()) {
                if (filename.equals("status.json")) {
                    plugin.getDataManager().createDefaultStatusFile(worldName);
                } else if (filename.equals("players.json")) {
                    plugin.getDataManager().createDefaultPlayersFile(worldName);
                }
                // Los demás archivos se crearán cuando sean necesarios
            }
        }
        
        plugin.getLogger().info("Archivos de datos creados para mundo RPG: " + worldName);
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
