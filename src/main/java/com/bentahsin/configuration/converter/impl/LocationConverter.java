package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

@SuppressWarnings("unused")
public class LocationConverter implements Converter<String, Location> {

    @Override
    public Location convertToField(String source) {
        if (source == null || !source.contains(",")) return null;

        try {
            String[] parts = source.split(",");
            if (parts.length < 4) return null;

            String worldName = parts[0];
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Bukkit.getLogger().warning("[Configuration] World '" + worldName + "' not found for location!");
                return null;
            }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(world, x, y, z, yaw, pitch);

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String convertToConfig(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        return loc.getWorld().getName() + "," +
                format(loc.getX()) + "," +
                format(loc.getY()) + "," +
                format(loc.getZ()) + "," +
                format(loc.getYaw()) + "," +
                format(loc.getPitch());
    }

    private String format(double d) {
        if (d == (long) d) return String.format(Locale.ENGLISH, "%d", (long) d);
        return String.format(Locale.ENGLISH, "%.2f", d);
    }
}