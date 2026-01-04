package com.nightslayer.mmorpg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

/**
 * Sistema centralizado de gestión de configuraciones con soporte para:
 * - Carga desde templates en JAR
 * - Configuraciones universales (plugins/MMORPGPlugin/data/)
 * - Configuraciones por mundo (worlds/{world}/data/) - ADITIVAS
 * - Jerarquía: templates → config/ → plugins/data/ → worlds/{world}/data/
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private final Gson gson;
    
    // Rutas base
    private final Path serverRoot;
    private final Path configDir;       // /server/config/
    private final Path pluginDataDir;   // /server/plugins/MMORPGPlugin/data/
    private final Path worldsDir;       // /server/worlds/
    
    // Templates disponibles en JAR
    private static final String[] TEMPLATES = {
        "events_template.json",
        "mobs_template.json",
        "pets_template.json",
        "items_template.json",
        "npcs_template.json",
        "dungeons_template.json",
        "invasions_template.json",
        "bestiary_template.json",
        "ranks_template.json",
        "achievements_template.json"
    };
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Detectar rutas - en Docker el servidor está en /server
        this.serverRoot = Paths.get("/server");
        this.configDir = serverRoot.resolve("config");
        this.pluginDataDir = serverRoot.resolve("plugins/MMORPGPlugin/data");
        this.worldsDir = serverRoot.resolve("worlds");
        
        ensureDirectoriesExist();
    }
    
    /**
     * Crea las estructuras de directorios necesarias
     */
    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(configDir);
            Files.createDirectories(pluginDataDir);
            Files.createDirectories(worldsDir);
            
            plugin.getLogger().info("Directorios de configuración verificados");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando directorios de configuración", e);
        }
    }
    
    /**
     * Inicializa todas las configuraciones copiando templates del JAR
     * Solo copia si el archivo no existe
     */
    public void initializeConfigs() {
        plugin.getLogger().info("Inicializando sistema de configuraciones...");
        
        for (String template : TEMPLATES) {
            String configName = template.replace("_template", "");
            
            // 1. Copiar a config/ si no existe (editable por usuario)
            Path configFile = configDir.resolve(configName);
            if (!Files.exists(configFile)) {
                copyTemplateFromJar(template, configFile);
            } else {
                plugin.getLogger().info("Config ya existe: " + configName);
            }
            
            // 2. Copiar a plugins/data/ como configuración universal
            Path dataFile = pluginDataDir.resolve(configName);
            copyFromConfigToData(configFile, dataFile);
        }
        
        plugin.getLogger().info("Sistema de configuraciones inicializado (" + TEMPLATES.length + " archivos)");
    }
    
    /**
     * Copia un template desde el JAR a un archivo destino
     */
    private void copyTemplateFromJar(String templateName, Path destination) {
        try (InputStream in = plugin.getResource("templates/" + templateName)) {
            if (in == null) {
                plugin.getLogger().warning("Template no encontrado en JAR: " + templateName);
                return;
            }
            
            Files.createDirectories(destination.getParent());
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().info("Template copiado: " + templateName + " → " + destination.getFileName());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error copiando template: " + templateName, e);
        }
    }
    
    /**
     * Copia desde config/ a plugins/data/
     */
    private void copyFromConfigToData(Path source, Path destination) {
        try {
            if (Files.exists(source)) {
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error copiando config a data: " + source, e);
        }
    }
    
    /**
     * Carga una configuración con soporte para herencia aditiva por mundo
     * 
     * @param configName Nombre del archivo (ej: "events_config.json")
     * @param world Mundo específico (null para universal)
     * @return JsonObject con la configuración combinada
     */
    public JsonObject loadConfigWithAdditive(String configName, World world) {
        JsonObject baseConfig = loadUniversalConfig(configName);
        
        if (world == null) {
            return baseConfig; // Solo configuración universal
        }
        
        // Cargar configuración del mundo (aditiva)
        Path worldConfigPath = worldsDir.resolve(world.getName()).resolve("data").resolve(configName);
        if (!Files.exists(worldConfigPath)) {
            return baseConfig; // No hay config específica del mundo
        }
        
        try {
            String json = Files.readString(worldConfigPath, StandardCharsets.UTF_8);
            JsonObject worldConfig = JsonParser.parseString(json).getAsJsonObject();
            
            // Combinar: base + world (aditivo)
            return mergeConfigs(baseConfig, worldConfig);
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error leyendo config de mundo: " + worldConfigPath, e);
            return baseConfig;
        }
    }
    
    /**
     * Carga solo la configuración universal desde plugins/data/
     */
    private JsonObject loadUniversalConfig(String configName) {
        Path dataFile = pluginDataDir.resolve(configName);
        
        if (!Files.exists(dataFile)) {
            plugin.getLogger().warning("Configuración no encontrada: " + configName);
            return new JsonObject();
        }
        
        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error leyendo configuración: " + configName, e);
            return new JsonObject();
        }
    }
    
    /**
     * Combina dos configuraciones JSON de forma aditiva
     * Las arrays se concatenan, los objetos se fusionan
     */
    private JsonObject mergeConfigs(JsonObject base, JsonObject additional) {
        JsonObject merged = base.deepCopy();
        
        for (String key : additional.keySet()) {
            JsonElement additionalValue = additional.get(key);
            
            if (!merged.has(key)) {
                // Clave nueva, agregar directamente
                merged.add(key, additionalValue);
            } else {
                JsonElement baseValue = merged.get(key);
                
                // Si ambos son arrays, concatenar
                if (baseValue.isJsonArray() && additionalValue.isJsonArray()) {
                    JsonArray mergedArray = new JsonArray();
                    baseValue.getAsJsonArray().forEach(mergedArray::add);
                    additionalValue.getAsJsonArray().forEach(mergedArray::add);
                    merged.add(key, mergedArray);
                }
                // Si ambos son objetos, fusionar recursivamente
                else if (baseValue.isJsonObject() && additionalValue.isJsonObject()) {
                    merged.add(key, mergeConfigs(baseValue.getAsJsonObject(), additionalValue.getAsJsonObject()));
                }
                // En otros casos, el valor del mundo sobrescribe
                else {
                    merged.add(key, additionalValue);
                }
            }
        }
        
        return merged;
    }
    
    /**
     * Método de conveniencia para cargar un array de configuración
     */
    public JsonArray loadConfigArray(String configName, String arrayKey, World world) {
        JsonObject config = loadConfigWithAdditive(configName, world);
        if (config.has(arrayKey) && config.get(arrayKey).isJsonArray()) {
            return config.getAsJsonArray(arrayKey);
        }
        return new JsonArray();
    }
    
    /**
     * Resuelve una entidad específica para un mundo (mobs, items, npcs, etc.)
     * Busca primero en config de mundo, luego en universal
     * 
     * @param configName Archivo de configuración
     * @param arrayKey Clave del array (ej: "mobs", "items")
     * @param idKey Campo identificador (ej: "id", "name")
     * @param idValue Valor del ID buscado
     * @param world Mundo donde buscar
     * @return JsonObject de la entidad o null si no existe
     */
    public JsonObject resolveForWorld(String configName, String arrayKey, String idKey, String idValue, World world) {
        JsonArray items = loadConfigArray(configName, arrayKey, world);
        
        for (JsonElement element : items) {
            if (!element.isJsonObject()) continue;
            
            JsonObject item = element.getAsJsonObject();
            if (item.has(idKey) && item.get(idKey).getAsString().equals(idValue)) {
                return item;
            }
        }
        
        return null; // No encontrado
    }
    
    /**
     * Guarda una configuración (solo en config/ y plugins/data/, NO en worlds/)
     */
    public void saveConfig(String configName, JsonObject config) {
        try {
            // Guardar en config/ (editable por usuario)
            Path configFile = configDir.resolve(configName);
            Files.writeString(configFile, gson.toJson(config), StandardCharsets.UTF_8);
            
            // Sincronizar a plugins/data/
            Path dataFile = pluginDataDir.resolve(configName);
            Files.writeString(dataFile, gson.toJson(config), StandardCharsets.UTF_8);
            
            plugin.getLogger().info("Configuración guardada: " + configName);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando configuración: " + configName, e);
        }
    }
    
    /**
     * Obtiene la ruta al directorio config/ (para lectura por otros managers)
     */
    public Path getConfigDir() {
        return configDir;
    }
    
    /**
     * Obtiene la ruta al directorio plugins/data/ (para lectura por otros managers)
     */
    public Path getPluginDataDir() {
        return pluginDataDir;
    }
    
    /**
     * Obtiene la ruta al directorio de un mundo específico
     */
    public Path getWorldDataDir(World world) {
        Path worldData = worldsDir.resolve(world.getName()).resolve("data");
        try {
            Files.createDirectories(worldData);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error creando directorio de mundo: " + world.getName(), e);
        }
        return worldData;
    }
}
