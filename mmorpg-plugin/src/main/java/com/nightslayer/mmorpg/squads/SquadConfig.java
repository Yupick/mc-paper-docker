package com.nightslayer.mmorpg.squads;

import java.util.List;
import java.util.Map;

public class SquadConfig {
    public SquadSystemConfig squad_system = new SquadSystemConfig();

    public static class SquadSystemConfig {
        public boolean enabled = true;
        public int max_squads = 100;
        public int max_squad_size = 10;
        public int min_squad_size = 2;
        public int squad_level_requirement = 5;
        public EconomyConfig economy = new EconomyConfig();
        public Map<String, RolePermissions> roles;
        public Map<String, LevelConfig> squad_levels;
        public Map<String, PerkConfig> perks;
        public Map<String, TreasuryPerkConfig> treasury_perks;
    }

    public static class EconomyConfig {
        public long create_cost = 500;
        public long level_up_cost_base = 1000;
        public double level_up_cost_multiplier = 1.5;
        public int treasury_tax_percent = 5;
    }

    public static class RolePermissions {
        public List<String> permissions;
        public long treasury_limit;
    }

    public static class LevelConfig {
        public int min_members;
        public int max_members;
        public List<String> perks;
    }

    public static class PerkConfig {
        public String name;
        public String description;
        public Integer slots;
        public Integer radius;
        public Double multiplier;
    }

    public static class TreasuryPerkConfig {
        public long cost;
        public int bonus_slots;
        public Double multiplier;
    }
}
