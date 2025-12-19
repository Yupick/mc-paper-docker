package com.nightslayer.mmorpg.respawn;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.mobs.CustomMob;
import com.nightslayer.mmorpg.mobs.MobManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona zonas de respawn de mobs
 */
public class RespawnManager {
    private final MMORPGPlugin plugin;
    private final MobManager mobManager;
    private final Map<String, RespawnZone> respawnZones;
    private final File configFile;
    private final Gson gson;
    private BukkitRunnable respawnTask;
    private boolean enabled;
    
    public RespawnManager(MMORPGPlugin plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.respawnZones = new ConcurrentHashMap<>();
        this.configFile = new File(plugin.getDataFolder(), "respawn_config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.enabled = true;
        
        loadConfig();
        startRespawnTask();
    }
    
    /**
     * Carga configuración de respawns desde JSON
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.getLogger().warning("respawn_config.json no encontrado");
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            RespawnConfig config = gson.fromJson(reader, RespawnConfig.class);
            
            if (config.respawnZones != null) {
                for (Map.Entry<String, ZoneData> entry : config.respawnZones.entrySet()) {
                    ZoneData data = entry.getValue();
                    
                    List<Location> locations = new ArrayList<>();
                    if (data.spawnLocations != null) {
                        for (LocationData locData : data.spawnLocations) {
                            locations.add(new Location(null, locData.x, locData.y, locData.z));
                        }
                    }
                    
                    RespawnZone zone = new RespawnZone(
                        entry.getKey(),
                        data.name,
                        RespawnZone.ZoneType.valueOf(data.type.toUpperCase()),
                        data.world,
                        data.mobIds,
                        locations,
                        data.maxMobs,
                        data.respawnInterval
                    );
                    zone.setEnabled(data.enabled);
                    
                    respawnZones.put(entry.getKey(), zone);
                }
                
                if (config.globalSettings != null) {
                    enabled = config.globalSettings.respawnEnabled;
                }
                
                plugin.getLogger().info("Cargadas " + respawnZones.size() + " zonas de respawn");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error cargando respawn_config.json: " + e.getMessage());
        }
    }
    
    /**
     * Inicia la tarea de respawn
     */
    private void startRespawnTask() {
        respawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) return;
                
                for (RespawnZone zone : respawnZones.values()) {
                    if (!zone.isEnabled() || !zone.shouldRespawn()) {
                        continue;
                    }
                    
                    processZoneRespawn(zone);
                }
            }
        };
        
        // Ejecutar cada 1 segundo (20 ticks)
        respawnTask.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Procesa el respawn de una zona
     */
    private void processZoneRespawn(RespawnZone zone) {
        World world = plugin.getServer().getWorld(zone.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("Mundo no encontrado: " + zone.getWorldName());
            return;
        }
        
        // Contar mobs vivos de la zona
        updateZoneMobCount(zone, world);
        
        // Respawnear si es necesario
        int mobsToSpawn = zone.getMobsToSpawn();
        for (int i = 0; i < mobsToSpawn; i++) {
            String mobId = zone.getRandomMobId();
            Location spawnLoc = zone.getRandomSpawnLocation(world);
            
            if (mobId != null && spawnLoc != null) {
                CustomMob customMob = mobManager.getMob(mobId);
                if (customMob != null) {
                    mobManager.spawnCustomMob(mobId, spawnLoc);
                    zone.incrementMobCount();
                }
            }
        }
        
        zone.setLastRespawnTime(System.currentTimeMillis());
    }
    
    /**
     * Actualiza el contador de mobs vivos en la zona
     */
    private void updateZoneMobCount(RespawnZone zone, World world) {
        int aliveCount = 0;
        
        for (Entity entity : world.getEntities()) {
            if (entity.hasMetadata("mmorpg_custom_mob")) {
                String mobId = entity.getMetadata("mmorpg_custom_mob").get(0).asString();
                if (zone.getMobIds().contains(mobId)) {
                    aliveCount++;
                }
            }
        }
        
        zone.setCurrentMobCount(aliveCount);
    }
    
    // ======================= Métodos públicos =======================
    
    /**
     * Obtiene una zona de respawn
     */
    public RespawnZone getZone(String zoneId) {
        return respawnZones.get(zoneId);
    }
    
    /**
     * Obtiene todas las zonas
     */
    public Map<String, RespawnZone> getAllZones() {
        return new HashMap<>(respawnZones);
    }
    
    /**
     * Habilita/deshabilita una zona
     */
    public void setZoneEnabled(String zoneId, boolean enabled) {
        RespawnZone zone = respawnZones.get(zoneId);
        if (zone != null) {
            zone.setEnabled(enabled);
        }
    }
    
    /**
     * Habilita/deshabilita el respawn global
     */
    public void setGlobalRespawnEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Verifica si respawn está habilitado
     */
    public boolean isRespawnEnabled() {
        return enabled;
    }
    
    /**
     * Obtiene información de zona como JSON para API
     */
    public Map<String, Object> getZoneInfo(String zoneId) {
        RespawnZone zone = respawnZones.get(zoneId);
        if (zone == null) return null;
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("zoneId", zone.getZoneId());
        info.put("name", zone.getName());
        info.put("type", zone.getType().name());
        info.put("world", zone.getWorldName());
        info.put("currentMobs", zone.getCurrentMobCount());
        info.put("maxMobs", zone.getMaxMobs());
        info.put("respawnInterval", zone.getRespawnInterval());
        info.put("enabled", zone.isEnabled());
        info.put("mobIds", zone.getMobIds());
        info.put("nextRespawn", zone.getRespawnInterval() - 
            ((System.currentTimeMillis() - zone.getLastRespawnTime()) / 1000));
        
        return info;
    }
    
    /**
     * Detiene la tarea de respawn
     */
    public void shutdown() {
        if (respawnTask != null) {
            respawnTask.cancel();
        }
    }
    
    // ======================= Clases internas para JSON =======================
    
    public static class RespawnConfig {
        public Map<String, ZoneData> respawnZones;
        public GlobalSettings globalSettings;
    }
    
    public static class ZoneData {
        public String name;
        public String type;
        public String world;
        public List<String> mobIds;
        public List<LocationData> spawnLocations;
        public int maxMobs;
        public int respawnInterval;
        public boolean enabled;
    }
    
    public static class LocationData {
        public double x;
        public double y;
        public double z;
    }
    
    public static class GlobalSettings {
        public boolean respawnEnabled;
        public int checkInterval;
        public boolean logRespawns;
        public int maxConcurrentRespawns;
    }
}
