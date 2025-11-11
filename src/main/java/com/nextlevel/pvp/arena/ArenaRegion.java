package com.nextlevel.pvp.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class ArenaRegion implements ConfigurationSerializable {

    private final Location pos1;
    private final Location pos2;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;
    private final World world;

    public ArenaRegion(Location pos1, Location pos2) {
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            throw new IllegalArgumentException("Both positions must be in the same world!");
        }

        this.pos1 = pos1;
        this.pos2 = pos2;
        this.world = pos1.getWorld();

        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.minY = Math.min(pos1.getY(), pos2.getY());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());

        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.maxY = Math.max(pos1.getY(), pos2.getY());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
    }

    public ArenaRegion(Map<String, Object> map) {
        this.pos1 = (Location) map.get("pos1");
        this.pos2 = (Location) map.get("pos2");
        this.world = pos1.getWorld();

        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.minY = Math.min(pos1.getY(), pos2.getY());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());

        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.maxY = Math.max(pos1.getY(), pos2.getY());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("pos1", pos1);
        map.put("pos2", pos2);
        return map;
    }

    public boolean contains(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public Location getPos1() {
        return pos1.clone();
    }

    public Location getPos2() {
        return pos2.clone();
    }

    public World getWorld() {
        return world;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMinZ() {
        return minZ;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getMaxZ() {
        return maxZ;
    }
}
