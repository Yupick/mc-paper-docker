package com.nightslayer.mmorpg.economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Gestor de tiendas de NPCs
 */
public class ShopManager {
    private final Plugin plugin;
    private final EconomyManager economyManager;
    private final Map<String, List<ShopItem>> shops; // shopId -> items
    private final Map<UUID, String> openShops; // playerId -> shopId
    
    public ShopManager(Plugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.shops = new HashMap<>();
        this.openShops = new HashMap<>();
        
        createDefaultShops();
    }
    
    /**
     * Crea las tiendas por defecto
     */
    private void createDefaultShops() {
        // Tienda General
        List<ShopItem> generalShop = new ArrayList<>();
        generalShop.add(createShopItem("bread", Material.BREAD, "Pan", 5, 2, 1));
        generalShop.add(createShopItem("apple", Material.APPLE, "Manzana", 3, 1, 1));
        generalShop.add(createShopItem("cooked_beef", Material.COOKED_BEEF, "Carne Cocinada", 8, 4, 1));
        generalShop.add(createShopItem("torch", Material.TORCH, "Antorcha", 1, 0.5, 16));
        generalShop.add(createShopItem("arrow", Material.ARROW, "Flecha", 2, 1, 16));
        shops.put("general", generalShop);
        
        // Tienda de Armas
        List<ShopItem> weaponShop = new ArrayList<>();
        weaponShop.add(createShopItem("iron_sword", Material.IRON_SWORD, "Espada de Hierro", 50, 25, 1));
        weaponShop.add(createShopItem("iron_axe", Material.IRON_AXE, "Hacha de Hierro", 45, 20, 1));
        weaponShop.add(createShopItem("bow", Material.BOW, "Arco", 60, 30, 1));
        weaponShop.add(createShopItem("diamond_sword", Material.DIAMOND_SWORD, "Espada de Diamante", 200, 100, 1));
        shops.put("weapons", weaponShop);
        
        // Tienda de Armaduras
        List<ShopItem> armorShop = new ArrayList<>();
        armorShop.add(createShopItem("iron_helmet", Material.IRON_HELMET, "Casco de Hierro", 40, 20, 1));
        armorShop.add(createShopItem("iron_chestplate", Material.IRON_CHESTPLATE, "Peto de Hierro", 80, 40, 1));
        armorShop.add(createShopItem("iron_leggings", Material.IRON_LEGGINGS, "Pantalones de Hierro", 70, 35, 1));
        armorShop.add(createShopItem("iron_boots", Material.IRON_BOOTS, "Botas de Hierro", 35, 17, 1));
        shops.put("armor", armorShop);
        
        // Tienda de Pociones
        List<ShopItem> potionShop = new ArrayList<>();
        potionShop.add(createShopItem("glass_bottle", Material.GLASS_BOTTLE, "Botella de Cristal", 5, 2, 1));
        potionShop.add(createShopItem("nether_wart", Material.NETHER_WART, "Verruga del Nether", 10, 5, 1));
        potionShop.add(createShopItem("blaze_powder", Material.BLAZE_POWDER, "Polvo de Blaze", 15, 7, 1));
        shops.put("potions", potionShop);
    }
    
    /**
     * Crea un item de tienda con configuración básica
     */
    private ShopItem createShopItem(String id, Material material, String name, 
                                   double buyPrice, double sellPrice, int amount) {
        ShopItem item = new ShopItem(id, material, name, buyPrice, sellPrice, amount);
        item.addLore("§7Item de la tienda");
        return item;
    }
    
    /**
     * Registra una nueva tienda
     */
    public void registerShop(String shopId, List<ShopItem> items) {
        shops.put(shopId, new ArrayList<>(items));
    }
    
    /**
     * Obtiene una tienda por ID
     */
    public List<ShopItem> getShop(String shopId) {
        return shops.get(shopId);
    }
    
    /**
     * Abre una tienda para un jugador
     */
    public void openShop(Player player, String shopId) {
        List<ShopItem> items = shops.get(shopId);
        if (items == null) {
            player.sendMessage("§cTienda no encontrada.");
            return;
        }
        
        // Crear inventario
        Inventory inv = Bukkit.createInventory(null, 54, "§8Tienda - " + shopId);
        
        // Añadir items
        int slot = 0;
        for (ShopItem item : items) {
            if (slot >= 54) break;
            inv.setItem(slot++, item.createItemStack());
        }
        
        player.openInventory(inv);
        openShops.put(player.getUniqueId(), shopId);
    }
    
    /**
     * Procesa la compra de un item
     */
    public boolean buyItem(Player player, ShopItem item, int quantity) {
        if (!item.canBuy()) {
            player.sendMessage("§cEste item no se puede comprar.");
            return false;
        }
        
        double totalPrice = item.getBuyPrice() * quantity;
        
        if (!economyManager.hasEnough(player, totalPrice)) {
            player.sendMessage("§cNo tienes suficiente dinero. Necesitas: " + 
                             economyManager.format(totalPrice));
            return false;
        }
        
        // Verificar espacio en inventario
        ItemStack itemStack = new ItemStack(item.getMaterial(), item.getAmount() * quantity);
        if (!hasInventorySpace(player, itemStack)) {
            player.sendMessage("§cNo tienes suficiente espacio en el inventario.");
            return false;
        }
        
        // Realizar transacción
        if (economyManager.withdraw(player, totalPrice)) {
            player.getInventory().addItem(itemStack);
            player.sendMessage("§aCompraste §f" + (item.getAmount() * quantity) + "x " + 
                             item.getDisplayName() + " §apor " + economyManager.format(totalPrice));
            return true;
        }
        
        return false;
    }
    
    /**
     * Procesa la venta de un item
     */
    public boolean sellItem(Player player, ShopItem item, int quantity) {
        if (!item.canSell()) {
            player.sendMessage("§cEste item no se puede vender.");
            return false;
        }
        
        // Verificar si el jugador tiene el item
        ItemStack itemStack = new ItemStack(item.getMaterial(), item.getAmount() * quantity);
        if (!player.getInventory().containsAtLeast(itemStack, item.getAmount() * quantity)) {
            player.sendMessage("§cNo tienes suficientes items para vender.");
            return false;
        }
        
        double totalPrice = item.getSellPrice() * quantity;
        
        // Remover items del inventario
        player.getInventory().removeItem(itemStack);
        
        // Dar dinero
        economyManager.deposit(player, totalPrice);
        player.sendMessage("§aVendiste §f" + (item.getAmount() * quantity) + "x " + 
                         item.getDisplayName() + " §apor " + economyManager.format(totalPrice));
        return true;
    }
    
    /**
     * Verifica si el jugador tiene espacio en el inventario
     */
    private boolean hasInventorySpace(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int remaining = item.getAmount();
        
        for (ItemStack invItem : inv.getStorageContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) {
                return true;
            }
            if (invItem.isSimilar(item)) {
                int space = invItem.getMaxStackSize() - invItem.getAmount();
                remaining -= space;
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        
        return remaining <= 0;
    }
    
    /**
     * Obtiene la tienda abierta por un jugador
     */
    public String getOpenShop(UUID playerId) {
        return openShops.get(playerId);
    }
    
    /**
     * Cierra la tienda para un jugador
     */
    public void closeShop(UUID playerId) {
        openShops.remove(playerId);
    }
    
    /**
     * Obtiene un item de tienda por material
     */
    public ShopItem getShopItem(String shopId, Material material) {
        List<ShopItem> items = shops.get(shopId);
        if (items == null) return null;
        
        for (ShopItem item : items) {
            if (item.getMaterial() == material) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Obtiene todas las tiendas registradas
     */
    public Map<String, List<ShopItem>> getAllShops() {
        return new HashMap<>(shops);
    }
}
