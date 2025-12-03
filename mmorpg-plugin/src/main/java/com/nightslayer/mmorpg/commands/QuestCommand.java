package com.nightslayer.mmorpg.commands;

import com.nightslayer.mmorpg.quests.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando para gestionar quests
 * Uso: /quest <list|active|accept|progress|complete|info>
 */
public class QuestCommand implements CommandExecutor, TabCompleter {
    private final QuestManager questManager;
    
    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
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
                listAvailableQuests(player);
                break;
                
            case "active":
                listActiveQuests(player);
                break;
                
            case "completed":
                listCompletedQuests(player);
                break;
                
            case "accept":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /quest accept <id>");
                    return true;
                }
                acceptQuest(player, args[1]);
                break;
                
            case "progress":
                if (args.length < 2) {
                    showAllProgress(player);
                } else {
                    showQuestProgress(player, args[1]);
                }
                break;
                
            case "complete":
            case "claim":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /quest complete <id>");
                    return true;
                }
                claimQuest(player, args[1]);
                break;
                
            case "info":
                if (args.length < 2) {
                    player.sendMessage("§cUso: /quest info <id>");
                    return true;
                }
                showQuestInfo(player, args[1]);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l=== Comandos de Quests ===");
        player.sendMessage("§e/quest list §7- Ver quests disponibles");
        player.sendMessage("§e/quest active §7- Ver tus quests activas");
        player.sendMessage("§e/quest completed §7- Ver quests completadas");
        player.sendMessage("§e/quest accept <id> §7- Aceptar una quest");
        player.sendMessage("§e/quest progress [id] §7- Ver progreso");
        player.sendMessage("§e/quest complete <id> §7- Reclamar recompensas");
        player.sendMessage("§e/quest info <id> §7- Ver información");
    }
    
    private void listAvailableQuests(Player player) {
        List<Quest> available = questManager.getAvailableQuests(player);
        
        if (available.isEmpty()) {
            player.sendMessage("§7No hay quests disponibles en este momento");
            return;
        }
        
        player.sendMessage("§6§l=== Quests Disponibles ===");
        
        for (Quest quest : available) {
            player.sendMessage("");
            player.sendMessage(quest.getDifficulty().getDisplay() + " §e" + quest.getName() + " §7(ID: " + quest.getId() + ")");
            player.sendMessage("§7" + quest.getDescription());
            player.sendMessage("§fNivel requerido: §e" + quest.getRequiredLevel());
            player.sendMessage("§aUsa §e/quest accept " + quest.getId() + " §apara aceptar");
        }
    }
    
    private void listActiveQuests(Player player) {
        List<Quest> active = questManager.getActiveQuests(player);
        
        if (active.isEmpty()) {
            player.sendMessage("§7No tienes quests activas");
            player.sendMessage("§7Usa §e/quest list §7para ver quests disponibles");
            return;
        }
        
        player.sendMessage("§6§l=== Quests Activas ===");
        
        for (Quest quest : active) {
            PlayerQuestProgress progress = questManager.getQuestProgress(player, quest.getId());
            
            player.sendMessage("");
            player.sendMessage("§e" + quest.getName() + " " + quest.getDifficulty().getDisplay());
            
            for (QuestObjective objective : quest.getObjectives()) {
                int current = progress.getObjectiveProgress(objective.getId());
                int required = objective.getRequiredAmount();
                boolean completed = current >= required;
                
                String status = completed ? "§a✓" : "§7◯";
                player.sendMessage(status + " §f" + objective.getDescription() + 
                                 " §7(" + current + "/" + required + ")");
            }
            
            if (progress.areAllObjectivesCompleted(quest)) {
                player.sendMessage("§a¡Quest completada! Usa §e/quest complete " + quest.getId());
            }
        }
    }
    
    private void listCompletedQuests(Player player) {
        List<Quest> completed = questManager.getCompletedQuests(player);
        
        if (completed.isEmpty()) {
            player.sendMessage("§7No has completado ninguna quest todavía");
            return;
        }
        
        player.sendMessage("§6§l=== Quests Completadas ===");
        
        for (Quest quest : completed) {
            PlayerQuestProgress progress = questManager.getQuestProgress(player, quest.getId());
            String status = progress.getStatus() == PlayerQuestProgress.QuestStatus.COMPLETED ? 
                          "§e¡Pendiente de reclamar!" : "§a✓ Reclamada";
            
            player.sendMessage("§e" + quest.getName() + " " + quest.getDifficulty().getDisplay() + " " + status);
        }
    }
    
    private void acceptQuest(Player player, String questId) {
        questManager.acceptQuest(player, questId);
    }
    
    private void showAllProgress(Player player) {
        List<Quest> active = questManager.getActiveQuests(player);
        
        if (active.isEmpty()) {
            player.sendMessage("§7No tienes quests activas");
            return;
        }
        
        listActiveQuests(player);
    }
    
    private void showQuestProgress(Player player, String questId) {
        Quest quest = questManager.getQuest(questId);
        
        if (quest == null) {
            player.sendMessage("§cQuest no encontrada");
            return;
        }
        
        PlayerQuestProgress progress = questManager.getQuestProgress(player, questId);
        
        player.sendMessage("§6§l=== " + quest.getName() + " ===");
        player.sendMessage("§7" + quest.getDescription());
        player.sendMessage("");
        player.sendMessage("§6Objetivos:");
        
        for (QuestObjective objective : quest.getObjectives()) {
            int current = progress.getObjectiveProgress(objective.getId());
            int required = objective.getRequiredAmount();
            boolean completed = current >= required;
            
            String status = completed ? "§a✓ Completado" : "§7En progreso";
            player.sendMessage(status + " §f" + objective.getDescription());
            player.sendMessage("  §7Progreso: " + current + "/" + required);
        }
        
        player.sendMessage("");
        player.sendMessage("§6Recompensas:");
        for (QuestReward reward : quest.getRewards()) {
            player.sendMessage("  " + reward.getDescription());
        }
    }
    
    private void claimQuest(Player player, String questId) {
        questManager.claimQuest(player, questId);
    }
    
    private void showQuestInfo(Player player, String questId) {
        Quest quest = questManager.getQuest(questId);
        
        if (quest == null) {
            player.sendMessage("§cQuest no encontrada");
            return;
        }
        
        player.sendMessage("§6§l=== " + quest.getName() + " ===");
        player.sendMessage(quest.getDifficulty().getDisplay());
        player.sendMessage("§7" + quest.getDescription());
        player.sendMessage("");
        player.sendMessage("§fNivel requerido: §e" + quest.getRequiredLevel());
        player.sendMessage("§fRepetible: " + (quest.isRepeatable() ? "§aSí" : "§cNo"));
        player.sendMessage("");
        player.sendMessage("§6Objetivos:");
        
        for (QuestObjective objective : quest.getObjectives()) {
            player.sendMessage("  §f" + objective.getDescription());
        }
        
        player.sendMessage("");
        player.sendMessage("§6Recompensas:");
        for (QuestReward reward : quest.getRewards()) {
            player.sendMessage("  " + reward.getDescription());
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "active", "completed", "accept", "progress", "complete", "info"));
        } else if (args.length == 2 && sender instanceof Player) {
            Player player = (Player) sender;
            
            if (args[0].equalsIgnoreCase("accept")) {
                for (Quest quest : questManager.getAvailableQuests(player)) {
                    completions.add(quest.getId());
                }
            } else if (args[0].equalsIgnoreCase("progress") || 
                      args[0].equalsIgnoreCase("complete") || 
                      args[0].equalsIgnoreCase("info")) {
                for (Quest quest : questManager.getAllQuests()) {
                    completions.add(quest.getId());
                }
            }
        }
        
        return completions;
    }
}
