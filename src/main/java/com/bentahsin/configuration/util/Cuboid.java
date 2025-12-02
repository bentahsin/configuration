package com.bentahsin.configuration.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

@SuppressWarnings("unused")
public class Cuboid {
    private final String worldName;
    private final int x1, y1, z1;
    private final int x2, y2, z2;

    public Cuboid(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }

    public Cuboid(Location l1, Location l2) {
        if (!Objects.equals(l1.getWorld(), l2.getWorld()))
            throw new IllegalArgumentException("Locations must be in the same world");
        this.worldName = Objects.requireNonNull(l1.getWorld()).getName();
        this.x1 = l1.getBlockX();
        this.y1 = l1.getBlockY();
        this.z1 = l1.getBlockZ();
        this.x2 = l2.getBlockX();
        this.y2 = l2.getBlockY();
        this.z2 = l2.getBlockZ();
    }

    public boolean contains(Location loc) {
        if (!Objects.requireNonNull(loc.getWorld()).getName().equals(worldName)) return false;
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    public Location getCenter() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                (x1 + x2) / 2.0,
                (y1 + y2) / 2.0,
                (z1 + z2) / 2.0);
    }

    public String getWorldName() { return worldName; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }

    @Override
    public String toString() {
        return worldName + "," + x1 + "," + y1 + "," + z1 + "," + x2 + "," + y2 + "," + z2;
    }
}