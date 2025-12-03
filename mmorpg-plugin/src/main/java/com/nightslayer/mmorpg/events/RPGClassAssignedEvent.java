package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.classes.ClassType;
import com.nightslayer.mmorpg.classes.PlayerClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento llamado cuando un jugador elige una clase RPG
 */
public class RPGClassAssignedEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ClassType previousClass;
    private final ClassType newClass;
    private final PlayerClass playerClass;
    private boolean cancelled = false;
    
    public RPGClassAssignedEvent(Player player, ClassType previousClass, ClassType newClass, PlayerClass playerClass) {
        this.player = player;
        this.previousClass = previousClass;
        this.newClass = newClass;
        this.playerClass = playerClass;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ClassType getPreviousClass() {
        return previousClass;
    }
    
    public ClassType getNewClass() {
        return newClass;
    }
    
    public PlayerClass getPlayerClass() {
        return playerClass;
    }
    
    public boolean isFirstClass() {
        return previousClass == ClassType.NONE;
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
