package com.nightslayer.mmorpg.spawns;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.RPGPathResolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona puntos de spawn individuales de items y entidades con respawn automático
 * Lee desde: worlds/{worldName}/data/spawns.json
 */
public class SpawnManager {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final Map<String, SpawnPoint> spawnPoints;
    private final Map<UUID, SpawnPoint> activeEntities;
    private final Gson gson;
    private BukkitRunnable respawnTask;
    
    public SpawnManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.spawnPoints = new ConcurrentHashMap<>();
        this.activeEntities = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    /**
     * Carga los spawns de un mundo específico desde worlds/{worldName}/data/spawns.json
     */
    public void loadWorldSpawns(String worldName) {
        File spawnsFile = pathResolver.getLocalFile(worldName, "spawns.json");
        
        if (!spawnsFile.exists()) {
            plugin.getLogger().info("No hay spawns configurados para el mundo: " + worldName);
            return;
        }
        
        try (FileReader reader = new FileReader(spawnsFile)) {
            JsonObject data = gson.fromJson(reader, JsonObject.class);
            JsonArray spawnsArray = data.getAsJsonArray("spawns");
            
            if (spawnsArray != null) {
                int count = 0;
                for (JsonElement element : spawnsArray) {
                    JsonObject spawnObj = element.getAsJsonObject();
                    
                    if (!spawnObj.get("enabled").getAsBoolean()) {
                        continue;
                    }
                    
                    String id = spawnObj.get("id").getAsString();
                    String type = spawnObj.get("type").getAsString();
                    double x = spawnObj.get("x").getAsDouble();
                    double y = spawnObj.get("y").getAsDouble();
                    double z = spawnObj.get("z").getAsDouble();
                    
                    World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Mundo no encontrado para spawn: " + worldName);
                        continue;
                    }
                    
                    Location location = new Location(world, x, y, z);
                    
                    SpawnPoint spawn = new SpawnPoint(
                        id,
                        type,
                        location,
                        spawnObj.get("respawn_enabled").getAsBoolean(),
                        spawnObj.get("respawn_time_seconds").getAsInt(),
                        spawnObj.get("respawn_on_death").getAsBoolean(),
                        spawnObj.get("respawn_on_use").getAsBoolean()
                    );
                    
                    // Configurar item o entidad
                    if ("item".equals(type)) {
                        spawn.setItemType(spawnObj.get("item").getAsString());
                    } else if ("mob".equals(type) || "npc".equals(type)) {
                        spawn.setEntityType(spawnObj.get("entity_type").getAsString());
                    }
                    
                    spawnPoints.put(id, spawn);
                    
                    // Spawn inicial
                    spawnEntity(spawn);
                    count++;
                }
                
                plugin.getLogger().info("Cargados " + count + " spawns para el mundo: " + worldName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al cargar spawns para " + worldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Inicia la tarea de respawn automático
     */
    public void startRespawnTask() {
        if (respawnTask != null) {
            respawnTask.cancel();
        }
        
        respawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                
                for (SpawnPoint spawn : spawnPoints.values()) {
                    if (!spawn.isRespawnEnabled() || spawn.isSpawned()) {
                        continue;
                    }
                    
                    if (spawn.getLastDespawnTime() > 0) {
                        long timeSinceDespawn = currentTime - spawn.getLastDespawnTime();
                        long respawnDelay = spawn.getRespawnTimeSeconds() * 1000L;
                        
                        if (timeSinceDespawn >= respawnDelay) {
                            spawnEntity(spawn);
                        }
                    }
                }
            }
        };
        
        // Ejecutar cada segundo
        respawnTask.runTaskTimer(plugin, 20L, 20L);
    }
    
    /**
     * Spawna una entidad o item
     */
    private void spawnEntity(SpawnPoint spawn) {
        Location loc = spawn.getLocation();
        
        if ("item".equals(spawn.getType())) {
            try {
                Material material = Material.valueOf(spawn.getItemType().toUpperCase());
                ItemStack itemStack = new ItemStack(material, 1);
                Item item = loc.getWorld().dropItem(loc, itemStack);
                item.setCustomName("§e" + spawn.getId());
                item.setCustomNameVisible(true);
                item.setPickupDelay(20); // 1 segundo
                
                activeEntities.put(item.getUniqueId(), spawn);
                spawn.setSpawned(true);
                spawn.setEntityUUID(item.getUniqueId());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material inválido para spawn " + spawn.getId() + ": " + spawn.getItemType());
            }
        } else if ("mob".equals(spawn.getType()) || "npc".equals(spawn.getType())) {
            try {
                EntityType entityType = EntityType.valueOf(spawn.getEntityType().toUpperCase());
                LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, entityType);
                entity.setCustomName("§c" + spawn.getId());
                entity.setCustomNameVisible(true);
                entity.setRemoveWhenFarAway(false);
                
                activeEntities.put(entity.getUniqueId(), spawn);
                spawn.setSpawned(true);
                spawn.setEntityUUID(entity.getUniqueId());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("EntityType inválido para spawn " + spawn.getId() + ": " + spawn.getEntityType());
            }
        }
    }
    
    /**
     * Maneja la muerte de una entidad spawneada
     */
    public void handleEntityDeath(UUID entityUUID) {
        SpawnPoint spawn = activeEntities.get(entityUUID);
        
        if (spawn != null && spawn.isRespawnOnDeath()) {
            spawn.setSpawned(false);
            spawn.setLastDespawnTime(System.currentTimeMillis());
            spawn.setEntityUUID(null);
            activeEntities.remove(entityUUID);
        }
    }
    
    /**
     * Maneja el uso/pickup de un item spawneado
     */
    public void handleItemPickup(UUID entityUUID) {
        SpawnPoint spawn = activeEntities.get(entityUUID);
        
        if (spawn != null && spawn.isRespawnOnUse()) {
            spawn.setSpawned(false);
            spawn.setLastDespawnTime(System.currentTimeMillis());
            spawn.setEntityUUID(null);
            activeEntities.remove(entityUUID);
        }
    }
    
    /**
     * Detiene el manager
     */
    public void shutdown() {
        if (respawnTask != null) {
            respawnTask.cancel();
        }
        
        // Limpiar entidades spawneadas
        for (SpawnPoint spawn : spawnPoints.values()) {
            if (spawn.getEntityUUID() != null) {
                UUID uuid = spawn.getEntityUUID();
                for (World world : plugin.getServer().getWorlds()) {
                    world.getEntities().stream()
                        .filter(e -> e.getUniqueId().equals(uuid))
                        .forEach(org.bukkit.entity.Entity::remove);
                }
            }
        }
        
        spawnPoints.clear();
        activeEntities.clear();
    }
    
    public Map<String, SpawnPoint> getSpawnPoints() {
        return new HashMap<>(spawnPoints);
    }
}
