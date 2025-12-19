package com.nightslayer.mmorpg;

public class WorldMetadata {
    private String name;
    private String description;
    private boolean isRPG;
    private RPGConfig rpgConfig;
    
    public WorldMetadata() {
        this.rpgConfig = new RPGConfig();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isRPG() {
        return isRPG;
    }
    
    public void setRPG(boolean RPG) {
        isRPG = RPG;
    }
    
    public RPGConfig getRpgConfig() {
        return rpgConfig;
    }
    
    public void setRpgConfig(RPGConfig rpgConfig) {
        this.rpgConfig = rpgConfig;
    }
    
    public static class RPGConfig {
        private boolean classesEnabled = true;
        private boolean questsEnabled = true;
        private boolean npcsEnabled = true;
        private boolean economyEnabled = true;
        
        public boolean isClassesEnabled() {
            return classesEnabled;
        }
        
        public void setClassesEnabled(boolean classesEnabled) {
            this.classesEnabled = classesEnabled;
        }
        
        public boolean isQuestsEnabled() {
            return questsEnabled;
        }
        
        public void setQuestsEnabled(boolean questsEnabled) {
            this.questsEnabled = questsEnabled;
        }
        
        public boolean isNpcsEnabled() {
            return npcsEnabled;
        }
        
        public void setNpcsEnabled(boolean npcsEnabled) {
            this.npcsEnabled = npcsEnabled;
        }
        
        public boolean isEconomyEnabled() {
            return economyEnabled;
        }
        
        public void setEconomyEnabled(boolean economyEnabled) {
            this.economyEnabled = economyEnabled;
        }
    }
}
