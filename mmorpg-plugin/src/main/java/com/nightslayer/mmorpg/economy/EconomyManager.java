package com.nightslayer.mmorpg.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gestor del sistema de economía RPG con integración Vault
 */
public class EconomyManager {
    private final Plugin plugin;
    private Economy economy;
    private boolean vaultEnabled;
    private final Map<UUID, Double> rpgCoins; // Sistema de monedas RPG interno
    
    public EconomyManager(Plugin plugin) {
        this.plugin = plugin;
        this.rpgCoins = new HashMap<>();
        this.vaultEnabled = setupEconomy();
        
        if (!vaultEnabled) {
            plugin.getLogger().warning("Vault no encontrado. Usando sistema de economía interno.");
        } else {
            plugin.getLogger().info("Sistema de economía Vault activado correctamente.");
        }
    }
    
    /**
     * Configura la integración con Vault
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Verifica si Vault está habilitado
     */
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
    
    /**
     * Obtiene el balance de un jugador
     */
    public double getBalance(Player player) {
        if (vaultEnabled) {
            return economy.getBalance(player);
        }
        return rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
    }
    
    /**
     * Obtiene el balance de RPG Coins (moneda interna)
     */
    public double getRPGCoins(Player player) {
        return rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
    }
    
    /**
     * Verifica si el jugador tiene suficiente dinero
     */
    public boolean hasEnough(Player player, double amount) {
        if (vaultEnabled) {
            return economy.has(player, amount);
        }
        return getBalance(player) >= amount;
    }
    
    /**
     * Añade dinero a un jugador
     */
    public boolean deposit(Player player, double amount) {
        if (amount < 0) {
            return false;
        }
        
        if (vaultEnabled) {
            return economy.depositPlayer(player, amount).transactionSuccess();
        }
        
        double current = rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
        rpgCoins.put(player.getUniqueId(), current + amount);
        return true;
    }
    
    /**
     * Retira dinero de un jugador
     */
    public boolean withdraw(Player player, double amount) {
        if (amount < 0 || !hasEnough(player, amount)) {
            return false;
        }
        
        if (vaultEnabled) {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        }
        
        double current = rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
        rpgCoins.put(player.getUniqueId(), current - amount);
        return true;
    }
    
    /**
     * Transfiere dinero entre jugadores
     */
    public boolean transfer(Player from, Player to, double amount) {
        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                return true;
            }
            // Revertir si falla el depósito
            deposit(from, amount);
        }
        return false;
    }
    
    /**
     * Añade RPG Coins (moneda interna del plugin)
     */
    public void addRPGCoins(Player player, double amount) {
        if (amount < 0) return;
        
        double current = rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
        rpgCoins.put(player.getUniqueId(), current + amount);
    }
    
    /**
     * Retira RPG Coins
     */
    public boolean withdrawRPGCoins(Player player, double amount) {
        if (amount < 0) return false;
        
        double current = rpgCoins.getOrDefault(player.getUniqueId(), 0.0);
        if (current < amount) {
            return false;
        }
        
        rpgCoins.put(player.getUniqueId(), current - amount);
        return true;
    }
    
    /**
     * Formatea la cantidad de dinero con el símbolo de moneda
     */
    public String format(double amount) {
        if (vaultEnabled) {
            return economy.format(amount);
        }
        return String.format("%.2f RPG Coins", amount);
    }
    
    /**
     * Formatea RPG Coins
     */
    public String formatRPGCoins(double amount) {
        return String.format("§6%.0f §eRPG Coins", amount);
    }
    
    /**
     * Obtiene el nombre de la moneda
     */
    public String getCurrencyName(boolean plural) {
        if (vaultEnabled) {
            return plural ? economy.currencyNamePlural() : economy.currencyNameSingular();
        }
        return plural ? "RPG Coins" : "RPG Coin";
    }
    
    /**
     * Crea una cuenta para un jugador si no existe
     */
    public void createAccount(Player player) {
        if (vaultEnabled && !economy.hasAccount(player)) {
            economy.createPlayerAccount(player);
        }
        rpgCoins.putIfAbsent(player.getUniqueId(), 0.0);
    }
    
    /**
     * Obtiene todos los datos de RPG Coins
     */
    public Map<UUID, Double> getAllRPGCoins() {
        return new HashMap<>(rpgCoins);
    }
    
    /**
     * Establece el balance de RPG Coins de un jugador
     */
    public void setRPGCoins(UUID playerId, double amount) {
        if (amount < 0) return;
        rpgCoins.put(playerId, amount);
    }
    
    /**
     * Limpia los datos de un jugador
     */
    public void clearPlayerData(UUID playerId) {
        rpgCoins.remove(playerId);
    }
}
