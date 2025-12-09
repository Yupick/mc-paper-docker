package com.nightslayer.mmorpg.pets;

import java.util.List;
import java.util.Map;

public class PetAbility {
    private String id;
    private String name;
    private String description;
    private int cooldown;
    private boolean passive;
    private Map<String, Object> properties;

    public PetAbility(String id, String name, String description, int cooldown, boolean passive, Map<String, Object> properties) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.passive = passive;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean isPassive() {
        return passive;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    public double getDoubleProperty(String key, double defaultValue) {
        Object value = getProperty(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        Object value = getProperty(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public String getStringProperty(String key, String defaultValue) {
        Object value = getProperty(key);
        return value != null ? value.toString() : defaultValue;
    }
}
