package com.nightslayer.mmorpg.classes;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Gestor del sistema de clases RPG
 */
public class ClassManager {
    private final Plugin plugin;
    private final Map<UUID, PlayerClass> playerClasses;
    private final Map<String, ClassAbility> abilities;
    private final Gson gson;
    private final File dataFolder;
    
    public ClassManager(Plugin plugin) {
        this.plugin = plugin;
        this.playerClasses = new HashMap<>();
        this.abilities = new HashMap<>();
        this.gson = new Gson();
        this.dataFolder = new File(plugin.getDataFolder(), "classes");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        registerDefaultAbilities();
        startManaRegeneration();
    }
    
    /**
     * Registra las habilidades por defecto de cada clase
     */
    private void registerDefaultAbilities() {
        // Habilidades de Guerrero
        registerAbility(new ClassAbility(
            "warrior_charge", "Carga Brutal", 
            "Carga hacia adelante con gran velocidad",
            20, 10, 1, ClassType.WARRIOR
        ));
        registerAbility(new ClassAbility(
            "warrior_shield", "Escudo Defensivo", 
            "Reduce el daño recibido temporalmente",
            30, 15, 5, ClassType.WARRIOR
        ));
        registerAbility(new ClassAbility(
            "warrior_berserk", "Furia Berserker", 
            "Aumenta tu daño drásticamente",
            50, 30, 10, ClassType.WARRIOR
        ));
        
        // Habilidades de Mago
        registerAbility(new ClassAbility(
            "mage_fireball", "Bola de Fuego", 
            "Lanza una bola de fuego explosiva",
            40, 8, 1, ClassType.MAGE
        ));
        registerAbility(new ClassAbility(
            "mage_teleport", "Teletransporte", 
            "Teletransportarse a corta distancia",
            60, 20, 5, ClassType.MAGE
        ));
        registerAbility(new ClassAbility(
            "mage_meteor", "Lluvia de Meteoros", 
            "Invoca meteoros del cielo",
            100, 45, 10, ClassType.MAGE
        ));
        
        // Habilidades de Arquero
        registerAbility(new ClassAbility(
            "archer_multishot", "Disparo Múltiple", 
            "Dispara múltiples flechas a la vez",
            25, 12, 1, ClassType.ARCHER
        ));
        registerAbility(new ClassAbility(
            "archer_trap", "Trampa Explosiva", 
            "Coloca una trampa en el suelo",
            35, 18, 5, ClassType.ARCHER
        ));
        registerAbility(new ClassAbility(
            "archer_rain", "Lluvia de Flechas", 
            "Invoca flechas del cielo en un área",
            70, 35, 10, ClassType.ARCHER
        ));
        
        // Habilidades de Asesino
        registerAbility(new ClassAbility(
            "assassin_backstab", "Apuñalamiento Furtivo", 
            "Daño crítico aumentado e invisibilidad temporal",
            30, 15, 1, ClassType.ASSASSIN
        ));
        registerAbility(new ClassAbility(
            "assassin_smoke_bomb", "Bomba de Humo", 
            "Crea humo y aumenta velocidad",
            25, 18, 5, ClassType.ASSASSIN
        ));
        registerAbility(new ClassAbility(
            "assassin_poison_blade", "Hoja Envenenada", 
            "Envenena a tus enemigos",
            45, 25, 10, ClassType.ASSASSIN
        ));
        
        // Habilidades de Clérigo
        registerAbility(new ClassAbility(
            "cleric_heal", "Sanación Divina", 
            "Restaura 30% de tu vida máxima",
            40, 12, 1, ClassType.CLERIC
        ));
        registerAbility(new ClassAbility(
            "cleric_holy_shield", "Escudo Sagrado", 
            "Resistencia y regeneración aumentadas",
            50, 20, 5, ClassType.CLERIC
        ));
        registerAbility(new ClassAbility(
            "cleric_divine_light", "Luz Divina", 
            "Cura a todos los aliados cercanos",
            80, 30, 10, ClassType.CLERIC
        ));
        
        // Habilidades de Paladín
        registerAbility(new ClassAbility(
            "paladin_holy_strike", "Golpe Sagrado", 
            "Ataque divino con daño aumentado",
            35, 14, 1, ClassType.PALADIN
        ));
        registerAbility(new ClassAbility(
            "paladin_divine_protection", "Protección Divina", 
            "Inmunidad al fuego y resistencia aumentada",
            60, 25, 5, ClassType.PALADIN
        ));
        registerAbility(new ClassAbility(
            "paladin_consecration", "Consagración", 
            "Área sagrada que daña a enemigos",
            90, 40, 10, ClassType.PALADIN
        ));
    }
    
    /**
     * Registra una habilidad
     */
    public void registerAbility(ClassAbility ability) {
        abilities.put(ability.getId(), ability);
    }
    
    /**
     * Obtiene una habilidad por ID
     */
    public ClassAbility getAbility(String abilityId) {
        return abilities.get(abilityId);
    }
    
    /**
     * Obtiene todas las habilidades de una clase
     */
    public List<ClassAbility> getClassAbilities(ClassType classType) {
        List<ClassAbility> classAbilities = new ArrayList<>();
        for (ClassAbility ability : abilities.values()) {
            if (ability.getClassType() == classType) {
                classAbilities.add(ability);
            }
        }
        classAbilities.sort(Comparator.comparingInt(ClassAbility::getRequiredLevel));
        return classAbilities;
    }
    
    /**
     * Obtiene la clase de un jugador
     */
    public PlayerClass getPlayerClass(Player player) {
        return getPlayerClass(player.getUniqueId());
    }
    
    /**
     * Obtiene la clase de un jugador por UUID
     */
    public PlayerClass getPlayerClass(UUID uuid) {
        return playerClasses.computeIfAbsent(uuid, k -> {
            PlayerClass playerClass = loadPlayerClass(uuid);
            if (playerClass == null) {
                playerClass = new PlayerClass(uuid);
            }
            return playerClass;
        });
    }
    
    /**
     * Establece la clase de un jugador
     */
    public void setPlayerClass(Player player, ClassType classType) {
        PlayerClass playerClass = getPlayerClass(player);
        playerClass.setClassType(classType);
        playerClass.applyToPlayer(player);
        savePlayerClass(playerClass);
        
        player.sendMessage("§a¡Has elegido la clase " + classType.getIcon() + " " + classType.getDisplayName() + "!");
    }
    
    /**
     * Carga la clase de un jugador desde archivo
     */
    private PlayerClass loadPlayerClass(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".json");
        
        if (!file.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            return PlayerClass.fromJson(json);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar clase de jugador " + uuid + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Guarda la clase de un jugador
     */
    public void savePlayerClass(PlayerClass playerClass) {
        File file = new File(dataFolder, playerClass.getPlayerUUID().toString() + ".json");
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(playerClass.toJson(), writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al guardar clase de jugador " + playerClass.getPlayerUUID() + ": " + e.getMessage());
        }
    }
    
    /**
     * Guarda todas las clases de jugadores
     */
    public void saveAll() {
        for (PlayerClass playerClass : playerClasses.values()) {
            savePlayerClass(playerClass);
        }
    }
    
    /**
     * Inicia la regeneración automática de maná
     */
    private void startManaRegeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerClass playerClass : playerClasses.values()) {
                    if (playerClass.getClassType() != ClassType.NONE) {
                        // Regenerar 5% del maná máximo cada segundo
                        int regenAmount = Math.max(1, playerClass.getMaxMana() / 20);
                        playerClass.regenerateMana(regenAmount);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Cada segundo (20 ticks)
    }
    
    /**
     * Usa una habilidad
     */
    public boolean useAbility(Player player, String abilityId) {
        PlayerClass playerClass = getPlayerClass(player);
        ClassAbility ability = getAbility(abilityId);
        
        if (ability == null) {
            player.sendMessage("§c¡Habilidad no encontrada!");
            return false;
        }
        
        if (!ability.canUse(player, playerClass)) {
            if (playerClass.getClassType() != ability.getClassType()) {
                player.sendMessage("§c¡Esta habilidad no es de tu clase!");
            } else if (playerClass.getLevel() < ability.getRequiredLevel()) {
                player.sendMessage("§cNecesitas nivel " + ability.getRequiredLevel() + " para usar esta habilidad!");
            } else if (playerClass.getMana() < ability.getManaCost()) {
                player.sendMessage("§cNo tienes suficiente maná! (" + playerClass.getMana() + "/" + ability.getManaCost() + ")");
            } else if (playerClass.isAbilityOnCooldown(abilityId)) {
                long remaining = playerClass.getRemainingCooldown(abilityId);
                player.sendMessage("§cHabilidad en cooldown! (" + remaining + "s restantes)");
            }
            return false;
        }
        
        ability.execute(player, playerClass);
        savePlayerClass(playerClass);
        return true;
    }
}
