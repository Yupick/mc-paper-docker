package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.mobs.CustomMobManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MobCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("spawn")) {
            sender.sendMessage(ChatColor.RED + "Uso: /rpg mob spawn <id> [mundo] [x] [y] [z]");
            return true;
        }
        String mobId = args[1];
        World world;
        double x, y, z;
        if (args.length >= 6) {
            world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Mundo no encontrado: " + args[2]);
                return true;
            }
            try {
                x = Double.parseDouble(args[3]);
                y = Double.parseDouble(args[4]);
                z = Double.parseDouble(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Coordenadas inválidas.");
                return true;
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            world = player.getWorld();
            Location loc = player.getLocation();
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
        } else {
            sender.sendMessage(ChatColor.RED + "Debes especificar mundo y coordenadas si no eres jugador.");
            return true;
        }
        // Lógica de spawn del mob custom
        boolean success = CustomMobManager.spawnCustomMob(mobId, world, x, y, z);
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "Mob '" + mobId + "' spawneado en " + world.getName() + " (" + x + ", " + y + ", " + z + ")");
        } else {
            sender.sendMessage(ChatColor.RED + "No se pudo spawnear el mob: " + mobId);
        }
        return true;
    }
}
