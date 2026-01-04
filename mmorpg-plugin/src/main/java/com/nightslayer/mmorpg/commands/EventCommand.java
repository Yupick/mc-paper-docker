package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.MMORPGPlugin;
import com.nightslayer.mmorpg.events.EventConfig;
import com.nightslayer.mmorpg.events.EventManager;
import com.nightslayer.mmorpg.events.EventSession;
import com.nightslayer.mmorpg.mobs.MobManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SPRINT 2: Comando completo de gestión de eventos
 * /event list - Listar eventos configurados
 * /event info <id> - Info detallada de evento
 * /event active - Ver eventos activos
 * /event start <id> <mundo> - Iniciar manualmente
 * /event stop <id> - Detener manualmente
 * /event reload - Recargar events_config.json
 * /event stats <id> - Estadísticas de evento
 * /event currency <jugador> - Ver monedas de evento
 * /event currency <jugador> add <n> - Dar monedas (admin)
 * /event currency <jugador> set <n> - Establecer monedas (admin)
 * /event leaderboard <id> - Top jugadores del evento
 */
public class EventCommand implements CommandExecutor, TabCompleter {
    
    private final MMORPGPlugin plugin;
    private final EventManager eventManager;
    private final MobManager mobManager;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public EventCommand(MMORPGPlugin plugin, EventManager eventManager, MobManager mobManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.mobManager = mobManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleList(sender);
            
            case "info":
                if (args.length < 2) {
                    sender.sendMessage("§c§lEvento ID requerido: §7/event info <id>");
                    return true;
                }
                return handleInfo(sender, args[1]);
            
            case "active":
                return handleActive(sender);
            
            case "start":
                if (!sender.hasPermission("mmorpg.admin.events")) {
                    sender.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUso: §7/event start <id> <mundo>");
                    return true;
                }
                return handleStart(sender, args[1], args[2]);
            
            case "stop":
                if (!sender.hasPermission("mmorpg.admin.events")) {
                    sender.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUso: §7/event stop <id>");
                    return true;
                }
                return handleStop(sender, args[1]);
            
            case "reload":
                if (!sender.hasPermission("mmorpg.admin.events")) {
                    sender.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return handleReload(sender);
            
            case "stats":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: §7/event stats <id>");
                    return true;
                }
                return handleStats(sender, args[1]);
            
            case "currency":
                if (args.length < 2) {
                    if (sender instanceof Player) {
                        return handleCurrencyView(sender, ((Player) sender).getName());
                    } else {
                        sender.sendMessage("§cUso: §7/event currency <jugador>");
                        return true;
                    }
                }
                
                if (args.length == 2) {
                    return handleCurrencyView(sender, args[1]);
                } else if (args.length == 4) {
                    if (!sender.hasPermission("mmorpg.admin.events")) {
                        sender.sendMessage("§cNo tienes permiso para modificar monedas.");
                        return true;
                    }
                    return handleCurrencyModify(sender, args[1], args[2], args[3]);
                } else {
                    sender.sendMessage("§cUso: §7/event currency <jugador> [add/set] [cantidad]");
                    return true;
                }
            
            case "leaderboard":
            case "lb":
                if (args.length < 2) {
                    sender.sendMessage("§cUso: §7/event leaderboard <id>");
                    return true;
                }
                return handleLeaderboard(sender, args[1]);
            
            case "validate":
                if (!sender.hasPermission("mmorpg.admin.events")) {
                    sender.sendMessage("§cNo tienes permiso para usar este comando.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUso: §7/event validate <id>");
                    return true;
                }
                return handleValidate(sender, args[1]);
            
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§lCOMAN DOS DE EVENTOS MMORPG");
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7/event §elist §7- Listar todos los eventos");
        sender.sendMessage("§7/event §einfo <id> §7- Información detallada");
        sender.sendMessage("§7/event §eactive §7- Ver eventos activos");
        sender.sendMessage("§7/event §estats <id> §7- Estadísticas del evento");
        sender.sendMessage("§7/event §ecurrency [jugador] §7- Ver monedas");
        sender.sendMessage("§7/event §eleaderboard <id> §7- Top jugadores");
        
        if (sender.hasPermission("mmorpg.admin.events")) {
            sender.sendMessage("§c§lADMIN:");
            sender.sendMessage("§7/event §cstart <id> <mundo> §7- Iniciar evento");
            sender.sendMessage("§7/event §cstop <id> §7- Detener evento");
            sender.sendMessage("§7/event §creload §7- Recargar configuración");
            sender.sendMessage("§7/event §cvalidate <id> §7- Validar mobs del evento");
            sender.sendMessage("§7/event §ccurrency <jugador> add <n>");
            sender.sendMessage("§7/event §ccurrency <jugador> set <n>");
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
    }
    
    private boolean handleList(CommandSender sender) {
        Map<String, EventConfig> events = eventManager.getEventConfigs();
        
        if (events.isEmpty()) {
            sender.sendMessage("§cNo hay eventos configurados.");
            return true;
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§lEVENTOS CONFIGURADOS §7(" + events.size() + ")");
        sender.sendMessage("§6§l═══════════════════════════════════");
        
        LocalDateTime now = LocalDateTime.now();
        
        for (EventConfig config : events.values()) {
            String status;
            if (!config.isEnabled()) {
                status = "§8[DESHABILITADO]";
            } else if (eventManager.getActiveSessions().containsKey(config.getId())) {
                status = "§a§l[ACTIVO]";
            } else if (now.isBefore(config.getStartDate())) {
                status = "§e[PRÓXIMO]";
            } else if (now.isAfter(config.getEndDate())) {
                status = "§7[FINALIZADO]";
            } else {
                status = "§b[EN PERIODO]";
            }
            
            sender.sendMessage("§7• " + status + " §e" + config.getName() + " §8(" + config.getId() + ")");
            sender.sendMessage("  §7Fechas: §f" + config.getStartDate().format(FORMATTER) + 
                " §7→ §f" + config.getEndDate().format(FORMATTER));
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7Usa §e/event info <id> §7para más detalles");
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String eventId) {
        EventConfig config = eventManager.getEventConfigs().get(eventId);
        
        if (config == null) {
            sender.sendMessage("§cEvento §e" + eventId + " §cno encontrado.");
            return true;
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§l" + config.getName());
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7ID: §f" + config.getId());
        sender.sendMessage("§7Descripción: §f" + config.getDescription());
        sender.sendMessage("§7Estado: " + (config.isEnabled() ? "§aHabilitado" : "§cDeshabilitado"));
        sender.sendMessage("§7Auto-start: " + (config.isAutoStart() ? "§aSí" : "§cNo"));
        sender.sendMessage("");
        sender.sendMessage("§e§lPERIODO:");
        sender.sendMessage("§7Inicio: §f" + config.getStartDate().format(FORMATTER));
        sender.sendMessage("§7Fin: §f" + config.getEndDate().format(FORMATTER));
        sender.sendMessage("");
        sender.sendMessage("§e§lMOBS §7(" + config.getCustomMobs().size() + "):");
        for (EventConfig.EventMob mob : config.getCustomMobs()) {
            sender.sendMessage("§7• §f" + mob.getMobId() + " §7(chance: §e" + (mob.getSpawnChance() * 100) + "%§7)");
        }
        sender.sendMessage("");
        sender.sendMessage("§e§lDROPS EXCLUSIVOS §7(" + config.getExclusiveDrops().size() + "):");
        for (EventConfig.EventDrop drop : config.getExclusiveDrops()) {
            sender.sendMessage("§7• §f" + drop.getItemName() + " §7x§e" + drop.getMinAmount() + "-" + drop.getMaxAmount() + 
                " §7(§e" + (drop.getDropChance() * 100) + "%§7)");
        }
        
        if (config.getRewards() != null) {
            sender.sendMessage("");
            sender.sendMessage("§e§lRECOMPENSAS:");
            sender.sendMessage("§7XP Multiplier: §e" + config.getRewards().getBonusXPMultiplier() + "x");
            sender.sendMessage("§7Monedas por kill: §e" + config.getRewards().getEventCoinsPerKill());
            sender.sendMessage("§7Monedas por boss: §e" + config.getRewards().getBossEventCoins());
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        return true;
    }
    
    private boolean handleActive(CommandSender sender) {
        Map<String, EventSession> active = eventManager.getActiveSessions();
        
        if (active.isEmpty()) {
            sender.sendMessage("§cNo hay eventos activos en este momento.");
            return true;
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§a§lEVENTOS ACTIVOS §7(" + active.size() + ")");
        sender.sendMessage("§6§l═══════════════════════════════════");
        
        for (EventSession session : active.values()) {
            sender.sendMessage("§7• §e" + session.getEventName() + " §7en mundo §f" + session.getWorldName());
            sender.sendMessage("  §7Iniciado: §f" + session.getStartedAt().format(FORMATTER));
            sender.sendMessage("  §7Participantes: §e" + session.getParticipants().size());
            sender.sendMessage("  §7Kills totales: §c" + session.getTotalKills());
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        return true;
    }
    
    private boolean handleStart(CommandSender sender, String eventId, String worldName) {
        if (eventManager.getActiveSessions().containsKey(eventId)) {
            sender.sendMessage("§cEl evento §e" + eventId + " §cya está activo.");
            return true;
        }
        
        EventConfig config = eventManager.getEventConfigs().get(eventId);
        if (config == null) {
            sender.sendMessage("§cEvento §e" + eventId + " §cno encontrado.");
            return true;
        }
        
        if (Bukkit.getWorld(worldName) == null) {
            sender.sendMessage("§cMundo §e" + worldName + " §cno encontrado.");
            return true;
        }
        
        boolean success = eventManager.startEvent(eventId, worldName);
        
        if (success) {
            sender.sendMessage("§a✓ Evento §e" + config.getName() + " §ainiciado en mundo §f" + worldName);
        } else {
            sender.sendMessage("§cError al iniciar el evento. Ver consola para detalles.");
        }
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender, String eventId) {
        if (!eventManager.getActiveSessions().containsKey(eventId)) {
            sender.sendMessage("§cEl evento §e" + eventId + " §cno está activo.");
            return true;
        }
        
        boolean success = eventManager.stopEvent(eventId);
        
        if (success) {
            sender.sendMessage("§a✓ Evento §e" + eventId + " §adetener correctamente.");
        } else {
            sender.sendMessage("§cError al detener el evento.");
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        sender.sendMessage("§eRecargando configuración de eventos...");
        
        try {
            eventManager.loadConfig();
            sender.sendMessage("§a✓ Configuración de eventos recargada exitosamente.");
            sender.sendMessage("§7Eventos cargados: §e" + eventManager.getEventConfigs().size());
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error al recargar: §7" + e.getMessage());
            plugin.getLogger().severe("Error al recargar eventos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    private boolean handleStats(CommandSender sender, String eventId) {
        EventConfig config = eventManager.getEventConfigs().get(eventId);
        
        if (config == null) {
            sender.sendMessage("§cEvento §e" + eventId + " §cno encontrado.");
            return true;
        }
        
        List<Map<String, Object>> history = eventManager.getEventHistory(10);
        List<Map<String, Object>> eventHistory = history.stream()
            .filter(h -> h.get("event_id").equals(eventId))
            .collect(Collectors.toList());
        
        if (eventHistory.isEmpty()) {
            sender.sendMessage("§cNo hay historial para el evento §e" + config.getName());
            return true;
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§lESTADÍSTICAS: " + config.getName());
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7Activaciones totales: §e" + eventHistory.size());
        
        int totalParticipants = 0;
        int totalKills = 0;
        
        for (Map<String, Object> record : eventHistory) {
            totalParticipants += (int) record.get("participants");
            totalKills += (int) record.get("total_kills");
        }
        
        sender.sendMessage("§7Participantes totales: §e" + totalParticipants);
        sender.sendMessage("§7Kills totales: §c" + totalKills);
        sender.sendMessage("§7Promedio kills/evento: §e" + (eventHistory.isEmpty() ? 0 : totalKills / eventHistory.size()));
        sender.sendMessage("");
        sender.sendMessage("§e§lÚLTIMAS 5 ACTIVACIONES:");
        
        int count = 0;
        for (Map<String, Object> record : eventHistory) {
            if (count++ >= 5) break;
            
            sender.sendMessage("§7• §f" + record.get("started_at") + " §7→ " + record.get("ended_at"));
            sender.sendMessage("  §7Participantes: §e" + record.get("participants") + " §7| Kills: §c" + record.get("total_kills"));
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        return true;
    }
    
    private boolean handleCurrencyView(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        UUID playerId = null;
        
        if (target != null) {
            playerId = target.getUniqueId();
        } else {
            // Buscar en BD por nombre (nota: esto es menos eficiente)
            sender.sendMessage("§eJugador offline, buscando en base de datos...");
            // Por simplicidad, requiere que esté online
            sender.sendMessage("§cEl jugador debe estar online para consultar monedas.");
            return true;
        }
        
        int coins = eventManager.getEventCurrency(playerId);
        List<Map<String, Object>> history = eventManager.getCurrencyHistory(playerId, 5);
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§lMONEDAS DE EVENTO: §f" + playerName);
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7Monedas actuales: §e" + coins + " §7monedas");
        sender.sendMessage("");
        sender.sendMessage("§e§lÚLTIMAS TRANSACCIONES:");
        
        if (history.isEmpty()) {
            sender.sendMessage("§7Sin transacciones recientes.");
        } else {
            for (Map<String, Object> entry : history) {
                String type = (String) entry.get("transaction_type");
                int amount = (int) entry.get("amount");
                String desc = (String) entry.get("description");
                
                String prefix = type.equals("EARN") ? "§a+" : "§c-";
                sender.sendMessage("§7• " + prefix + Math.abs(amount) + " §7- §f" + desc);
            }
        }
        
        sender.sendMessage("§6§l═══════════════════════════════════");
        return true;
    }
    
    private boolean handleCurrencyModify(CommandSender sender, String playerName, String operation, String amountStr) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cEl jugador §e" + playerName + " §cdebe estar online.");
            return true;
        }
        
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cCantidad inválida: §e" + amountStr);
            return true;
        }
        
        UUID playerId = target.getUniqueId();
        
        if (operation.equalsIgnoreCase("add")) {
            eventManager.addEventCurrency(playerId, amount, "ADMIN", "Admin: " + sender.getName());
            sender.sendMessage("§a✓ Añadidas §e" + amount + " §amonedas a §f" + playerName);
            target.sendMessage("§a§l[EVENTO] §7Has recibido §e" + amount + " §7monedas de evento por un administrador.");
        } else if (operation.equalsIgnoreCase("set")) {
            int current = eventManager.getEventCurrency(playerId);
            int difference = amount - current;
            
            if (difference > 0) {
                eventManager.addEventCurrency(playerId, difference, "ADMIN", "Admin set: " + sender.getName());
            } else if (difference < 0) {
                eventManager.spendEventCurrency(playerId, Math.abs(difference), "Admin set: " + sender.getName());
            }
            
            sender.sendMessage("§a✓ Monedas de §f" + playerName + " §aestablecidas a §e" + amount);
            target.sendMessage("§e§l[EVENTO] §7Tus monedas han sido ajustadas a §e" + amount + " §7por un administrador.");
        } else {
            sender.sendMessage("§cOperación inválida. Usa: §eadd §co §eset");
            return true;
        }
        
        return true;
    }
    
    private boolean handleLeaderboard(CommandSender sender, String eventId) {
        EventConfig config = eventManager.getEventConfigs().get(eventId);
        
        if (config == null) {
            sender.sendMessage("§cEvento §e" + eventId + " §cno encontrado.");
            return true;
        }
        
        // Obtener top 10 jugadores por kills en este evento
        // (Esto requeriría una query SQL adicional, por simplicidad mostramos mensaje)
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§e§lTOP JUGADORES: " + config.getName());
        sender.sendMessage("§6§l═══════════════════════════════════");
        sender.sendMessage("§7Funcionalidad de leaderboard en desarrollo");
        sender.sendMessage("§7Consulta la web panel para estadísticas detalladas");
        sender.sendMessage("§6§l═══════════════════════════════════");
        
        return true;
    }
    
    private boolean handleValidate(CommandSender sender, String eventId) {
        EventConfig config = eventManager.getEventConfigs().get(eventId);
        
        if (config == null) {
            sender.sendMessage("§cEvento §e" + eventId + " §cno encontrado.");
            return true;
        }
        
        sender.sendMessage("§eValidando mobs del evento §f" + config.getName() + "§e...");
        
        List<String> missingMobs = eventManager.validateEventMobs(eventId, mobManager);
        
        if (missingMobs.isEmpty()) {
            sender.sendMessage("§a✓ Todos los mobs del evento existen en la configuración.");
        } else {
            sender.sendMessage("§c✗ Los siguientes mobs NO existen en mobs_config.json:");
            for (String mobId : missingMobs) {
                sender.sendMessage("  §7• §c" + mobId);
            }
            sender.sendMessage("");
            sender.sendMessage("§eAgrega estos mobs a mobs_config.json o edita el evento.");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "info", "active", "stats", "currency", "leaderboard", "validate");
            
            if (sender.hasPermission("mmorpg.admin.events")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.addAll(Arrays.asList("start", "stop", "reload"));
            }
            
            return subCommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("info") || subCommand.equals("start") || subCommand.equals("stop") || 
                subCommand.equals("stats") || subCommand.equals("leaderboard") || subCommand.equals("validate")) {
                return eventManager.getEventConfigs().keySet().stream()
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (subCommand.equals("currency")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("start")) {
                return Bukkit.getWorlds().stream()
                    .map(world -> world.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (subCommand.equals("currency") && sender.hasPermission("mmorpg.admin.events")) {
                return Arrays.asList("add", "set").stream()
                    .filter(op -> op.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
