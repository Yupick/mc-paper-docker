package com.nightslayer.mmorpg.invasions;

import java.util.List;

/**
 * Configuration for an invasion type
 */
public class InvasionConfig {
    private String invasionId;
    private String displayName;
    private String description;
    private List<String> targetWorlds;
    private List<InvasionWaveConfig> waves;
    private InvasionRewards rewards;
    private InvasionSchedule schedule;
    private boolean enabled;

    public InvasionConfig(String invasionId, String displayName, String description,
                          List<String> targetWorlds, List<InvasionWaveConfig> waves,
                          InvasionRewards rewards, InvasionSchedule schedule, boolean enabled) {
        this.invasionId = invasionId;
        this.displayName = displayName;
        this.description = description;
        this.targetWorlds = targetWorlds;
        this.waves = waves;
        this.rewards = rewards;
        this.schedule = schedule;
        this.enabled = enabled;
    }

    // Getters and setters
    public String getInvasionId() { return invasionId; }
    public void setInvasionId(String invasionId) { this.invasionId = invasionId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTargetWorlds() { return targetWorlds; }
    public void setTargetWorlds(List<String> targetWorlds) { this.targetWorlds = targetWorlds; }

    public List<InvasionWaveConfig> getWaves() { return waves; }
    public void setWaves(List<InvasionWaveConfig> waves) { this.waves = waves; }

    public InvasionRewards getRewards() { return rewards; }
    public void setRewards(InvasionRewards rewards) { this.rewards = rewards; }

    public InvasionSchedule getSchedule() { return schedule; }
    public void setSchedule(InvasionSchedule schedule) { this.schedule = schedule; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Wave configuration
     */
    public static class InvasionWaveConfig {
        private int waveNumber;
        private String mobType;
        private int mobCount;
        private int mobLevel;
        private int delaySeconds;
        private boolean isBossWave;
        private String bossName;
        private double bossHealthMultiplier;

        public InvasionWaveConfig(int waveNumber, String mobType, int mobCount, int mobLevel,
                                  int delaySeconds, boolean isBossWave, String bossName,
                                  double bossHealthMultiplier) {
            this.waveNumber = waveNumber;
            this.mobType = mobType;
            this.mobCount = mobCount;
            this.mobLevel = mobLevel;
            this.delaySeconds = delaySeconds;
            this.isBossWave = isBossWave;
            this.bossName = bossName;
            this.bossHealthMultiplier = bossHealthMultiplier;
        }

        // Getters and setters
        public int getWaveNumber() { return waveNumber; }
        public void setWaveNumber(int waveNumber) { this.waveNumber = waveNumber; }

        public String getMobType() { return mobType; }
        public void setMobType(String mobType) { this.mobType = mobType; }

        public int getMobCount() { return mobCount; }
        public void setMobCount(int mobCount) { this.mobCount = mobCount; }

        public int getMobLevel() { return mobLevel; }
        public void setMobLevel(int mobLevel) { this.mobLevel = mobLevel; }

        public int getDelaySeconds() { return delaySeconds; }
        public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

        public boolean isBossWave() { return isBossWave; }
        public void setBossWave(boolean bossWave) { isBossWave = bossWave; }

        public String getBossName() { return bossName; }
        public void setBossName(String bossName) { this.bossName = bossName; }

        public double getBossHealthMultiplier() { return bossHealthMultiplier; }
        public void setBossHealthMultiplier(double bossHealthMultiplier) {
            this.bossHealthMultiplier = bossHealthMultiplier;
        }
    }

    /**
     * Rewards configuration
     */
    public static class InvasionRewards {
        private int xpPerWave;
        private int coinsPerWave;
        private int xpBonus;
        private int coinsBonus;
        private List<String> specialItems;

        public InvasionRewards(int xpPerWave, int coinsPerWave, int xpBonus, int coinsBonus,
                               List<String> specialItems) {
            this.xpPerWave = xpPerWave;
            this.coinsPerWave = coinsPerWave;
            this.xpBonus = xpBonus;
            this.coinsBonus = coinsBonus;
            this.specialItems = specialItems;
        }

        // Getters and setters
        public int getXpPerWave() { return xpPerWave; }
        public void setXpPerWave(int xpPerWave) { this.xpPerWave = xpPerWave; }

        public int getCoinsPerWave() { return coinsPerWave; }
        public void setCoinsPerWave(int coinsPerWave) { this.coinsPerWave = coinsPerWave; }

        public int getXpBonus() { return xpBonus; }
        public void setXpBonus(int xpBonus) { this.xpBonus = xpBonus; }

        public int getCoinsBonus() { return coinsBonus; }
        public void setCoinsBonus(int coinsBonus) { this.coinsBonus = coinsBonus; }

        public List<String> getSpecialItems() { return specialItems; }
        public void setSpecialItems(List<String> specialItems) { this.specialItems = specialItems; }
    }

    /**
     * Schedule configuration
     */
    public static class InvasionSchedule {
        private String scheduleType; // "FIXED", "RANDOM", "MANUAL"
        private List<String> fixedTimes; // HH:MM format
        private int randomMinHours;
        private int randomMaxHours;
        private int durationMinutes;

        public InvasionSchedule(String scheduleType, List<String> fixedTimes,
                                int randomMinHours, int randomMaxHours, int durationMinutes) {
            this.scheduleType = scheduleType;
            this.fixedTimes = fixedTimes;
            this.randomMinHours = randomMinHours;
            this.randomMaxHours = randomMaxHours;
            this.durationMinutes = durationMinutes;
        }

        // Getters and setters
        public String getScheduleType() { return scheduleType; }
        public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

        public List<String> getFixedTimes() { return fixedTimes; }
        public void setFixedTimes(List<String> fixedTimes) { this.fixedTimes = fixedTimes; }

        public int getRandomMinHours() { return randomMinHours; }
        public void setRandomMinHours(int randomMinHours) { this.randomMinHours = randomMinHours; }

        public int getRandomMaxHours() { return randomMaxHours; }
        public void setRandomMaxHours(int randomMaxHours) { this.randomMaxHours = randomMaxHours; }

        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    }
}
