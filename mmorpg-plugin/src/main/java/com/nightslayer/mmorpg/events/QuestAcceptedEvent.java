package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.quests.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento llamado cuando un jugador acepta una quest
 */
public class QuestAcceptedEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Quest quest;
    private boolean cancelled = false;
    
    public QuestAcceptedEvent(Player player, Quest quest) {
        this.player = player;
        this.quest = quest;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Quest getQuest() {
        return quest;
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
