package com.nightslayer.mmorpg.crafting;

import org.bukkit.Location;
import org.bukkit.World;

public class CraftingStation {
    private final String id;
    private final String name;
    private final String description;
    private final Location location;
    private final double radius;

    public CraftingStation(String id, String name, String description, Location location, double radius) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.radius = radius;
    }

    public CraftingStation(String id, String name, String description, World world, 
                          int x, int y, int z, double radius) {
        this(id, name, description, new Location(world, x, y, z), radius);
    }

    public boolean isNear(Location playerLocation) {
        if (!playerLocation.getWorld().equals(location.getWorld())) {
            return false;
        }
        return playerLocation.distance(location) <= radius;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public double getRadius() { return radius; }
    public World getWorld() { return location.getWorld(); }
    public int getX() { return location.getBlockX(); }
    public int getY() { return location.getBlockY(); }
    public int getZ() { return location.getBlockZ(); }
}
