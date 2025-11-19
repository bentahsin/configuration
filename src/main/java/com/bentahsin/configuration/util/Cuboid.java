package com.bentahsin.configuration.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;

public class Cuboid {
    private final String worldName;
    private final int x1, y1, z1;
    private final int x2, y2, z2;

    public Cuboid(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    @SuppressWarnings("unused")
    public Cuboid(Location l1, Location l2) {
        if (!Objects.equals(l1.getWorld(), l2.getWorld())) throw new IllegalArgumentException("Locations must be in the same world");
        this.worldName = Objects.requireNonNull(l1.getWorld()).getName();
        this.x1 = Math.min(l1.getBlockX(), l2.getBlockX());
        this.y1 = Math.min(l1.getBlockY(), l2.getBlockY());
        this.z1 = Math.min(l1.getBlockZ(), l2.getBlockZ());
        this.x2 = Math.max(l1.getBlockX(), l2.getBlockX());
        this.y2 = Math.max(l1.getBlockY(), l2.getBlockY());
        this.z2 = Math.max(l1.getBlockZ(), l2.getBlockZ());
    }

    @SuppressWarnings("unused")
    public boolean contains(Location loc) {
        if (!Objects.requireNonNull(loc.getWorld()).getName().equals(worldName)) return false;
        return loc.getBlockX() >= x1 && loc.getBlockX() <= x2 &&
                loc.getBlockY() >= y1 && loc.getBlockY() <= y2 &&
                loc.getBlockZ() >= z1 && loc.getBlockZ() <= z2;
    }

    @SuppressWarnings("unused")
    public Location getCenter() {
        return new Location(Bukkit.getWorld(worldName),
                x1 + (x2 - x1) / 2.0,
                y1 + (y2 - y1) / 2.0,
                z1 + (z2 - z1) / 2.0);
    }

    @Override
    public String toString() {
        return worldName + "," + x1 + "," + y1 + "," + z1 + "," + x2 + "," + y2 + "," + z2;
    }
}