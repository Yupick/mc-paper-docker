package com.nightslayer.mmorpg.mobs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona mobs personalizados del plugin MMORPG
 */
public class MobManager {
    private final MMORPGPlugin plugin;
    private final Map<String, CustomMob> customMobs;
    private final Map<UUID, String> spawnedMobs; // UUID de entidad -> ID de custom mob
    private final File mobsFile;
    private final Gson gson;
    
    public MobManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.customMobs = new ConcurrentHashMap<>();
        this.spawnedMobs = new ConcurrentHashMap<>();
        this.mobsFile = new File(plugin.getDataFolder(), "data/mobs.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        loadMobs();
    }
    
    /**
     * Carga mobs desde el archivo JSON
     */
    public void loadMobs() {
        if (!mobsFile.exists()) {
            mobsFile.getParentFile().mkdirs();
            return;
        }
        
        try (Reader reader = new FileReader(mobsFile)) {
            Type type = new TypeToken<Map<String, MobData>>(){}.getType();
            Map<String, MobData> mobsData = gson.fromJson(reader, type);
            
            if (mobsData != null) {
                for (Map.Entry<String, MobData> entry : mobsData.entrySet()) {
                    MobData data = entry.getValue();
                    
                    List<CustomMob.MobDrop> drops = new ArrayList<>();
                    if (data.drops != null) {
                        for (DropData dropData : data.drops) {
                            drops.add(new CustomMob.MobDrop(
                                dropData.itemType,
                                dropData.minAmount,
                                dropData.maxAmount,
                                dropData.dropChance
                            ));
                        }
                    }
                    
                    World world = null;
                    Location location = null;
                    if (data.spawnLocation != null) {
                        world = plugin.getServer().getWorld(data.spawnLocation.world);
                        if (world != null) {
                            location = new Location(world, 
                                data.spawnLocation.x, 
                                data.spawnLocation.y, 
                                data.spawnLocation.z);
                        }
                    }
                    
                    CustomMob mob = new CustomMob(
                        entry.getKey(),
                        data.name,
                        EntityType.valueOf(data.entityType),
                        data.health,
                        data.damage,
                        data.defense,
                        data.level,
                        drops,
                        data.experienceReward,
                        data.isBoss,
                        location
                    );
                    
                    customMobs.put(entry.getKey(), mob);
                }
                
                plugin.getLogger().info("Loaded " + customMobs.size() + " custom mobs");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al cargar mobs: " + e.getMessage());
        }
    }
    
    /**
     * Guarda mobs al archivo JSON
     */
    public void saveMobs() {
        try (Writer writer = new FileWriter(mobsFile)) {
            Map<String, MobData> mobsData = new HashMap<>();
            
            for (Map.Entry<String, CustomMob> entry : customMobs.entrySet()) {
                CustomMob mob = entry.getValue();
                
                MobData data = new MobData();
                data.name = mob.getName();
                data.entityType = mob.getEntityType().name();
                data.health = mob.getHealth();
                data.damage = mob.getDamage();
                data.defense = mob.getDefense();
                data.level = mob.getLevel();
                data.experienceReward = mob.getExperienceReward();
                data.isBoss = mob.isBoss();
                
                if (mob.getSpawnLocation() != null) {
                    data.spawnLocation = new LocationData();
                    data.spawnLocation.world = mob.getSpawnLocation().getWorld().getName();
                    data.spawnLocation.x = mob.getSpawnLocation().getX();
                    data.spawnLocation.y = mob.getSpawnLocation().getY();
                    data.spawnLocation.z = mob.getSpawnLocation().getZ();
                }
                
                data.drops = new ArrayList<>();
                for (CustomMob.MobDrop drop : mob.getDrops()) {
                    DropData dropData = new DropData();
                    dropData.itemType = drop.getItemType();
                    dropData.minAmount = drop.getMinAmount();
                    dropData.maxAmount = drop.getMaxAmount();
                    dropData.dropChance = drop.getDropChance();
                    data.drops.add(dropData);
                }
                
                mobsData.put(entry.getKey(), data);
            }
            
            gson.toJson(mobsData, writer);
            plugin.getLogger().info("Saved " + customMobs.size() + " custom mobs");
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar mobs: " + e.getMessage());
        }
    }
    
    /**
     * Registra un nuevo mob personalizado
     */
    public void registerMob(CustomMob mob) {
        customMobs.put(mob.getId(), mob);
        saveMobs();
        plugin.getLogger().info("Registered custom mob: " + mob.getId());
    }
    
    /**
     * Elimina un mob personalizado
     */
    public void unregisterMob(String mobId) {
        customMobs.remove(mobId);
        saveMobs();
        plugin.getLogger().info("Unregistered custom mob: " + mobId);
    }
    
    /**
     * Spawnea un mob personalizado en el mundo
     */
    public Entity spawnCustomMob(String mobId, Location location) {
        CustomMob customMob = customMobs.get(mobId);
        if (customMob == null) {
            return null;
        }
        
        Entity entity = location.getWorld().spawnEntity(location, customMob.getEntityType());
        
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            
            // Configurar nombre
            livingEntity.setCustomName(customMob.getName());
            livingEntity.setCustomNameVisible(true);
            
            // Configurar health
            if (livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(customMob.getHealth());
                livingEntity.setHealth(customMob.getHealth());
            }
            
            // Configurar damage (si es un Mob)
            if (entity instanceof Mob && livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(customMob.getDamage());
            }
            
            // Configurar armor (defense)
            if (livingEntity.getAttribute(Attribute.GENERIC_ARMOR) != null) {
                livingEntity.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(customMob.getDefense());
            }
            
            // Marcar como custom mob
            livingEntity.setMetadata("mmorpg_custom_mob", new FixedMetadataValue(plugin, mobId));
            spawnedMobs.put(entity.getUniqueId(), mobId);
            
            plugin.getLogger().info("Spawned custom mob: " + mobId + " at " + location);
        }
        
        return entity;
    }
    
    /**
     * Obtiene un mob personalizado
     */
    public CustomMob getMob(String mobId) {
        return customMobs.get(mobId);
    }
    
    /**
     * Obtiene todos los mobs personalizados
     */
    public Map<String, CustomMob> getAllMobs() {
        return new HashMap<>(customMobs);
    }
    
    /**
     * Obtiene el ID de un mob por su entidad
     */
    public String getSpawnedMobId(Entity entity) {
        return spawnedMobs.get(entity.getUniqueId());
    }
    
    /**
     * Elimina un mob spawnado del tracking
     */
    public void removeSpawnedMob(Entity entity) {
        spawnedMobs.remove(entity.getUniqueId());
    }
    
    /**
     * Alias para removeSpawnedMob - mantiene compatibilidad
     */
    public void untrackMob(Entity entity) {
        removeSpawnedMob(entity);
    }
    
    // ======================= Clases internas para JSON =======================
    
    public static class MobData {
        public String name;
        public String entityType;
        public double health;
        public double damage;
        public double defense;
        public int level;
        public int experienceReward;
        public boolean isBoss;
        public LocationData spawnLocation;
        public List<DropData> drops;
    }
    
    public static class LocationData {
        public String world;
        public double x;
        public double y;
        public double z;
    }
    
    public static class DropData {
        public String itemType;
        public int minAmount;
        public int maxAmount;
        public double dropChance;
    }
}
