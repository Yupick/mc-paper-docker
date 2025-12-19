package com.nightslayer.mmorpg.npcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un diálogo de NPC con múltiples líneas y opciones
 */
public class NPCDialogue {
    private final String id;
    private final List<String> lines;
    private final List<DialogueOption> options;
    private String nextDialogueId;
    
    public NPCDialogue(String id) {
        this.id = id;
        this.lines = new ArrayList<>();
        this.options = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public List<String> getLines() {
        return lines;
    }
    
    public void addLine(String line) {
        this.lines.add(line);
    }
    
    public List<DialogueOption> getOptions() {
        return options;
    }
    
    public void addOption(DialogueOption option) {
        this.options.add(option);
    }
    
    public String getNextDialogueId() {
        return nextDialogueId;
    }
    
    public void setNextDialogueId(String nextDialogueId) {
        this.nextDialogueId = nextDialogueId;
    }
    
    /**
     * Opción de diálogo que el jugador puede elegir
     */
    public static class DialogueOption {
        private final String text;
        private final String targetDialogueId;
        private final String action; // quest_accept, shop_open, etc.
        
        public DialogueOption(String text, String targetDialogueId) {
            this(text, targetDialogueId, null);
        }
        
        public DialogueOption(String text, String targetDialogueId, String action) {
            this.text = text;
            this.targetDialogueId = targetDialogueId;
            this.action = action;
        }
        
        public String getText() {
            return text;
        }
        
        public String getTargetDialogueId() {
            return targetDialogueId;
        }
        
        public String getAction() {
            return action;
        }
    }
}
