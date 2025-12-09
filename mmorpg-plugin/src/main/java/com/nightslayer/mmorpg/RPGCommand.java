package com.nightslayer.mmorpg;

import com.nightslayer.mmorpg.commands.MobCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RPGCommand implements CommandExecutor {
    
    private final MMORPGPlugin plugin;
    private final MobCommand mobCommand = new MobCommand();
    
    public RPGCommand(MMORPGPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        String worldName = player.getWorld().getName();
        
        // Verificar si el jugador está en un mundo RPG
        if (!plugin.isRPGWorld(worldName)) {
            player.sendMessage("§cEste mundo no tiene el modo RPG activado.");
            return true;
        }
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        if (args.length > 0 && args[0].equalsIgnoreCase("mob")) {
            // /rpg mob ...
            String[] mobArgs = new String[args.length - 1];
            System.arraycopy(args, 1, mobArgs, 0, mobArgs.length);
            return mobCommand.onCommand(sender, command, label, mobArgs);
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                showHelp(player);
                break;
            case "status":
                showStatus(player, worldName);
                break;
            case "info":
                showWorldInfo(player, worldName);
                break;
            default:
                player.sendMessage("§cComando desconocido. Usa /rpg help");
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l=== MMORPG Plugin ===");
        player.sendMessage("§e/rpg help §7- Muestra esta ayuda");
        player.sendMessage("§e/rpg status §7- Muestra tu estado RPG");
        player.sendMessage("§e/rpg info §7- Información del mundo RPG actual");
        player.sendMessage("§7Más comandos disponibles próximamente...");
    }
    
    private void showStatus(Player player, String worldName) {
        player.sendMessage("§6§l=== Tu Estado RPG ===");
        player.sendMessage("§eMundo: §f" + worldName);
        player.sendMessage("§eNivel: §f1 §7(Sistema de niveles próximamente)");
        player.sendMessage("§eClase: §fNinguna §7(Elige una clase próximamente)");
        player.sendMessage("§eQuests activas: §f0 §7(Sistema de quests próximamente)");
    }
    
    private void showWorldInfo(Player player, String worldName) {
        WorldMetadata metadata = plugin.getWorldRPGManager().getWorldMetadata(worldName);
        
        if (metadata == null) {
            player.sendMessage("§cNo se pudo obtener información del mundo.");
            return;
        }
        
        player.sendMessage("§6§l=== Información del Mundo RPG ===");
        player.sendMessage("§eNombre: §f" + metadata.getName());
        player.sendMessage("§eDescripción: §f" + metadata.getDescription());
        player.sendMessage("§eModo RPG: §a✓ Activado");
        
        WorldMetadata.RPGConfig config = metadata.getRpgConfig();
        player.sendMessage("§6Características:");
        player.sendMessage("  §eClases: " + (config.isClassesEnabled() ? "§a✓" : "§c✗"));
        player.sendMessage("  §eQuests: " + (config.isQuestsEnabled() ? "§a✓" : "§c✗"));
        player.sendMessage("  §eNPCs: " + (config.isNpcsEnabled() ? "§a✓" : "§c✗"));
        player.sendMessage("  §eEconomía: " + (config.isEconomyEnabled() ? "§a✓" : "§c✗"));
    }
}
