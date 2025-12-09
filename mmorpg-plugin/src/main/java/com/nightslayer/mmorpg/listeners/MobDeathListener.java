package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.items.ItemManager;
import com.nightslayer.mmorpg.bestiary.BestiaryManager;
import com.nightslayer.mmorpg.achievements.AchievementManager;
import com.nightslayer.mmorpg.invasions.InvasionManager;
import com.nightslayer.mmorpg.events.EventManager;
import com.nightslayer.mmorpg.mobs.CustomMob;
import com.nightslayer.mmorpg.mobs.MobManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Random;

/**
 * Listener para manejar muerte de mobs custom y drops de items RPG
 */
public class MobDeathListener implements Listener {
    private final MMORPGPlugin plugin;
    private final MobManager mobManager;
    private final ItemManager itemManager;
    private final BestiaryManager bestiaryManager;
    private final AchievementManager achievementManager;
    private final InvasionManager invasionManager;
    private final EventManager eventManager;
    private final Random random;
    
    public MobDeathListener(MMORPGPlugin plugin, MobManager mobManager, ItemManager itemManager, 
                           BestiaryManager bestiaryManager, AchievementManager achievementManager,
                           InvasionManager invasionManager, EventManager eventManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.itemManager = itemManager;
        this.bestiaryManager = bestiaryManager;
        this.achievementManager = achievementManager;
        this.invasionManager = invasionManager;
        this.eventManager = eventManager;
        this.random = new Random();
    }
    
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Verificar si es un mob custom
        if (!entity.hasMetadata("mmorpg_custom_mob")) {
            return;
        }
        
        List<MetadataValue> metadata = entity.getMetadata("mmorpg_custom_mob");
        if (metadata.isEmpty()) {
            return;
        }
        
        String mobId = metadata.get(0).asString();
        CustomMob customMob = mobManager.getMob(mobId);
        
        if (customMob == null) {
            return;
        }
        
        Player killer = entity.getKiller();
        
        // Registrar muerte en invasión si aplica
        if (invasionManager != null && killer != null) {
            invasionManager.handleInvasionMobDeath(entity, killer.getUniqueId());
        }
        
        // Registrar muerte en evento si aplica
        if (eventManager != null && killer != null) {
            eventManager.onEventMobKill(killer, mobId);
        }
        
        // Limpiar drops vanilla
        event.getDrops().clear();
        
        // Procesar drops del mob custom
        for (CustomMob.MobDrop drop : customMob.getDrops()) {
            if (random.nextDouble() <= drop.getDropChance()) {
                // Calcular cantidad aleatoria
                int amount = drop.getMinAmount();
                if (drop.getMaxAmount() > drop.getMinAmount()) {
                    amount = drop.getMinAmount() + random.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                }
                
                // Crear item RPG si existe en items.json
                ItemStack itemStack = itemManager.createItemStack(drop.getItemType());
                
                if (itemStack != null) {
                    itemStack.setAmount(amount);
                    event.getDrops().add(itemStack);
                } else {
                    // Fallback: drop vanilla si no se encuentra en items.json
                    plugin.getLogger().warning("Item RPG no encontrado: " + drop.getItemType());
                }
            }
        }
        
        // Dar XP al jugador
        if (killer != null) {
            event.setDroppedExp(customMob.getExperienceReward());
            
            // Registrar kill en sistema de tracking
            registerKill(killer, customMob);

            // Registrar kill en bestiario
            if (bestiaryManager != null) {
                bestiaryManager.recordMobKill(killer, mobId);
            }

            // Registrar progreso de logros
            if (achievementManager != null) {
                achievementManager.recordKill(killer, mobId);
            }
        }
        
        // Remover del tracking de mobs spawneados
        mobManager.removeSpawnedMob(entity);
    }
    
    /**
     * Registra el kill en el sistema de tracking (llamada HTTP al panel web)
     */
    private void registerKill(Player player, CustomMob mob) {
        // TODO: Implementar llamada HTTP POST a /api/rpg/kill/record
        // Por ahora solo log
        plugin.getLogger().info(player.getName() + " mató a " + mob.getName() + " (+" + mob.getExperienceReward() + " XP)");
    }
}
