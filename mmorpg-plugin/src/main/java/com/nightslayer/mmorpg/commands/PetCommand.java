package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.pets.*;
import com.nightslayer.mmorpg.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Comando para gestionar mascotas y monturas RPG
 * Uso: /pet <adopt|feed|evolve|release|equip|info|mounts|unlock-mount|equip-mount|list>
 */
public class PetCommand implements CommandExecutor, TabCompleter {
    private final PetManager petManager;
    private final EconomyManager economyManager;
    
    public PetCommand(PetManager petManager, EconomyManager economyManager) {
        this.petManager = petManager;
        this.economyManager = economyManager;
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
                showAvailablePets(player);
                break;
                
            case "adopt":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet adopt <pet_id>");
                    return true;
                }
                adoptPet(player, args[1]);
                break;
                
            case "feed":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet feed <pet_id>");
                    return true;
                }
                feedPet(player, args[1]);
                break;
                
            case "evolve":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet evolve <pet_id>");
                    return true;
                }
                evolvePet(player, args[1]);
                break;
                
            case "release":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet release <pet_id>");
                    return true;
                }
                releasePet(player, args[1]);
                break;
                
            case "equip":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet equip <pet_id|none>");
                    return true;
                }
                equipPet(player, args[1]);
                break;
                
            case "info":
                if (args.length < 2) {
                    showMyPets(player);
                } else {
                    showPetInfo(player, args[1]);
                }
                break;
                
            case "mounts":
                showAvailableMounts(player);
                break;
                
            case "unlock-mount":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet unlock-mount <mount_id>");
                    return true;
                }
                unlockMount(player, args[1]);
                break;
                
            case "equip-mount":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /pet equip-mount <mount_id|none>");
                    return true;
                }
                equipMount(player, args[1]);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcomandos
            completions.addAll(Arrays.asList("list", "adopt", "feed", "evolve", "release", "equip", "info", "mounts", "unlock-mount", "equip-mount"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "adopt":
                case "info":
                    // Sugerir IDs de mascotas disponibles
                    completions.addAll(petManager.getAllPetIds());
                    break;
                case "feed":
                case "evolve":
                case "release":
                case "equip":
                    // Sugerir IDs de mascotas del jugador
                    PlayerPetData playerData = petManager.getPlayerData(player.getUniqueId().toString());
                    if (playerData != null) {
                        completions.addAll(playerData.getOwnedPetIds());
                    }
                    break;
                case "unlock-mount":
                case "equip-mount":
                    // Sugerir IDs de monturas
                    completions.addAll(petManager.getAllMountIds());
                    break;
            }
        }
        
        return completions;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l=== Comandos de Mascotas y Monturas ===");
        player.sendMessage("§e/pet list §7- Ver mascotas disponibles");
        player.sendMessage("§e/pet adopt <pet_id> §7- Adoptar una mascota");
        player.sendMessage("§e/pet feed <pet_id> §7- Alimentar tu mascota");
        player.sendMessage("§e/pet evolve <pet_id> §7- Evolucionar tu mascota");
        player.sendMessage("§e/pet release <pet_id> §7- Liberar una mascota");
        player.sendMessage("§e/pet equip <pet_id|none> §7- Equipar/desequipar mascota");
        player.sendMessage("§e/pet info [pet_id] §7- Ver info de mascota");
        player.sendMessage("§e/pet mounts §7- Ver monturas disponibles");
        player.sendMessage("§e/pet unlock-mount <mount_id> §7- Desbloquear montura");
        player.sendMessage("§e/pet equip-mount <mount_id|none> §7- Equipar/desequipar montura");
    }
    
    private void showAvailablePets(Player player) {
        Collection<Pet> petsCollection = petManager.getAllPets();
        
        player.sendMessage("§6§l=== Mascotas Disponibles ===");
        player.sendMessage("");
        
        for (Pet pet : petsCollection) {
            String rarityColor = getRarityColor(pet.getRarity());
            player.sendMessage("§e" + pet.getId() + " §7- " + rarityColor + pet.getName());
            player.sendMessage("  §7Tipo: §f" + pet.getType().toString());
            player.sendMessage("  §7Rareza: " + rarityColor + pet.getRarity());
            player.sendMessage("  §7Costo: §6" + pet.getAdoptionCost() + " monedas");
            player.sendMessage("");
        }
        
        player.sendMessage("§7Usa §e/pet adopt <pet_id> §7para adoptar");
    }
    
    private void adoptPet(Player player, String petId) {
        try {
            String playerUuid = player.getUniqueId().toString();
            int playerCoins = (int) economyManager.getRPGCoins(player);
            
            // Validar que la mascota existe
            Pet pet = petManager.getPet(petId);
            if (pet == null) {
                player.sendMessage("§cMascota no encontrada: " + petId);
                return;
            }
            
            // Adoptar mascota
            boolean success = petManager.adoptPet(playerUuid, petId, playerCoins);
            
            if (success) {
                int cost = pet.getAdoptionCost();
                economyManager.withdrawRPGCoins(player, cost);
                player.sendMessage("§a¡Has adoptado a " + pet.getName() + "! (-" + cost + " monedas)");
            } else {
                player.sendMessage("§cNo puedes adoptar esta mascota (verifica límite o monedas insuficientes)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al adoptar mascota: " + e.getMessage());
        }
    }
    
    private void feedPet(Player player, String petId) {
        try {
            boolean success = petManager.feedPet(player.getUniqueId().toString(), petId);
            
            if (success) {
                player.sendMessage("§aHas alimentado a tu mascota");
            } else {
                player.sendMessage("§cNo se pudo alimentar a la mascota (verifica que la tengas y esté viva)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al alimentar mascota: " + e.getMessage());
        }
    }
    
    private void evolvePet(Player player, String petId) {
        try {
            boolean success = petManager.evolvePet(player.getUniqueId().toString(), petId);
            
            if (success) {
                player.sendMessage("§a§l¡Tu mascota ha evolucionado!");
            } else {
                player.sendMessage("§cNo se pudo evolucionar (verifica XP requerido o nivel máximo alcanzado)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al evolucionar mascota: " + e.getMessage());
        }
    }
    
    private void releasePet(Player player, String petId) {
        try {
            boolean success = petManager.releasePet(player.getUniqueId().toString(), petId);
            
            if (success) {
                player.sendMessage("§eHas liberado a tu mascota");
            } else {
                player.sendMessage("§cNo se pudo liberar la mascota (puede estar activa o no la tienes)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al liberar mascota: " + e.getMessage());
        }
    }
    
    private void equipPet(Player player, String petId) {
        try {
            if (petId.equalsIgnoreCase("none")) {
                petManager.setActivePet(player.getUniqueId().toString(), null);
                player.sendMessage("§eHas desequipado tu mascota activa");
                return;
            }
            
            boolean success = petManager.setActivePet(player.getUniqueId().toString(), petId);
            
            if (success) {
                player.sendMessage("§aHas equipado tu mascota: " + petId);
            } else {
                player.sendMessage("§cNo puedes equipar esta mascota (verifica que la tengas)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al equipar mascota: " + e.getMessage());
        }
    }
    
    private void showMyPets(Player player) {
        PlayerPetData playerData = petManager.getPlayerData(player.getUniqueId().toString());
        
        if (playerData == null || playerData.getOwnedPets().isEmpty()) {
            player.sendMessage("§cNo tienes mascotas adoptadas");
            player.sendMessage("§7Usa §e/pet list §7para ver mascotas disponibles");
            return;
        }
        
        player.sendMessage("§6§l=== Mis Mascotas ===");
        player.sendMessage("");
        
        for (PlayerPetData.OwnedPet ownedPet : playerData.getOwnedPets()) {
            Pet pet = petManager.getPet(ownedPet.getPetId());
            if (pet == null) continue;
            
            boolean isActive = ownedPet.getPetId().equals(playerData.getActivePetId());
            String activeMarker = isActive ? "§a[ACTIVA] " : "";
            
            player.sendMessage(activeMarker + "§e" + pet.getName());
            player.sendMessage("  §7Nivel: §f" + ownedPet.getLevel());
            player.sendMessage("  §7Experiencia: §f" + ownedPet.getExperience() + " XP");
            player.sendMessage("  §7Salud: §c" + ownedPet.getCurrentHealth() + "§7/§c" + pet.getStat("health"));
            player.sendMessage("");
        }
    }
    
    private void showPetInfo(Player player, String petId) {
        Pet pet = petManager.getPet(petId);
        
        if (pet == null) {
            player.sendMessage("§cMascota no encontrada: " + petId);
            return;
        }
        
        String rarityColor = getRarityColor(pet.getRarity());
        
        player.sendMessage("§6§l=== " + pet.getName() + " ===");
        player.sendMessage("");
        player.sendMessage("§eID: §f" + pet.getId());
        player.sendMessage("§eTipo: §f" + pet.getType().toString());
        player.sendMessage("§eRareza: " + rarityColor + pet.getRarity());
        player.sendMessage("§eDescripción: §7" + pet.getDescription());
        player.sendMessage("");
        player.sendMessage("§6Estadísticas Base:");
        player.sendMessage("  §7Salud: §c" + pet.getStat("health"));
        player.sendMessage("  §7Daño: §e" + pet.getStat("damage"));
        player.sendMessage("  §7Velocidad: §b" + pet.getStat("speed"));
        player.sendMessage("");
        player.sendMessage("§eCosto de Adopción: §6" + pet.getAdoptionCost() + " monedas");
    }
    
    private void showAvailableMounts(Player player) {
        Collection<Mount> mountsCollection = petManager.getAllMounts();
        PlayerPetData playerData = petManager.getPlayerData(player.getUniqueId().toString());
        
        player.sendMessage("§6§l=== Monturas Disponibles ===");
        player.sendMessage("");
        
        for (Mount mount : mountsCollection) {
            boolean unlocked = playerData != null && playerData.hasMount(mount.getId());
            String status = unlocked ? "§a[DESBLOQUEADA]" : "§7[BLOQUEADA]";
            String rarityColor = getRarityColor(mount.getRarity());
            
            player.sendMessage(status + " §e" + mount.getId() + " §7- " + rarityColor + mount.getName());
            player.sendMessage("  §7Velocidad: §b" + mount.getSpeed() + "x");
            player.sendMessage("  §7Nivel requerido: §6" + mount.getUnlockLevel());
            player.sendMessage("  §7Costo: §6" + mount.getUnlockCost() + " monedas");
            if (mount.getSpecialAbility() != null) {
                player.sendMessage("  §7Habilidad: §d" + mount.getSpecialAbility());
            }
            player.sendMessage("");
        }
        
        player.sendMessage("§7Usa §e/pet unlock-mount <mount_id> §7para desbloquear");
    }
    
    private void unlockMount(Player player, String mountId) {
        try {
            String playerUuid = player.getUniqueId().toString();
            int playerCoins = (int) economyManager.getRPGCoins(player);
            int playerLevel = player.getLevel(); // Simplificado, debería usar el sistema de niveles RPG
            
            boolean success = petManager.unlockMount(playerUuid, mountId, playerCoins, playerLevel);
            
            if (success) {
                Mount mount = petManager.getMount(mountId);
                int cost = mount != null ? mount.getUnlockCost() : 0;
                economyManager.withdrawRPGCoins(player, cost);
                player.sendMessage("§a¡Has desbloqueado la montura! (-" + cost + " monedas)");
            } else {
                player.sendMessage("§cNo puedes desbloquear esta montura (nivel o monedas insuficientes)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al desbloquear montura: " + e.getMessage());
        }
    }
    
    private void equipMount(Player player, String mountId) {
        try {
            if (mountId.equalsIgnoreCase("none")) {
                petManager.setActiveMount(player.getUniqueId().toString(), null);
                player.sendMessage("§eHas desequipado tu montura activa");
                return;
            }
            
            boolean success = petManager.setActiveMount(player.getUniqueId().toString(), mountId);
            
            if (success) {
                player.sendMessage("§aHas equipado tu montura: " + mountId);
            } else {
                player.sendMessage("§cNo puedes equipar esta montura (verifica que la tengas desbloqueada)");
            }
        } catch (Exception e) {
            player.sendMessage("§cError al equipar montura: " + e.getMessage());
        }
    }
    
    private String getRarityColor(String rarity) {
        switch (rarity.toUpperCase()) {
            case "COMMON":
                return "§f";
            case "UNCOMMON":
                return "§a";
            case "RARE":
                return "§9";
            case "LEGENDARY":
                return "§6";
            default:
                return "§7";
        }
    }
}
