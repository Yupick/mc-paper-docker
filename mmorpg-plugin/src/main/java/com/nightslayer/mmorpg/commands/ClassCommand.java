package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.classes.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando para gestionar clases RPG
 * Uso: /class <list|choose|info|skills|use>
 */
public class ClassCommand implements CommandExecutor, TabCompleter {
    private final ClassManager classManager;
    
    public ClassCommand(ClassManager classManager) {
        this.classManager = classManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                showClassList(player);
                break;
                
            case "choose":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /class choose <guerrero|mago|arquero>");
                    return true;
                }
                chooseClass(player, args[1]);
                break;
                
            case "info":
                if (args.length < 2) {
                    showPlayerInfo(player);
                } else {
                    showClassInfo(player, args[1]);
                }
                break;
                
            case "skills":
                showPlayerSkills(player);
                break;
                
            case "use":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /class use <habilidad>");
                    return true;
                }
                useAbility(player, args[1]);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l=== Comandos de Clases RPG ===");
        player.sendMessage("§e/class list §7- Ver todas las clases disponibles");
        player.sendMessage("§e/class choose <clase> §7- Elegir tu clase");
        player.sendMessage("§e/class info [clase] §7- Ver información de clase");
        player.sendMessage("§e/class skills §7- Ver tus habilidades");
        player.sendMessage("§e/class use <habilidad> §7- Usar una habilidad");
    }
    
    private void showClassList(Player player) {
        player.sendMessage("§6§l=== Clases Disponibles ===");
        
        for (ClassType type : ClassType.values()) {
            if (type == ClassType.NONE) continue;
            
            ClassStats stats = type.getBaseStats();
            player.sendMessage("");
            player.sendMessage("§e" + type.getIcon() + " " + type.getDisplayName());
            player.sendMessage("§7" + type.getDescription());
            player.sendMessage("§fVida: §c" + stats.getBaseHealth() + " §f| Maná: §b" + stats.getBaseMana());
            player.sendMessage("§fDefensa: §7" + stats.getBaseDefense() + " §f| Poder Mágico: §d" + stats.getBaseMagicPower());
        }
        
        player.sendMessage("");
        player.sendMessage("§aUsa §e/class choose <clase> §apara elegir tu clase");
    }
    
    private void chooseClass(Player player, String className) {
        PlayerClass playerClass = classManager.getPlayerClass(player);
        
        // Verificar si ya tiene una clase
        if (playerClass.getClassType() != ClassType.NONE) {
            player.sendMessage("§c¡Ya has elegido una clase! (" + playerClass.getClassType().getDisplayName() + ")");
            player.sendMessage("§7Contacta a un administrador si deseas cambiarla");
            return;
        }
        
        ClassType newClass = ClassType.fromString(className);
        
        if (newClass == ClassType.NONE) {
            player.sendMessage("§cClase no válida. Usa: guerrero, mago o arquero");
            return;
        }
        
        classManager.setPlayerClass(player, newClass);
        
        // Mostrar información de la clase elegida
        player.sendMessage("");
        player.sendMessage("§6§l=== " + newClass.getIcon() + " " + newClass.getDisplayName() + " ===");
        player.sendMessage("§7" + newClass.getDescription());
        player.sendMessage("");
        player.sendMessage("§aUsa §e/class skills §apara ver tus habilidades");
    }
    
    private void showPlayerInfo(Player player) {
        PlayerClass playerClass = classManager.getPlayerClass(player);
        
        if (playerClass.getClassType() == ClassType.NONE) {
            player.sendMessage("§cNo has elegido ninguna clase todavía");
            player.sendMessage("§7Usa §e/class list §7para ver las clases disponibles");
            return;
        }
        
        ClassType type = playerClass.getClassType();
        ClassStats stats = type.getBaseStats().getStatsForLevel(playerClass.getLevel());
        
        player.sendMessage("§6§l=== Tu Información de Clase ===");
        player.sendMessage("§eClase: §f" + type.getIcon() + " " + type.getDisplayName());
        player.sendMessage("§eNivel: §f" + playerClass.getLevel());
        player.sendMessage("§eExperiencia: §f" + playerClass.getExperience() + "/" + playerClass.getRequiredExperience());
        player.sendMessage("§eManá: §b" + playerClass.getMana() + "/" + playerClass.getMaxMana());
        player.sendMessage("");
        player.sendMessage("§6Estadísticas:");
        player.sendMessage("§fVida Máxima: §c" + stats.getBaseHealth());
        player.sendMessage("§fManá Máximo: §b" + stats.getBaseMana());
        player.sendMessage("§fDefensa: §7" + stats.getBaseDefense());
        player.sendMessage("§fPoder Mágico: §d" + stats.getBaseMagicPower());
        player.sendMessage("§fVelocidad de Ataque: §a" + stats.getBaseAttackSpeed());
    }
    
    private void showClassInfo(Player player, String className) {
        ClassType type = ClassType.fromString(className);
        
        if (type == ClassType.NONE) {
            player.sendMessage("§cClase no válida");
            return;
        }
        
        ClassStats stats = type.getBaseStats();
        List<ClassAbility> abilities = classManager.getClassAbilities(type);
        
        player.sendMessage("§6§l=== " + type.getIcon() + " " + type.getDisplayName() + " ===");
        player.sendMessage("§7" + type.getDescription());
        player.sendMessage("");
        player.sendMessage("§6Estadísticas Base:");
        player.sendMessage("§fVida: §c" + stats.getBaseHealth() + " §f| Maná: §b" + stats.getBaseMana());
        player.sendMessage("§fDefensa: §7" + stats.getBaseDefense() + " §f| Poder Mágico: §d" + stats.getBaseMagicPower());
        player.sendMessage("");
        player.sendMessage("§6Habilidades:");
        
        for (ClassAbility ability : abilities) {
            player.sendMessage("§e" + ability.getName() + " §7(Nivel " + ability.getRequiredLevel() + ")");
            player.sendMessage("  §f" + ability.getDescription());
            player.sendMessage("  §bManá: " + ability.getManaCost() + " §7| Cooldown: " + ability.getCooldown() + "s");
        }
    }
    
    private void showPlayerSkills(Player player) {
        PlayerClass playerClass = classManager.getPlayerClass(player);
        
        if (playerClass.getClassType() == ClassType.NONE) {
            player.sendMessage("§cNo has elegido ninguna clase todavía");
            return;
        }
        
        List<ClassAbility> abilities = classManager.getClassAbilities(playerClass.getClassType());
        
        player.sendMessage("§6§l=== Tus Habilidades ===");
        player.sendMessage("§eClase: §f" + playerClass.getClassType().getIcon() + " " + playerClass.getClassType().getDisplayName());
        player.sendMessage("§eNivel: §f" + playerClass.getLevel());
        player.sendMessage("§eMaña actual: §b" + playerClass.getMana() + "/" + playerClass.getMaxMana());
        player.sendMessage("");
        
        for (ClassAbility ability : abilities) {
            boolean unlocked = playerClass.getLevel() >= ability.getRequiredLevel();
            boolean onCooldown = playerClass.isAbilityOnCooldown(ability.getId());
            boolean hasEnoughMana = playerClass.getMana() >= ability.getManaCost();
            
            String status;
            if (!unlocked) {
                status = "§7🔒 Bloqueada (Nivel " + ability.getRequiredLevel() + ")";
            } else if (onCooldown) {
                long remaining = playerClass.getRemainingCooldown(ability.getId());
                status = "§c⏱ Cooldown (" + remaining + "s)";
            } else if (!hasEnoughMana) {
                status = "§9❄ Sin maná";
            } else {
                status = "§a✓ Disponible";
            }
            
            player.sendMessage("§e" + ability.getName() + " " + status);
            player.sendMessage("  §f" + ability.getDescription());
            player.sendMessage("  §bManá: " + ability.getManaCost() + " §7| Cooldown: " + ability.getCooldown() + "s");
            player.sendMessage("  §7Usa: §e/class use " + ability.getId());
            player.sendMessage("");
        }
    }
    
    private void useAbility(Player player, String abilityId) {
        PlayerClass playerClass = classManager.getPlayerClass(player);
        
        if (playerClass.getClassType() == ClassType.NONE) {
            player.sendMessage("§cNo has elegido ninguna clase todavía");
            return;
        }
        
        classManager.useAbility(player, abilityId);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "choose", "info", "skills", "use"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("choose") || args[0].equalsIgnoreCase("info")) {
                completions.addAll(Arrays.asList("guerrero", "mago", "arquero"));
            } else if (args[0].equalsIgnoreCase("use") && sender instanceof Player) {
                Player player = (Player) sender;
                PlayerClass playerClass = classManager.getPlayerClass(player);
                
                if (playerClass.getClassType() != ClassType.NONE) {
                    for (ClassAbility ability : classManager.getClassAbilities(playerClass.getClassType())) {
                        completions.add(ability.getId());
                    }
                }
            }
        }
        
        return completions;
    }
}
