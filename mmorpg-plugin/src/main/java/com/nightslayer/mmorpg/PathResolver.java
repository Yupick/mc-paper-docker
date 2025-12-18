package com.nightslayer.mmorpg;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

/**
 * Resuelve rutas de archivos de datos RPG según clasificación (local, universal, exclusive-local)
 * 
 * Clasificación de Datos:
 * - UNIVERSAL: items, mobs_global, npcs_global, quests_global, enchantments_global, pets_global
 *   Ubicación: plugins/MMORPGPlugin/data/
 * 
 * - LOCAL: npcs, quests, mobs, pets, enchantments (busca primero local, luego universal)
 *   Ubicación: plugins/MMORPGPlugin/data/{level-name}/
 * 
 * - EXCLUSIVE-LOCAL: players, status, invasions, kills, respawn, squads
 *   Ubicación: plugins/MMORPGPlugin/data/{level-name}/ (solo local)
 */
public class PathResolver {
    
    private final MMORPGPlugin plugin;
    private final File pluginDataDir;
    private final Map<String, String> levelNameCache; // Cache de level-name por mundo
    
    // Conjuntos de clasificación
    private static final Set<String> UNIVERSAL_DATA = Set.of(
        "items", "mobs_global", "npcs_global", "quests_global", 
        "enchantments_global", "pets_global"
    );
    
    private static final Set<String> HYBRID_DATA = Set.of(
        "npcs", "quests", "mobs", "pets", "enchantments"
    );
    
    private static final Set<String> EXCLUSIVE_LOCAL_DATA = Set.of(
        "players", "status", "invasions", "kills", "respawn", "squads"
    );
    
    public PathResolver(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pluginDataDir = new File(plugin.getDataFolder(), "data");
        this.levelNameCache = new HashMap<>();
        ensureDataDirExists();
    }
    
    /**
     * Asegura que el directorio de datos existe
     */
    private void ensureDataDirExists() {
        if (!pluginDataDir.exists()) {
            pluginDataDir.mkdirs();
            plugin.getLogger().info("Directorio de datos creado: " + pluginDataDir.getAbsolutePath());
        }
    }
    
    /**
     * Obtiene el level-name del mundo desde server.properties
     * Usa caché para evitar lecturas repetidas
     */
    private String getLevelNameForWorld(String worldSlug) {
        // Buscar en caché primero
        if (levelNameCache.containsKey(worldSlug)) {
            return levelNameCache.get(worldSlug);
        }
        
        World world = plugin.getServer().getWorld(worldSlug);
        if (world == null) {
            levelNameCache.put(worldSlug, worldSlug);
            return worldSlug;
        }
        
        // Intentar leer server.properties del mundo
        File worldDir = world.getWorldFolder();
        File serverPropsFile = new File(worldDir, "server.properties");
        
        if (serverPropsFile.exists()) {
            try {
                FileConfiguration props = YamlConfiguration.loadConfiguration(serverPropsFile);
                String levelName = props.getString("level-name");
                if (levelName != null && !levelName.isEmpty()) {
                    // Limpiar ruta (p.ej. "worlds/world" -> "world")
                    levelName = new File(levelName).getName();
                    levelNameCache.put(worldSlug, levelName);
                    return levelName;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, 
                    "Error al leer server.properties para " + worldSlug, e);
            }
        }
        
        // Fallback al slug del mundo
        levelNameCache.put(worldSlug, worldSlug);
        return worldSlug;
    }
    
    /**
     * Resuelve la ruta de un archivo de datos según su tipo
     * 
     * @param worldSlug Slug del mundo (ej: 'mmorpg', 'survival')
     * @param dataType Tipo de dato (ej: 'npcs', 'quests', 'mobs', 'items')
     * @param scope Alcance del dato ('local', 'universal', 'exclusive-local')
     * @return Ruta absoluta al archivo JSON
     */
    public File resolvePath(String worldSlug, String dataType, String scope) {
        // Validar scope
        if (!scope.equals("local") && !scope.equals("universal") && !scope.equals("exclusive-local")) {
            plugin.getLogger().warning("Scope inválido: " + scope + ". Usando 'local'");
            scope = "local";
        }
        
        String filename = dataType.endsWith(".json") ? dataType : dataType + ".json";
        
        // UNIVERSAL: siempre en raíz
        if ("universal".equals(scope) || UNIVERSAL_DATA.contains(dataType)) {
            return new File(pluginDataDir, filename);
        }
        
        // EXCLUSIVE-LOCAL: solo en mundo
        if ("exclusive-local".equals(scope) || EXCLUSIVE_LOCAL_DATA.contains(dataType)) {
            String levelName = getLevelNameForWorld(worldSlug);
            return new File(new File(pluginDataDir, levelName), filename);
        }
        
        // LOCAL o HYBRID: preferir local si existe, sino universal
        if ("local".equals(scope) || HYBRID_DATA.contains(dataType)) {
            String levelName = getLevelNameForWorld(worldSlug);
            File localPath = new File(new File(pluginDataDir, levelName), filename);
            
            // Si el archivo existe localmente, devolverlo
            if (localPath.exists()) {
                return localPath;
            }
            
            // Si existe universalmente, devolverlo
            File universalPath = new File(pluginDataDir, filename);
            if (universalPath.exists()) {
                return universalPath;
            }
            
            // Si no existe ninguno, retornar la ruta local para crear
            return localPath;
        }
        
        // Default
        String levelName = getLevelNameForWorld(worldSlug);
        return new File(new File(pluginDataDir, levelName), filename);
    }
    
    /**
     * Resuelve múltiples rutas (local y universal) para datos híbridos
     * 
     * @param worldSlug Slug del mundo
     * @param dataType Tipo de dato
     * @return Map con 'local' y 'universal' rutas
     */
    public Map<String, File> resolvePathPair(String worldSlug, String dataType) {
        Map<String, File> paths = new HashMap<>();
        
        String filename = dataType.endsWith(".json") ? dataType : dataType + ".json";
        String levelName = getLevelNameForWorld(worldSlug);
        
        File localPath = new File(new File(pluginDataDir, levelName), filename);
        File universalPath = new File(pluginDataDir, filename);
        
        paths.put("local", localPath);
        paths.put("universal", universalPath);
        
        return paths;
    }
    
    /**
     * Obtiene la ruta del directorio de datos del mundo
     * 
     * @param worldSlug Slug del mundo
     * @return Directorio de datos del mundo
     */
    public File getWorldDataDir(String worldSlug) {
        String levelName = getLevelNameForWorld(worldSlug);
        File worldDataDir = new File(pluginDataDir, levelName);
        
        if (!worldDataDir.exists()) {
            worldDataDir.mkdirs();
        }
        
        return worldDataDir;
    }
    
    /**
     * Obtiene la ruta del directorio de datos universal
     * 
     * @return Directorio de datos universal
     */
    public File getUniversalDataDir() {
        return pluginDataDir;
    }
    
    /**
     * Verifica si un archivo de datos existe
     * 
     * @param worldSlug Slug del mundo
     * @param dataType Tipo de dato
     * @param scope Alcance del dato
     * @return true si el archivo existe
     */
    public boolean exists(String worldSlug, String dataType, String scope) {
        return resolvePath(worldSlug, dataType, scope).exists();
    }
    
    /**
     * Limpia la caché de level-names (útil después de recargar plugin)
     */
    public void clearCache() {
        levelNameCache.clear();
    }
    
    /**
     * Obtiene información de debug sobre rutas resueltas
     * 
     * @param worldSlug Slug del mundo
     * @return String con información de rutas
     */
    public String getDebugInfo(String worldSlug) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Resolución de Rutas para ").append(worldSlug).append(" ===\n");
        
        String levelName = getLevelNameForWorld(worldSlug);
        sb.append("Level-name: ").append(levelName).append("\n");
        sb.append("Data dir base: ").append(pluginDataDir.getAbsolutePath()).append("\n");
        
        sb.append("\nRutas de NPCS:\n");
        sb.append("  Local:     ").append(resolvePath(worldSlug, "npcs", "local").getAbsolutePath()).append("\n");
        sb.append("  Universal: ").append(resolvePath(worldSlug, "npcs", "universal").getAbsolutePath()).append("\n");
        
        sb.append("\nRutas de QUESTS:\n");
        sb.append("  Local:     ").append(resolvePath(worldSlug, "quests", "local").getAbsolutePath()).append("\n");
        sb.append("  Universal: ").append(resolvePath(worldSlug, "quests", "universal").getAbsolutePath()).append("\n");
        
        sb.append("\nRutas de KILLS (exclusive-local):\n");
        sb.append("  Path:      ").append(resolvePath(worldSlug, "kills", "exclusive-local").getAbsolutePath()).append("\n");
        
        return sb.toString();
    }
}
