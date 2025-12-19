package com.nightslayer.mmorpg.items;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.RPGPathResolver;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Gestiona items RPG custom con atributos y rarezas
 * Lee desde: plugins/MMORPGPlugin/data/items.json (universal)
 */
public class ItemManager {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final Map<String, RPGItem> items;
    private final Map<String, Rarity> rarities;
    private final File itemsFile;
    private final Gson gson;
    
    public ItemManager(MMORPGPlugin plugin) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.items = new HashMap<>();
        this.rarities = new HashMap<>();
        this.itemsFile = pathResolver.getUniversalFile("items.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        // Asegurar carpeta y archivo de items (universal)
        pathResolver.ensureUniversalDataDirExists();
        if (!itemsFile.exists()) {
            createDefaultItemsFile();
        }
        
        loadItems();
    }

    private void createDefaultItemsFile() {
        try (Writer writer = new FileWriter(itemsFile)) {
            ItemData data = new ItemData();
            data.rarities = new HashMap<>();
            data.items = new ArrayList<>();
            gson.toJson(data, writer);
            plugin.getLogger().info("Archivo items.json creado por defecto en " + itemsFile.getPath());
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo crear items.json por defecto: " + e.getMessage());
        }
    }
    
    /**
     * Carga items desde items.json
     */
    public void loadItems() {
        if (!itemsFile.exists()) {
            plugin.getLogger().warning("items.json no encontrado");
            return;
        }
        
        try (Reader reader = new FileReader(itemsFile)) {
            Type type = new TypeToken<ItemData>(){}.getType();
            ItemData data = gson.fromJson(reader, type);
            
            if (data != null) {
                // Cargar rarezas
                if (data.rarities != null) {
                    rarities.clear();
                    for (Map.Entry<String, RarityData> entry : data.rarities.entrySet()) {
                        RarityData rd = entry.getValue();
                        rarities.put(entry.getKey(), new Rarity(
                            entry.getKey(),
                            rd.color,
                            rd.dropMultiplier,
                            rd.attributeMultiplier,
                            rd.dropChance,
                            rd.description
                        ));
                    }
                }
                
                // Cargar items
                if (data.items != null) {
                    items.clear();
                    for (RPGItemData itemData : data.items) {
                        Material material;
                        try {
                            material = Material.valueOf(itemData.material.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Material inválido: " + itemData.material);
                            continue;
                        }
                        
                        Rarity rarity = rarities.get(itemData.rarity);
                        if (rarity == null) {
                            plugin.getLogger().warning("Rareza inválida: " + itemData.rarity);
                            continue;
                        }
                        
                        RPGItem item = new RPGItem(
                            itemData.id,
                            itemData.name,
                            material,
                            rarity,
                            itemData.attributes != null ? itemData.attributes : new HashMap<>(),
                            itemData.enchantments != null ? itemData.enchantments : new ArrayList<>()
                        );
                        
                        items.put(itemData.id, item);
                    }
                }
                
                plugin.getLogger().info("Cargados " + items.size() + " items RPG");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error al cargar items: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene un item RPG por su ID
     */
    public RPGItem getItem(String id) {
        return items.get(id);
    }
    
    /**
     * Crea un ItemStack desde un RPGItem
     */
    public ItemStack createItemStack(String itemId) {
        RPGItem rpgItem = items.get(itemId);
        if (rpgItem == null) {
            return null;
        }
        
        ItemStack itemStack = new ItemStack(rpgItem.getMaterial(), 1);
        ItemMeta meta = itemStack.getItemMeta();
        
        if (meta != null) {
            // Nombre con color de rareza
            String coloredName = ChatColor.translateAlternateColorCodes('&', 
                rpgItem.getRarity().getColor() + rpgItem.getName());
            meta.setDisplayName(coloredName);
            
            // Lore con atributos
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + rpgItem.getRarity().getDescription());
            
            if (!rpgItem.getAttributes().isEmpty()) {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Atributos:");
                for (Map.Entry<String, Double> attr : rpgItem.getAttributes().entrySet()) {
                    lore.add(ChatColor.GRAY + "  +" + attr.getValue() + " " + attr.getKey());
                }
            }
            
            meta.setLore(lore);
            
            // Aplicar encantamientos
            for (EnchantmentData enchData : rpgItem.getEnchantments()) {
                try {
                    Enchantment ench = Enchantment.getByName(enchData.type);
                    if (ench != null) {
                        meta.addEnchant(ench, enchData.level, true);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Encantamiento inválido: " + enchData.type);
                }
            }
            
            itemStack.setItemMeta(meta);
        }
        
        return itemStack;
    }
    
    /**
     * Obtiene un item aleatorio basado en rareza
     */
    public String getRandomItemByRarity(String rarityName) {
        Random random = new Random();
        List<String> itemsOfRarity = new ArrayList<>();
        
        for (Map.Entry<String, RPGItem> entry : items.entrySet()) {
            if (entry.getValue().getRarity().getName().equals(rarityName)) {
                itemsOfRarity.add(entry.getKey());
            }
        }
        
        if (itemsOfRarity.isEmpty()) {
            return null;
        }
        
        return itemsOfRarity.get(random.nextInt(itemsOfRarity.size()));
    }
    
    // Clases de datos para deserialización
    private static class ItemData {
        List<RPGItemData> items;
        Map<String, RarityData> rarities;
    }
    
    private static class RPGItemData {
        String id;
        String name;
        String material;
        String rarity;
        Map<String, Double> attributes;
        List<EnchantmentData> enchantments;
    }
    
    private static class RarityData {
        String color;
        double dropMultiplier;
        double attributeMultiplier;
        double dropChance;
        String description;
    }
    
    public static class EnchantmentData {
        public String type;
        public int level;
    }
    
    public Map<String, Rarity> getRarities() {
        return rarities;
    }
}
