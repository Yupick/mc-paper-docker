package com.nightslayer.mmorpg.mobs;

import com.nightslayer.mmorpg.MMORPGPlugin;
import org.bukkit.Location;
import org.bukkit.World;

public class CustomMobManager {
    // Wrapper para exponer el spawnCustomMob de MobManager
    public static boolean spawnCustomMob(String mobId, World world, double x, double y, double z) {
        MobManager mobManager = MMORPGPlugin.getInstance().getMobManager();
        Location loc = new Location(world, x, y, z);
        return mobManager.spawnCustomMob(mobId, loc) != null;
    }
}
