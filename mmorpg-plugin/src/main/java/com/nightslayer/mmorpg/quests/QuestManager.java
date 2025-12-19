package com.nightslayer.mmorpg.quests;

import com.nightslayer.mmorpg.RPGPathResolver;
import com.nightslayer.mmorpg.MMORPGPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nightslayer.mmorpg.classes.ClassManager;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor del sistema de quests
 * Lee desde: worlds/{worldName}/data/quests.json (local por mundo)
 * Nota: El progreso de jugadores se mantiene en archivos individuales por compatibilidad
 */
public class QuestManager {
    private final MMORPGPlugin plugin;
    private final RPGPathResolver pathResolver;
    private final ClassManager classManager;
    private final Map<String, Quest> quests;
    private final Map<UUID, Map<String, PlayerQuestProgress>> playerProgress; // UUID -> QuestID -> Progress
    private final Gson gson;
    private final File playerProgressFolder; // Mantener carpeta de progreso de jugadores
    
    public QuestManager(MMORPGPlugin plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.pathResolver = plugin.getWorldRPGManager().getPathResolver();
        this.classManager = classManager;
        this.quests = new HashMap<>();
        this.playerProgress = new HashMap<>();
        this.gson = new Gson();
        // Progreso de jugadores se mantiene en carpeta separada
        this.playerProgressFolder = new File(plugin.getDataFolder(), "quest-progress");
        
        if (!playerProgressFolder.exists()) {
            playerProgressFolder.mkdirs();
        }
        
        createDefaultQuests();
    }
    
    /**
     * Crea quests por defecto para demostración
     */
    private void createDefaultQuests() {
        // Quest de inicio
        Quest welcomeQuest = new Quest(
            "welcome_quest",
            "Bienvenido al Mundo RPG",
            "Completa tu primera misión para comenzar tu aventura",
            1,
            "quest_master_1",
            Quest.QuestDifficulty.EASY
        );
        welcomeQuest.addObjective(new QuestObjective("talk_1", QuestObjectiveType.TALK, "Maestro de Misiones", 1));
        welcomeQuest.addReward(new QuestReward(QuestReward.RewardType.EXPERIENCE, 50));
        welcomeQuest.addReward(new QuestReward(QuestReward.RewardType.ITEM, 10, "BREAD"));
        registerQuest(welcomeQuest);
        
        // Quest de caza
        Quest huntQuest = new Quest(
            "hunt_zombies",
            "Cazador de No-Muertos",
            "Elimina zombies para proteger la aldea",
            3,
            "quest_master_1",
            Quest.QuestDifficulty.NORMAL
        );
        huntQuest.addObjective(new QuestObjective("kill_zombies", QuestObjectiveType.KILL, "Zombies", 10));
        huntQuest.addReward(new QuestReward(QuestReward.RewardType.EXPERIENCE, 100));
        huntQuest.addReward(new QuestReward(QuestReward.RewardType.MONEY, 50));
        huntQuest.setRepeatable(true);
        huntQuest.setCooldownTime(3600000); // 1 hora
        registerQuest(huntQuest);
        
        // Quest de recolección
        Quest gatherQuest = new Quest(
            "gather_resources",
            "Recolector Experto",
            "Reúne recursos para la comunidad",
            2,
            "quest_master_1",
            Quest.QuestDifficulty.EASY
        );
        gatherQuest.addObjective(new QuestObjective("collect_wood", QuestObjectiveType.COLLECT, "Madera", 64));
        gatherQuest.addObjective(new QuestObjective("collect_stone", QuestObjectiveType.COLLECT, "Piedra", 64));
        gatherQuest.addReward(new QuestReward(QuestReward.RewardType.EXPERIENCE, 75));
        gatherQuest.addReward(new QuestReward(QuestReward.RewardType.ITEM, 1, "IRON_PICKAXE"));
        registerQuest(gatherQuest);
        
        // Quest épica
        Quest epicQuest = new Quest(
            "dragon_slayer",
            "Asesino de Dragones",
            "Derrota al poderoso Dragón del Fin",
            15,
            "quest_master_epic",
            Quest.QuestDifficulty.EPIC
        );
        epicQuest.addObjective(new QuestObjective("kill_dragon", QuestObjectiveType.KILL, "Ender Dragon", 1));
        epicQuest.addReward(new QuestReward(QuestReward.RewardType.EXPERIENCE, 1000));
        epicQuest.addReward(new QuestReward(QuestReward.RewardType.MONEY, 500));
        epicQuest.addReward(new QuestReward(QuestReward.RewardType.CLASS_SKILL_POINT, 5));
        registerQuest(epicQuest);
    }
    
    /**
     * Registra una quest
     */
    public void registerQuest(Quest quest) {
        quests.put(quest.getId(), quest);
    }
    
    /**
     * Elimina una quest registrada
     */
    public void unregisterQuest(String questId) {
        quests.remove(questId);
    }
    
    /**
     * Obtiene una quest por ID
     */
    public Quest getQuest(String questId) {
        return quests.get(questId);
    }
    
    /**
     * Obtiene todas las quests
     */
    public Collection<Quest> getAllQuests() {
        return quests.values();
    }
    
    /**
     * Obtiene las quests disponibles para un jugador
     */
    public List<Quest> getAvailableQuests(Player player) {
        int playerLevel = classManager.getPlayerClass(player).getLevel();
        
        return quests.values().stream()
            .filter(quest -> {
                PlayerQuestProgress progress = getQuestProgress(player, quest.getId());
                return playerLevel >= quest.getRequiredLevel() && progress.isAvailable();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las quests activas de un jugador
     */
    public List<Quest> getActiveQuests(Player player) {
        Map<String, PlayerQuestProgress> playerQuests = playerProgress.get(player.getUniqueId());
        
        if (playerQuests == null) {
            return new ArrayList<>();
        }
        
        return playerQuests.values().stream()
            .filter(progress -> progress.getStatus() == PlayerQuestProgress.QuestStatus.IN_PROGRESS)
            .map(progress -> quests.get(progress.getQuestId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las quests completadas de un jugador
     */
    public List<Quest> getCompletedQuests(Player player) {
        Map<String, PlayerQuestProgress> playerQuests = playerProgress.get(player.getUniqueId());
        
        if (playerQuests == null) {
            return new ArrayList<>();
        }
        
        return playerQuests.values().stream()
            .filter(progress -> progress.getStatus() == PlayerQuestProgress.QuestStatus.COMPLETED ||
                               progress.getStatus() == PlayerQuestProgress.QuestStatus.CLAIMED)
            .map(progress -> quests.get(progress.getQuestId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene el progreso de una quest para un jugador
     */
    public PlayerQuestProgress getQuestProgress(Player player, String questId) {
        return getQuestProgress(player.getUniqueId(), questId);
    }
    
    /**
     * Obtiene el progreso de una quest por UUID
     */
    public PlayerQuestProgress getQuestProgress(UUID playerUUID, String questId) {
        Map<String, PlayerQuestProgress> playerQuests = playerProgress.computeIfAbsent(
            playerUUID, k -> new HashMap<>()
        );
        
        return playerQuests.computeIfAbsent(questId, k -> {
            PlayerQuestProgress progress = loadQuestProgress(playerUUID, questId);
            if (progress == null) {
                progress = new PlayerQuestProgress(playerUUID, questId);
            }
            return progress;
        });
    }
    
    /**
     * Acepta una quest
     */
    public boolean acceptQuest(Player player, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            player.sendMessage("§cQuest no encontrada");
            return false;
        }
        
        int playerLevel = classManager.getPlayerClass(player).getLevel();
        if (playerLevel < quest.getRequiredLevel()) {
            player.sendMessage("§cNecesitas nivel " + quest.getRequiredLevel() + " para esta quest");
            return false;
        }
        
        PlayerQuestProgress progress = getQuestProgress(player, questId);
        
        if (!progress.isAvailable()) {
            if (progress.getStatus() == PlayerQuestProgress.QuestStatus.IN_PROGRESS) {
                player.sendMessage("§cYa tienes esta quest activa");
            } else if (progress.getStatus() == PlayerQuestProgress.QuestStatus.ON_COOLDOWN) {
                long remaining = progress.getRemainingCooldown();
                player.sendMessage("§cQuest en cooldown. Disponible en " + formatTime(remaining));
            } else {
                player.sendMessage("§cNo puedes aceptar esta quest en este momento");
            }
            return false;
        }
        
        progress.start();
        saveQuestProgress(progress);
        
        player.sendMessage("§a¡Quest aceptada: §e" + quest.getName());
        player.sendMessage("§7" + quest.getDescription());
        
        return true;
    }
    
    /**
     * Actualiza el progreso de un objetivo
     */
    public void updateObjective(Player player, String questId, String objectiveId, int amount) {
        Quest quest = getQuest(questId);
        if (quest == null) return;
        
        PlayerQuestProgress progress = getQuestProgress(player, questId);
        if (progress.getStatus() != PlayerQuestProgress.QuestStatus.IN_PROGRESS) {
            return;
        }
        
        QuestObjective objective = quest.getObjectives().stream()
            .filter(obj -> obj.getId().equals(objectiveId))
            .findFirst()
            .orElse(null);
        
        if (objective == null) return;
        
        int oldProgress = progress.getObjectiveProgress(objectiveId);
        progress.incrementObjectiveProgress(objectiveId, amount);
        int newProgress = progress.getObjectiveProgress(objectiveId);
        
        // Limitar al máximo requerido
        if (newProgress > objective.getRequiredAmount()) {
            newProgress = objective.getRequiredAmount();
            progress.setObjectiveProgress(objectiveId, newProgress);
        }
        
        // Notificar al jugador
        if (oldProgress < objective.getRequiredAmount() && newProgress >= objective.getRequiredAmount()) {
            player.sendMessage("§a✓ Objetivo completado: §f" + objective.getDescription());
        } else {
            player.sendMessage("§7Progreso: §e" + objective.getDescription() + 
                             " §7(" + newProgress + "/" + objective.getRequiredAmount() + ")");
        }
        
        // Verificar si la quest está completa
        if (progress.areAllObjectivesCompleted(quest)) {
            progress.complete();
            player.sendMessage("");
            player.sendMessage("§6§l¡QUEST COMPLETADA!");
            player.sendMessage("§e" + quest.getName());
            player.sendMessage("§aHabla con el NPC para reclamar tu recompensa");
            player.sendMessage("");
        }
        
        saveQuestProgress(progress);
    }
    
    /**
     * Completa y reclama recompensas de una quest
     */
    public boolean claimQuest(Player player, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) {
            player.sendMessage("§cQuest no encontrada");
            return false;
        }
        
        PlayerQuestProgress progress = getQuestProgress(player, questId);
        
        if (progress.getStatus() != PlayerQuestProgress.QuestStatus.COMPLETED) {
            player.sendMessage("§cEsta quest no está completada todavía");
            return false;
        }
        
        // Dar recompensas
        player.sendMessage("§6§l=== RECOMPENSAS ===");
        
        for (QuestReward reward : quest.getRewards()) {
            giveReward(player, reward, quest.getDifficulty().getRewardMultiplier());
            player.sendMessage("  " + reward.getDescription());
        }
        
        progress.claim(quest);
        saveQuestProgress(progress);
        
        player.sendMessage("§a¡Quest reclamada!");
        
        return true;
    }
    
    /**
     * Otorga una recompensa al jugador
     */
    private void giveReward(Player player, QuestReward reward, double multiplier) {
        int amount = (int) (reward.getAmount() * multiplier);
        
        switch (reward.getType()) {
            case EXPERIENCE:
                classManager.getPlayerClass(player).addExperience(amount);
                break;
            case MONEY:
                // Aquí se integrará con sistema de economía
                player.sendMessage("§7(Sistema de economía pendiente)");
                break;
            case ITEM:
                if (reward.createItemStack() != null) {
                    player.getInventory().addItem(reward.createItemStack());
                }
                break;
            case CLASS_SKILL_POINT:
                // Aquí se implementarán puntos de habilidad
                player.sendMessage("§7(Puntos de habilidad pendiente)");
                break;
        }
    }
    
    /**
     * Formatea un tiempo en segundos a formato legible
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " segundos";
        } else if (seconds < 3600) {
            return (seconds / 60) + " minutos";
        } else {
            return (seconds / 3600) + " horas";
        }
    }
    
    /**
     * Carga el progreso de una quest
     */
    private PlayerQuestProgress loadQuestProgress(UUID playerUUID, String questId) {
        File file = new File(playerProgressFolder, playerUUID.toString() + "_" + questId + ".json");
        
        if (!file.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            return PlayerQuestProgress.fromJson(json);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar progreso de quest: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Guarda el progreso de una quest
     */
    public void saveQuestProgress(PlayerQuestProgress progress) {
        File file = new File(playerProgressFolder, progress.getPlayerUUID().toString() + "_" + progress.getQuestId() + ".json");
        
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(progress.toJson(), writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al guardar progreso de quest: " + e.getMessage());
        }
    }
    
    /**
     * Guarda todo el progreso de quests
     */
    public void saveAll() {
        for (Map<String, PlayerQuestProgress> playerQuests : playerProgress.values()) {
            for (PlayerQuestProgress progress : playerQuests.values()) {
                saveQuestProgress(progress);
            }
        }
    }
    
    /**
     * Guarda definiciones de quests para un mundo específico
     * Guarda en: worlds/{worldName}/data/quests.json
     */
    public void saveWorldQuests(String worldName) {
        File file = pathResolver.getLocalFile(worldName, "quests.json");
        
        try (FileWriter writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            
            for (Quest quest : quests.values()) {
                json.add(quest.getId(), quest.toJson());
            }
            
            gson.toJson(json, writer);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al guardar quests para mundo " + worldName + ": " + e.getMessage());
        }
    }
    
    /**
     * Carga quests de un mundo específico
     * Lee desde: worlds/{worldName}/data/quests.json
     */
    public void loadWorldQuests(String worldName) {
        File file = pathResolver.getLocalFile(worldName, "quests.json");
        
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json != null) {
                for (String key : json.keySet()) {
                    // Implementar deserialización de Quests desde JSON
                    plugin.getLogger().info("Cargada quest: " + key + " del mundo " + worldName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al cargar quests para mundo " + worldName + ": " + e.getMessage());
        }
    }
}
