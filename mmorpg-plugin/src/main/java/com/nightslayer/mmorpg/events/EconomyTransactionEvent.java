package com.nightslayer.mmorpg.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento llamado cuando ocurre una transacción económica
 */
public class EconomyTransactionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TransactionType type;
    private final double amount;
    private final String reason;
    private boolean cancelled = false;
    
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER,
        SHOP_BUY,
        SHOP_SELL,
        QUEST_REWARD
    }
    
    public EconomyTransactionEvent(Player player, TransactionType type, double amount, String reason) {
        this.player = player;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public String getReason() {
        return reason;
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
