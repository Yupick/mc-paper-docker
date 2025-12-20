package com.nightslayer.mmorpg;

import org.bukkit.World;
import java.io.File;

/**
 * Resuelve rutas para archivos RPG según su scope (local o universal)
 * 
 * ESTRUCTURA DE ARCHIVOS RPG:
 * ---------------------------
 * Universales (compartidos por todos los mundos):
 *     plugins/MMORPGPlugin/data/
 *         ├── items.json
 *         └── mobs.json
 * 
 * Locales (específicos por mundo):
 *     worlds/{world_name}/data/
 *         ├── npcs.json
 *         ├── quests.json
 *         ├── spawns.json
 *         ├── dungeons.json
 *         ├── players.json
 *         └── status.json
 */
public class RPGPathResolver {
    
    private final MMORPGPlugin plugin;
    private final File universalDataDir;
    private final String worldsBasePath;
    
    public RPGPathResolver(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.universalDataDir = new File(plugin.getDataFolder(), "data");
        this.worldsBasePath = plugin.getConfig().getString("worlds.base-path", "/server/worlds");
        
        // Crear directorio de datos universales si no existe
        if (!universalDataDir.exists()) {
            universalDataDir.mkdirs();
        }
    }
    
    /**
     * Obtiene el directorio de datos locales del mundo ACTIVO
     * IMPORTANTE: Siempre usa 'active' independientemente del worldName
     * porque worlds/active es un symlink al mundo actualmente activo
     * @param worldName Nombre del mundo (ignorado, siempre usa 'active')
     * @return File apuntando a worlds/active/data/
     */
    public File getWorldDataDir(String worldName) {
        return new File(worldsBasePath + "/active/data");
    }
    
    /**
     * Obtiene el directorio de datos locales de un mundo desde el objeto World
     * @param world El mundo de Bukkit
     * @return File apuntando a worlds/{worldName}/data/
     */
    public File getWorldDataDir(World world) {
        return getWorldDataDir(world.getName());
    }
    
    /**
     * Obtiene el directorio de datos universales
     * @return File apuntando a plugins/MMORPGPlugin/data/
     */
    public File getUniversalDataDir() {
        return universalDataDir;
    }
    
    /**
     * Obtiene la ruta de un archivo local del mundo
     * @param worldName Nombre del mundo
     * @param filename Nombre del archivo (ej: "npcs.json")
     * @return File apuntando a worlds/{worldName}/data/{filename}
     */
    public File getLocalFile(String worldName, String filename) {
        File worldDataDir = getWorldDataDir(worldName);
        return new File(worldDataDir, filename);
    }
    
    /**
     * Obtiene la ruta de un archivo universal
     * @param filename Nombre del archivo (ej: "items.json")
     * @return File apuntando a plugins/MMORPGPlugin/data/{filename}
     */
    public File getUniversalFile(String filename) {
        return new File(universalDataDir, filename);
    }
    
    /**
     * Verifica si un archivo es universal o local según su tipo
     * @param filename Nombre del archivo
     * @return true si es universal, false si es local
     */
    public boolean isUniversalFile(String filename) {
        String name = filename.toLowerCase().replace(".json", "");
        return name.equals("items") || name.equals("mobs");
    }
    
    /**
     * Obtiene la ruta correcta de un archivo según su scope automático
     * @param worldName Nombre del mundo
     * @param filename Nombre del archivo
     * @return File apuntando a la ruta correcta (local o universal)
     */
    public File getFile(String worldName, String filename) {
        if (isUniversalFile(filename)) {
            return getUniversalFile(filename);
        } else {
            return getLocalFile(worldName, filename);
        }
    }
    
    /**
     * Crea el directorio de datos locales de un mundo si no existe
     * @param worldName Nombre del mundo
     * @return true si se creó o ya existe, false si hubo error
     */
    public boolean ensureWorldDataDirExists(String worldName) {
        File worldDataDir = getWorldDataDir(worldName);
        if (!worldDataDir.exists()) {
            return worldDataDir.mkdirs();
        }
        return true;
    }
    
    /**
     * Crea el directorio de datos universales si no existe
     * @return true si se creó o ya existe, false si hubo error
     */
    public boolean ensureUniversalDataDirExists() {
        if (!universalDataDir.exists()) {
            return universalDataDir.mkdirs();
        }
        return true;
    }
}
