package com.nightslayer.mmorpg.economy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un item en una tienda
 */
public class ShopItem {
    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double buyPrice;
    private final double sellPrice;
    private final int amount;
    private final boolean canBuy;
    private final boolean canSell;
    
    public ShopItem(String id, Material material, String displayName, 
                   double buyPrice, double sellPrice, int amount) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = new ArrayList<>();
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.amount = amount;
        this.canBuy = buyPrice > 0;
        this.canSell = sellPrice > 0;
    }
    
    public String getId() {
        return id;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public void addLore(String line) {
        lore.add(line);
    }
    
    public double getBuyPrice() {
        return buyPrice;
    }
    
    public double getSellPrice() {
        return sellPrice;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public boolean canBuy() {
        return canBuy;
    }
    
    public boolean canSell() {
        return canSell;
    }
    
    /**
     * Crea el ItemStack para mostrar en la tienda
     */
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§f" + displayName);
            
            List<String> itemLore = new ArrayList<>(lore);
            itemLore.add("");
            
            if (canBuy) {
                itemLore.add("§aComprar: §6" + buyPrice + " coins");
            }
            if (canSell) {
                itemLore.add("§cVender: §6" + sellPrice + " coins");
            }
            
            itemLore.add("");
            itemLore.add("§7Click izquierdo para comprar");
            if (canSell) {
                itemLore.add("§7Click derecho para vender");
            }
            
            meta.setLore(itemLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
