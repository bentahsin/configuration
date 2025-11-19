package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Locale;

@SuppressWarnings("unused")
public class LocationConverter implements Converter<String, Location> {

    /**
     * Config'den gelen String veriyi (örn: "world,100,64,200,90,0") Location nesnesine çevirir.
     */
    @Override
    public Location convertToField(String source) {
        if (source == null || !source.contains(",")) {
            return null;
        }

        try {
            String[] parts = source.split(",");

            if (parts.length < 4) {
                return null;
            }

            String worldName = parts[0];
            World world = Bukkit.getWorld(worldName);

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            float yaw = 0;
            float pitch = 0;

            if (parts.length > 4) {
                yaw = Float.parseFloat(parts[4]);
            }
            if (parts.length > 5) {
                pitch = Float.parseFloat(parts[5]);
            }

            return new Location(world, x, y, z, yaw, pitch);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Location nesnesini Config için sıkıştırılmış String formatına çevirir.
     */
    @Override
    public String convertToConfig(Location loc) {
        if (loc == null) return null;

        String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "world";
        return worldName + "," +
                format(loc.getX()) + "," +
                format(loc.getY()) + "," +
                format(loc.getZ()) + "," +
                format(loc.getYaw()) + "," +
                format(loc.getPitch());
    }

    /**
     * Sayıları temiz formatlar.
     * Örn: 10.0 -> "10", 10.5678 -> "10.57"
     */
    private String format(double d) {
        if (d == (long) d) {
            return String.format(Locale.ENGLISH, "%d", (long) d);
        }
        return String.format(Locale.ENGLISH, "%.2f", d);
    }
}