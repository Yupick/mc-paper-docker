package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.npcs.CustomNPC;
import com.nightslayer.mmorpg.npcs.NPCType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento llamado cuando un jugador interactúa con un NPC
 */
public class NPCInteractEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final CustomNPC npc;
    private final InteractionType interactionType;
    private boolean cancelled = false;
    
    public enum InteractionType {
        RIGHT_CLICK,
        LEFT_CLICK,
        SHIFT_RIGHT_CLICK
    }
    
    public NPCInteractEvent(Player player, CustomNPC npc, InteractionType interactionType) {
        this.player = player;
        this.npc = npc;
        this.interactionType = interactionType;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public CustomNPC getNPC() {
        return npc;
    }
    
    public NPCType getNPCType() {
        return npc.getType();
    }
    
    public InteractionType getInteractionType() {
        return interactionType;
    }
    
    public boolean hasQuests() {
        return !npc.getAssociatedQuests().isEmpty();
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
