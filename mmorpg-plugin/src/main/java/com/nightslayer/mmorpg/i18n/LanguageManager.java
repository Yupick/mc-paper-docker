package com.nightslayer.mmorpg.i18n;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sistema de internacionalización (i18n) para el plugin
 */
public class LanguageManager {
    private final Plugin plugin;
    private final File langDir;
    private final Map<String, YamlConfiguration> languages;
    private String defaultLanguage;
    private final Map<String, String> playerLanguages;
    
    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.langDir = new File(plugin.getDataFolder(), "lang");
        this.languages = new HashMap<>();
        this.playerLanguages = new HashMap<>();
        this.defaultLanguage = "es_ES";
        
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        loadLanguages();
    }
    
    /**
     * Carga todos los archivos de idioma
     */
    private void loadLanguages() {
        // Crear archivos de idioma por defecto si no existen
        saveDefaultLanguageFile("es_ES.yml");
        saveDefaultLanguageFile("en_US.yml");
        
        // Cargar todos los archivos .yml del directorio lang
        File[] files = langDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String locale = file.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                languages.put(locale, config);
                plugin.getLogger().info("Idioma cargado: " + locale);
            }
        }
        
        if (languages.isEmpty()) {
            plugin.getLogger().warning("No se cargaron archivos de idioma. Usando valores por defecto.");
        }
    }
    
    /**
     * Guarda un archivo de idioma por defecto desde resources
     */
    private void saveDefaultLanguageFile(String filename) {
        File file = new File(langDir, filename);
        if (!file.exists()) {
            InputStream stream = plugin.getResource("lang/" + filename);
            if (stream != null) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
                try {
                    config.save(file);
                    plugin.getLogger().info("Archivo de idioma creado: " + filename);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error creando archivo de idioma: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Obtiene un mensaje traducido para un jugador
     */
    public String getMessage(Player player, String key, Object... args) {
        String locale = getPlayerLanguage(player);
        return getMessage(locale, key, args);
    }
    
    /**
     * Obtiene un mensaje traducido para un locale específico
     */
    public String getMessage(String locale, String key, Object... args) {
        YamlConfiguration lang = languages.get(locale);
        
        // Si no existe el idioma, usar el predeterminado
        if (lang == null) {
            lang = languages.get(defaultLanguage);
        }
        
        // Si tampoco existe el predeterminado, devolver la key
        if (lang == null) {
            return key;
        }
        
        String message = lang.getString(key, key);
        
        // Reemplazar placeholders
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        
        // Reemplazar códigos de color
        return message.replace("&", "§");
    }
    
    /**
     * Obtiene el idioma de un jugador
     */
    public String getPlayerLanguage(Player player) {
        String uuid = player.getUniqueId().toString();
        return playerLanguages.getOrDefault(uuid, defaultLanguage);
    }
    
    /**
     * Establece el idioma de un jugador
     */
    public void setPlayerLanguage(Player player, String locale) {
        if (languages.containsKey(locale)) {
            playerLanguages.put(player.getUniqueId().toString(), locale);
        }
    }
    
    /**
     * Establece el idioma predeterminado
     */
    public void setDefaultLanguage(String locale) {
        if (languages.containsKey(locale)) {
            this.defaultLanguage = locale;
        }
    }
    
    /**
     * Obtiene todos los idiomas disponibles
     */
    public Map<String, String> getAvailableLanguages() {
        Map<String, String> langs = new HashMap<>();
        for (String locale : languages.keySet()) {
            YamlConfiguration config = languages.get(locale);
            String name = config.getString("language.name", locale);
            langs.put(locale, name);
        }
        return langs;
    }
    
    /**
     * Recarga todos los archivos de idioma
     */
    public void reload() {
        languages.clear();
        loadLanguages();
    }
}
