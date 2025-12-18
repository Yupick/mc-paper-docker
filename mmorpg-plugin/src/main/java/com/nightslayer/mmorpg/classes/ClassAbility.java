package com.nightslayer.mmorpg.classes;

import org.bukkit.entity.Player;

/**
 * Representa una habilidad específica de una clase
 */
public class ClassAbility {
    private final String id;
    private final String name;
    private final String description;
    private final int manaCost;
    private final int cooldown; // En segundos
    private final int requiredLevel;
    private final ClassType classType;
    
    public ClassAbility(String id, String name, String description, 
                       int manaCost, int cooldown, int requiredLevel, ClassType classType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.requiredLevel = requiredLevel;
        this.classType = classType;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getManaCost() {
        return manaCost;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public ClassType getClassType() {
        return classType;
    }
    
    /**
     * Verifica si el jugador puede usar esta habilidad
     */
    public boolean canUse(Player player, PlayerClass playerClass) {
        if (playerClass.getClassType() != this.classType) {
            return false;
        }
        
        if (playerClass.getLevel() < this.requiredLevel) {
            return false;
        }
        
        if (playerClass.getMana() < this.manaCost) {
            return false;
        }
        
        return !playerClass.isAbilityOnCooldown(this.id);
    }
    
    /**
     * Ejecuta la habilidad
     */
    public void execute(Player player, PlayerClass playerClass) {
        if (!canUse(player, playerClass)) {
            return;
        }
        
        // Consumir maná
        playerClass.consumeMana(this.manaCost);
        
        // Activar cooldown
        playerClass.setAbilityCooldown(this.id, System.currentTimeMillis() + (cooldown * 1000L));
        
        // Ejecutar efecto específico según el ID
        executeEffect(player, playerClass);
    }
    
    /**
     * Efecto específico de la habilidad (implementar según tipo)
     */
    private void executeEffect(Player player, PlayerClass playerClass) {
        // Implementación específica de cada habilidad
        switch (id) {
            // GUERRERO
            case "warrior_charge":
                player.setVelocity(player.getLocation().getDirection().multiply(2.0));
                player.sendMessage("§c¡Carga de Guerrero activada!");
                break;
            
            // MAGO
            case "mage_fireball":
                player.launchProjectile(org.bukkit.entity.Fireball.class);
                player.sendMessage("§6¡Bola de fuego lanzada!");
                break;
            
            // ARQUERO
            case "archer_multishot":
                for (int i = 0; i < 3; i++) {
                    player.launchProjectile(org.bukkit.entity.Arrow.class);
                }
                player.sendMessage("§a¡Disparo múltiple activado!");
                break;
            
            // ASESINO
            case "assassin_backstab":
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH, 100, 1));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.INVISIBILITY, 60, 0));
                player.sendMessage("§8¡Apuñalamiento furtivo! +50% daño crítico");
                break;
            
            case "assassin_smoke_bomb":
                player.getWorld().createExplosion(player.getLocation(), 0F);
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, 100, 1));
                player.sendMessage("§7¡Bomba de humo! +Velocidad");
                break;
            
            case "assassin_poison_blade":
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH, 200, 0));
                player.sendMessage("§2¡Hoja envenenada! Próximo golpe envenena");
                break;
            
            // CLÉRIGO
            case "cleric_heal":
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                double healthToHeal = Math.min(maxHealth * 0.3, maxHealth - player.getHealth());
                player.setHealth(player.getHealth() + healthToHeal);
                player.sendMessage("§b¡Sanación divina! +" + (int)healthToHeal + " HP");
                break;
            
            case "cleric_holy_shield":
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.RESISTANCE, 200, 1));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION, 200, 0));
                player.sendMessage("§e¡Escudo sagrado! Resistencia +40%");
                break;
            
            case "cleric_divine_light":
                player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10).forEach(entity -> {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        double targetMaxHealth = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                        target.setHealth(Math.min(target.getHealth() + 4, targetMaxHealth));
                    }
                });
                player.sendMessage("§f¡Luz divina! Aliados cercanos curados");
                break;
            
            // PALADÍN
            case "paladin_holy_strike":
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.STRENGTH, 100, 2));
                player.sendMessage("§6¡Golpe sagrado! +75% daño");
                break;
            
            case "paladin_divine_protection":
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.RESISTANCE, 300, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 300, 0));
                player.sendMessage("§e¡Protección divina! Inmune a fuego");
                break;
            
            case "paladin_consecration":
                player.getWorld().strikeLightningEffect(player.getLocation());
                player.getWorld().getNearbyEntities(player.getLocation(), 8, 8, 8).forEach(entity -> {
                    if (entity instanceof org.bukkit.entity.Monster) {
                        ((org.bukkit.entity.Monster) entity).damage(10.0, player);
                    }
                });
                player.sendMessage("§f¡Consagración! Área sagrada activada");
                break;
        }
    }
}
