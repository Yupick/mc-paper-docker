package com.nightslayer.mmorpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Inicializa automáticamente archivos de datos faltantes
 * Copia archivos .example desde config/plugin/ o config/plugin-data/ si existen
 * Genera archivos por defecto si los ejemplos no están disponibles
 */
public class DataInitializer {
    
    private final MMORPGPlugin plugin;
    private final PathResolver pathResolver;
    private final Gson gson;
    
    public DataInitializer(MMORPGPlugin plugin, PathResolver pathResolver) {
        this.plugin = plugin;
        this.pathResolver = pathResolver;
        this.gson = plugin.getGson();
    }
    
    /**
     * Inicializa todos los datos requeridos para un mundo RPG
     * 
     * @param worldSlug Slug del mundo (usar "_universal_" para solo inicializar datos globales)
     */
    public void initializeWorldData(String worldSlug) {
        if ("_universal_".equals(worldSlug)) {
            plugin.getLogger().info("Inicializando datos RPG universales (globales)...");
            initializeUniversalData();
            return;
        }
        
        plugin.getLogger().info("Inicializando datos RPG para mundo: " + worldSlug);
        
        // Datos universal (solo si no existen)
        initializeUniversalData();
        
        // Datos locales del mundo
        initializeWorldDataFiles(worldSlug);
        
        plugin.getLogger().info("Datos RPG inicializados para: " + worldSlug);
    }
    
    /**
     * Inicializa datos universales (se crea una sola vez)
     */
    private void initializeUniversalData() {
        initializeDataFile("items", "universal");
        initializeDataFile("mobs", "universal");
        initializeDataFile("npcs", "universal");
        initializeDataFile("quests", "universal");
        initializeDataFile("enchantments", "universal");
        initializeDataFile("pets", "universal");
    }
    
    /**
     * Inicializa archivos de datos locales de un mundo
     * 
     * @param worldSlug Slug del mundo
     */
    private void initializeWorldDataFiles(String worldSlug) {
        // Datos híbridos (local)
        initializeDataFile("npcs", "local", worldSlug);
        initializeDataFile("quests", "local", worldSlug);
        initializeDataFile("mobs", "local", worldSlug);
        initializeDataFile("pets", "local", worldSlug);
        initializeDataFile("enchantments", "local", worldSlug);
        
        // Datos exclusive-local
        initializeDataFile("players", "exclusive-local", worldSlug);
        initializeDataFile("status", "exclusive-local", worldSlug);
        initializeDataFile("invasions", "exclusive-local", worldSlug);
        initializeDataFile("kills", "exclusive-local", worldSlug);
        initializeDataFile("respawn", "exclusive-local", worldSlug);
        initializeDataFile("squads", "exclusive-local", worldSlug);
    }
    
    /**
     * Inicializa un archivo de datos si no existe
     * Intenta copiar desde .example, sino genera por defecto
     * 
     * @param dataType Tipo de dato (npcs, quests, etc.)
     * @param scope Alcance (universal, local, exclusive-local)
     * @param worldSlug Slug del mundo (requerido para local/exclusive-local)
     */
    private void initializeDataFile(String dataType, String scope, String worldSlug) {
        File targetPath = pathResolver.resolvePath(worldSlug, dataType, scope);
        
        if (targetPath.exists()) {
            return; // Ya existe, no hacer nada
        }
        
        // Intentar copiar desde archivo .example
        if (copyFromExample(dataType, targetPath)) {
            plugin.getLogger().info("✓ Copiado " + dataType + " desde .example");
            return;
        }
        
        // Generar archivo por defecto
        if (generateDefaultFile(dataType, scope, targetPath)) {
            plugin.getLogger().info("✓ Generado archivo por defecto para " + dataType);
        }
    }
    
    /**
     * Inicializa un archivo de datos universal
     * 
     * @param dataType Tipo de dato
     * @param scope Alcance (debe ser "universal")
     */
    private void initializeDataFile(String dataType, String scope) {
        if (!"universal".equals(scope)) {
            return;
        }
        
        File targetPath = pathResolver.getUniversalDataDir();
        File dataFile = new File(targetPath, dataType + ".json");
        
        if (dataFile.exists()) {
            return; // Ya existe
        }
        
        // Intentar copiar desde .example
        if (copyFromExample(dataType, dataFile)) {
            return;
        }
        
        // Generar por defecto
        generateDefaultFile(dataType, scope, dataFile);
    }
    
    /**
     * Intenta copiar un archivo desde .example
     * Busca en: config/plugin-data/{dataType}.json.example
     * 
     * @param dataType Tipo de dato
     * @param targetPath Ruta destino
     * @return true si se copió exitosamente
     */
    private boolean copyFromExample(String dataType, File targetPath) {
        // Rutas donde buscar archivos .example
        File[] examplePaths = new File[] {
            new File(plugin.getDataFolder().getParentFile(), "config/plugin-data/" + dataType + ".json.example"),
            new File(plugin.getDataFolder().getParentFile(), "config/plugin/" + dataType + ".json.example"),
            new File("/home/mkd/contenedores/mc-paper/config/plugin-data/" + dataType + ".json.example"),
            new File("/home/mkd/contenedores/mc-paper/config/plugin/" + dataType + ".json.example")
        };
        
        for (File exampleFile : examplePaths) {
            if (exampleFile.exists()) {
                try {
                    targetPath.getParentFile().mkdirs();
                    Files.copy(exampleFile.toPath(), targetPath.toPath());
                    return true;
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, 
                        "Error al copiar desde " + exampleFile.getAbsolutePath(), e);
                }
            }
        }
        
        return false;
    }
    
    /**
     * Genera un archivo de datos por defecto
     * 
     * @param dataType Tipo de dato
     * @param scope Alcance
     * @param targetPath Ruta destino
     * @return true si se generó exitosamente
     */
    private boolean generateDefaultFile(String dataType, String scope, File targetPath) {
        try {
            targetPath.getParentFile().mkdirs();
            
            JsonObject defaultData = generateDefaultData(dataType);
            
            try (FileWriter writer = new FileWriter(targetPath)) {
                gson.toJson(defaultData, writer);
            }
            
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, 
                "Error al generar archivo por defecto para " + dataType, e);
            return false;
        }
    }
    
    /**
     * Genera objeto JSON por defecto según tipo de dato
     * 
     * @param dataType Tipo de dato
     * @return JsonObject con estructura por defecto
     */
    private JsonObject generateDefaultData(String dataType) {
        JsonObject obj = new JsonObject();
        
        switch (dataType) {
            case "npcs":
                obj.add("npcs", new JsonArray());
                break;
            
            case "quests":
                obj.add("quests", new JsonArray());
                break;
            
            case "mobs":
                // Mobs pueden ser dict
                obj.add("mobs", new JsonObject());
                break;
            
            case "items":
                obj.add("items", new JsonArray());
                obj.add("rarities", new JsonObject());
                break;
            
            case "pets":
                obj.add("pets", new JsonArray());
                break;
            
            case "enchantments":
                obj.add("enchantments", new JsonArray());
                break;
            
            case "players":
                obj.add("players", new JsonObject());
                break;
            
            case "status":
                obj.addProperty("world", "unknown");
                obj.addProperty("rpg_enabled", true);
                obj.addProperty("players_online", 0);
                obj.addProperty("classes_active", 0);
                obj.addProperty("quests_active", 0);
                obj.addProperty("npcs_spawned", 0);
                break;
            
            case "invasions":
                obj.add("invasions", new JsonArray());
                break;
            
            case "kills":
                obj.add("kills", new JsonArray());
                obj.add("playerStats", new JsonObject());
                break;
            
            case "respawn":
                obj.add("respawn_config", new JsonObject());
                break;
            
            case "squads":
                obj.add("squads", new JsonObject());
                break;
            
            default:
                obj.add("data", new JsonArray());
        }
        
        return obj;
    }
}
