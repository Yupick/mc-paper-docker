package com.nightslayer.mmorpg.audit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sistema de auditoría para registrar todas las acciones RPG
 */
public class AuditLogger {
    private final Plugin plugin;
    private final File auditDir;
    private final Gson gson;
    private final Queue<AuditEntry> pendingEntries;
    private final DateTimeFormatter dateFormatter;
    private final DateTimeFormatter filenameFormatter;
    
    public AuditLogger(Plugin plugin) {
        this.plugin = plugin;
        this.auditDir = new File(plugin.getDataFolder(), "audit");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.pendingEntries = new ConcurrentLinkedQueue<>();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.filenameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        if (!auditDir.exists()) {
            auditDir.mkdirs();
        }
        
        // Iniciar tarea de guardado periódico
        startAutoSave();
    }
    
    /**
     * Registra una acción de clase
     */
    public void logClassAction(String playerName, String action, String details) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.CLASS,
            playerName,
            action,
            details
        );
        pendingEntries.add(entry);
    }
    
    /**
     * Registra una acción de quest
     */
    public void logQuestAction(String playerName, String questId, String action, String details) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.QUEST,
            playerName,
            action,
            String.format("Quest: %s | %s", questId, details)
        );
        pendingEntries.add(entry);
    }
    
    /**
     * Registra una transacción económica
     */
    public void logEconomyAction(String playerName, String type, double amount, String reason) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.ECONOMY,
            playerName,
            type,
            String.format("Amount: %.2f | Reason: %s", amount, reason)
        );
        pendingEntries.add(entry);
    }
    
    /**
     * Registra interacción con NPC
     */
    public void logNPCInteraction(String playerName, String npcId, String action) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.NPC,
            playerName,
            action,
            "NPC: " + npcId
        );
        pendingEntries.add(entry);
    }
    
    /**
     * Registra acción administrativa
     */
    public void logAdminAction(String adminName, String action, String target, String details) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.ADMIN,
            adminName,
            action,
            String.format("Target: %s | %s", target, details)
        );
        entry.setSeverity(AuditSeverity.HIGH);
        pendingEntries.add(entry);
    }
    
    /**
     * Registra un error o warning
     */
    public void logError(String source, String error, String stackTrace) {
        AuditEntry entry = new AuditEntry(
            AuditCategory.ERROR,
            "SYSTEM",
            source,
            String.format("Error: %s\nStack: %s", error, stackTrace)
        );
        entry.setSeverity(AuditSeverity.CRITICAL);
        pendingEntries.add(entry);
    }
    
    /**
     * Guarda todas las entradas pendientes
     */
    public void flush() {
        if (pendingEntries.isEmpty()) {
            return;
        }
        
        Map<String, List<AuditEntry>> entriesByDate = new HashMap<>();
        
        // Agrupar por fecha
        while (!pendingEntries.isEmpty()) {
            AuditEntry entry = pendingEntries.poll();
            String date = entry.getTimestamp().format(filenameFormatter);
            entriesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(entry);
        }
        
        // Guardar cada grupo en su archivo correspondiente
        for (Map.Entry<String, List<AuditEntry>> dateGroup : entriesByDate.entrySet()) {
            saveToFile(dateGroup.getKey(), dateGroup.getValue());
        }
    }
    
    /**
     * Guarda entradas en un archivo
     */
    private void saveToFile(String date, List<AuditEntry> entries) {
        File auditFile = new File(auditDir, "audit-" + date + ".json");
        List<AuditEntry> allEntries = new ArrayList<>();
        
        // Cargar entradas existentes si el archivo ya existe
        if (auditFile.exists()) {
            try {
                java.nio.file.Files.readAllLines(auditFile.toPath()).forEach(line -> {
                    try {
                        allEntries.add(gson.fromJson(line, AuditEntry.class));
                    } catch (Exception ignored) {}
                });
            } catch (IOException e) {
                plugin.getLogger().warning("Error leyendo archivo de auditoría: " + e.getMessage());
            }
        }
        
        // Añadir nuevas entradas
        allEntries.addAll(entries);
        
        // Guardar todo
        try (FileWriter writer = new FileWriter(auditFile)) {
            for (AuditEntry entry : allEntries) {
                writer.write(gson.toJson(entry) + "\n");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando auditoría: " + e.getMessage());
        }
    }
    
    /**
     * Inicia el guardado automático periódico
     */
    private void startAutoSave() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::flush, 6000L, 6000L); // Cada 5 minutos
    }
    
    /**
     * Obtiene estadísticas de auditoría
     */
    public Map<String, Object> getStatistics(int days) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        Map<AuditCategory, Integer> categoryCounts = new HashMap<>();
        Map<String, Integer> playerActions = new HashMap<>();
        int totalEntries = 0;
        
        File[] auditFiles = auditDir.listFiles((dir, name) -> name.startsWith("audit-") && name.endsWith(".json"));
        
        if (auditFiles != null) {
            for (File file : auditFiles) {
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        AuditEntry entry = gson.fromJson(line, AuditEntry.class);
                        if (entry.getTimestamp().isAfter(cutoff)) {
                            totalEntries++;
                            categoryCounts.merge(entry.getCategory(), 1, Integer::sum);
                            playerActions.merge(entry.getPlayerName(), 1, Integer::sum);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error leyendo estadísticas: " + e.getMessage());
                }
            }
        }
        
        stats.put("totalEntries", totalEntries);
        stats.put("categoryCounts", categoryCounts);
        stats.put("topPlayers", getTopPlayers(playerActions, 10));
        stats.put("period", days + " days");
        
        return stats;
    }
    
    /**
     * Obtiene los jugadores más activos
     */
    private List<Map.Entry<String, Integer>> getTopPlayers(Map<String, Integer> playerActions, int limit) {
        return playerActions.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .toList();
    }
    
    /**
     * Limpia archivos de auditoría antiguos
     */
    public void cleanOldAudits(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        File[] auditFiles = auditDir.listFiles((dir, name) -> name.startsWith("audit-") && name.endsWith(".json"));
        
        if (auditFiles != null) {
            for (File file : auditFiles) {
                String filename = file.getName();
                String dateStr = filename.replace("audit-", "").replace(".json", "");
                try {
                    LocalDateTime fileDate = LocalDateTime.parse(dateStr + " 00:00:00", dateFormatter);
                    if (fileDate.isBefore(cutoff)) {
                        file.delete();
                        plugin.getLogger().info("Archivo de auditoría eliminado: " + filename);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error procesando archivo: " + filename);
                }
            }
        }
    }
    
    /**
     * Categorías de auditoría
     */
    public enum AuditCategory {
        CLASS,
        QUEST,
        ECONOMY,
        NPC,
        ADMIN,
        ERROR,
        SYSTEM
    }
    
    /**
     * Niveles de severidad
     */
    public enum AuditSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * Entrada de auditoría
     */
    public static class AuditEntry {
        private final AuditCategory category;
        private final String playerName;
        private final String action;
        private final String details;
        private final LocalDateTime timestamp;
        private AuditSeverity severity;
        
        public AuditEntry(AuditCategory category, String playerName, String action, String details) {
            this.category = category;
            this.playerName = playerName;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now();
            this.severity = AuditSeverity.MEDIUM;
        }
        
        public AuditCategory getCategory() { return category; }
        public String getPlayerName() { return playerName; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public AuditSeverity getSeverity() { return severity; }
        
        public void setSeverity(AuditSeverity severity) {
            this.severity = severity;
        }
    }
}
