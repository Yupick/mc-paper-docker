package com.nightslayer.mmorpg.events;

import com.nightslayer.mmorpg.quests.Quest;
import com.nightslayer.mmorpg.quests.QuestReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

/**
 * Evento llamado cuando un jugador completa una quest
 */
public class QuestCompletedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Quest quest;
    private final List<QuestReward> rewards;
    private final long completionTime;
    
    public QuestCompletedEvent(Player player, Quest quest, List<QuestReward> rewards) {
        this.player = player;
        this.quest = quest;
        this.rewards = rewards;
        this.completionTime = System.currentTimeMillis();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Quest getQuest() {
        return quest;
    }
    
    public List<QuestReward> getRewards() {
        return rewards;
    }
    
    public long getCompletionTime() {
        return completionTime;
    }
    
    public boolean isRepeatable() {
        return quest.isRepeatable();
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
