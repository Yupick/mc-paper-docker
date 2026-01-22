package com.nightslayer.mmorpg.mobs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.RPGPathResolver;
import com.nightslayer.mmorpg.database.DatabaseManager;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona mobs personalizados del plugin MMORPG
 * Lee/escribe en: config/data/universal.db tabla custom_mobs
 */
public class MobManager {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final DatabaseManager databaseManager;
    private final Map<String, CustomMob> customMobs;
    private final Map<UUID, String> spawnedMobs; // UUID de entidad -> ID de custom mob
    private final Gson gson;
    
    public MobManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.databaseManager = plugin.getDatabaseManager();
        this.customMobs = new ConcurrentHashMap<>();
        this.spawnedMobs = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        loadMobs();
    }
    
    /**
     * Carga mobs desde la base de datos SQLite (universal)
     */
    public void loadMobs() {
        if (databaseManager == null) {
            plugin.getLogger().warning("DatabaseManager no disponible, no se pueden cargar mobs");
            return;
        }
        
        try {
            ResultSet rs = databaseManager.executeQuery(
                "SELECT * FROM custom_mobs"
            );
            
            while (rs != null && rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String entityTypeStr = rs.getString("entity_type");
                double health = rs.getDouble("health");
                double damage = rs.getDouble("damage");
                double defense = rs.getDouble("defense");
                int level = rs.getInt("level");
                int experienceReward = rs.getInt("experience_reward");
                boolean isBoss = rs.getInt("is_boss") == 1;
                String spawnWorld = rs.getString("spawn_world");
                Double spawnX = (Double) rs.getObject("spawn_x");
                Double spawnY = (Double) rs.getObject("spawn_y");
                Double spawnZ = (Double) rs.getObject("spawn_z");
                
                // Validar EntityType
                EntityType entityType;
                try {
                    entityType = EntityType.valueOf(entityTypeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Mob '" + id + "' tiene entityType inválido: " + entityTypeStr);
                    continue;
                }
                
                // Construir location si existe
                Location location = null;
                if (spawnWorld != null && spawnX != null && spawnY != null && spawnZ != null) {
                    World world = plugin.getServer().getWorld(spawnWorld);
                    if (world != null) {
                        location = new Location(world, spawnX, spawnY, spawnZ);
                    }
                }
                
                // Cargar drops del mob
                List<CustomMob.MobDrop> drops = loadMobDrops(id);
                
                CustomMob mob = new CustomMob(
                    id,
                    name,
                    entityType,
                    health,
                    damage,
                    defense,
                    level,
                    drops,
                    experienceReward,
                    isBoss,
                    location
                );
                
                customMobs.put(id, mob);
            }
            
            if (rs != null) {
                rs.close();
            }
            
            plugin.getLogger().info("Cargados " + customMobs.size() + " custom mobs desde BD");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error al cargar mobs desde BD: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga los drops de un mob desde la tabla mob_drops
     */
    private List<CustomMob.MobDrop> loadMobDrops(String mobId) {
        List<CustomMob.MobDrop> drops = new ArrayList<>();
        
        try {
            ResultSet rs = databaseManager.executeQuery(
                "SELECT item_type, min_amount, max_amount, drop_chance FROM mob_drops WHERE mob_id = ?",
                mobId
            );
            
            while (rs != null && rs.next()) {
                drops.add(new CustomMob.MobDrop(
                    rs.getString("item_type"),
                    rs.getInt("min_amount"),
                    rs.getInt("max_amount"),
                    rs.getDouble("drop_chance")
                ));
            }
            
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error cargando drops para mob " + mobId + ": " + e.getMessage());
        }
        
        return drops;
    }
    
    /**
     * Guarda un mob en la base de datos SQLite
     */
    public void saveMob(CustomMob mob) {
        if (databaseManager == null) {
            plugin.getLogger().warning("DatabaseManager no disponible, no se puede guardar mob");
            return;
        }
        
        // Guardar mob principal
        Location loc = mob.getSpawnLocation();
            
            databaseManager.executeUpdate(
                """
                INSERT OR REPLACE INTO custom_mobs 
                (id, name, entity_type, health, damage, defense, level, experience_reward, is_boss, 
                 spawn_world, spawn_x, spawn_y, spawn_z, created_at) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                mob.getId(),
                mob.getName(),
                mob.getEntityType().name(),
                mob.getHealth(),
                mob.getDamage(),
                mob.getDefense(),
                mob.getLevel(),
                mob.getExperienceReward(),
                mob.isBoss() ? 1 : 0,
                loc != null ? loc.getWorld().getName() : null,
                loc != null ? loc.getX() : null,
                loc != null ? loc.getY() : null,
                loc != null ? loc.getZ() : null,
                System.currentTimeMillis()
            );
            
            // Eliminar drops anteriores
            databaseManager.executeUpdate(
                "DELETE FROM mob_drops WHERE mob_id = ?",
                mob.getId()
            );
            
            // Guardar drops
            for (CustomMob.MobDrop drop : mob.getDrops()) {
                databaseManager.executeUpdate(
                    "INSERT INTO mob_drops (mob_id, item_type, min_amount, max_amount, drop_chance) VALUES (?, ?, ?, ?, ?)",
                    mob.getId(),
                    drop.getItemType(),
                    drop.getMinAmount(),
                    drop.getMaxAmount(),
                    drop.getDropChance()
                );
            }
            
        plugin.getLogger().info("Guardado mob en BD: " + mob.getId());
    }
    
    /**
     * Elimina un mob de la base de datos
     */
    public void deleteMob(String mobId) {
        if (databaseManager == null) {
            return;
        }
        
        // Eliminar drops primero (foreign key)
        databaseManager.executeUpdate("DELETE FROM mob_drops WHERE mob_id = ?", mobId);
            
            // Eliminar mob
            databaseManager.executeUpdate("DELETE FROM custom_mobs WHERE id = ?", mobId);
            
        plugin.getLogger().info("Eliminado mob de BD: " + mobId);
    }
    
    /**
     * Registra un nuevo mob personalizado
     */
    public void registerMob(CustomMob mob) {
        customMobs.put(mob.getId(), mob);
        saveMob(mob);
        plugin.getLogger().info("Registrado custom mob: " + mob.getId());
    }
    
    /**
     * Elimina un mob personalizado
     */
    public void unregisterMob(String mobId) {
        customMobs.remove(mobId);
        deleteMob(mobId);
        plugin.getLogger().info("Eliminado custom mob: " + mobId);
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
}
