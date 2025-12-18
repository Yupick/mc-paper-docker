package com.nightslayer.mmorpg.listeners;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.spawns.SpawnManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Listener para gestionar eventos de spawns (muerte y pickup)
 */
public class SpawnListener implements Listener {
    private final MMORPGPlugin plugin;
    private final SpawnManager spawnManager;
    
    public SpawnListener(MMORPGPlugin plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Notificar al SpawnManager sobre la muerte
        spawnManager.handleEntityDeath(event.getEntity().getUniqueId());
    }
    
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        // Solo procesar si es un jugador quien recoge el item
        if (event.getEntity() instanceof Player) {
            // Notificar al SpawnManager sobre el pickup
            spawnManager.handleItemPickup(event.getItem().getUniqueId());
        }
    }
}
