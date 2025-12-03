package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.classes.ClassType;
import com.nightslayer.mmorpg.classes.PlayerClass;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento llamado cuando un jugador sube de nivel
 */
public class PlayerLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final PlayerClass playerClass;
    private final int previousLevel;
    private final int newLevel;
    private final ClassType classType;
    
    public PlayerLevelUpEvent(Player player, PlayerClass playerClass, int previousLevel, int newLevel) {
        this.player = player;
        this.playerClass = playerClass;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.classType = playerClass.getClassType();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public PlayerClass getPlayerClass() {
        return playerClass;
    }
    
    public int getPreviousLevel() {
        return previousLevel;
    }
    
    public int getNewLevel() {
        return newLevel;
    }
    
    public int getLevelGain() {
        return newLevel - previousLevel;
    }
    
    public ClassType getClassType() {
        return classType;
    }
    
    public boolean isMultipleLevels() {
        return getLevelGain() > 1;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
