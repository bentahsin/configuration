package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;
import com.bentahsin.configuration.util.Cuboid;

@SuppressWarnings("unused")
public class CuboidConverter implements Converter<String, Cuboid> {

    @Override
    public Cuboid convertToField(String source) {
        if (source == null || source.isEmpty()) return null;
        try {
            String[] split = source.split(",");
            if (split.length < 7) return null;

            return new Cuboid(
                    split[0],
                    Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]),
                    Integer.parseInt(split[4]), Integer.parseInt(split[5]), Integer.parseInt(split[6])
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String convertToConfig(Cuboid source) {
        if (source == null) return null;
        return source.toString();
    }
}