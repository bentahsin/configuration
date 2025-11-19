package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "deprecation"})
public class ItemStackConverter implements Converter<Map<String, Object>, ItemStack> {

    @Override
    public ItemStack convertToField(Map<String, Object> source) {
        try {
            String matStr = (String) source.getOrDefault("material", "STONE");
            Material material = Material.getMaterial(matStr.toUpperCase());
            if (material == null) material = Material.STONE;

            int amount = (int) source.getOrDefault("amount", 1);
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (source.containsKey("name")) {
                    meta.setDisplayName(color((String) source.get("name")));
                }

                if (source.containsKey("lore")) {
                    Object loreObj = source.get("lore");
                    if (loreObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> rawLore = (List<String>) loreObj;
                        meta.setLore(rawLore.stream().map(this::color).collect(Collectors.toList()));
                    }
                }

                if (source.containsKey("custom_model_data") && source.get("custom_model_data") instanceof Integer) {
                    int data = (int) source.get("custom_model_data");
                    setCustomModelData(meta, data);
                }

                if (source.containsKey("flags")) {
                    Object flagsObj = source.get("flags");
                    if (flagsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> flags = (List<String>) flagsObj;
                        for (String f : flags) {
                            try {
                                meta.addItemFlags(ItemFlag.valueOf(f.toUpperCase()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }

                if (source.containsKey("enchantments")) {
                    Object enchsObj = source.get("enchantments");
                    if (enchsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> enchs = (Map<String, Object>) enchsObj;

                        for (Map.Entry<String, Object> entry : enchs.entrySet()) {
                            Enchantment ench = getEnchantment(entry.getKey());
                            if (ench != null && entry.getValue() instanceof Integer) {
                                meta.addEnchant(ench, (int) entry.getValue(), true);
                            }
                        }
                    }
                }

                item.setItemMeta(meta);
            }
            return item;

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Configuration] ItemStack dönüştürülürken hata oluştu!", e);
            return new ItemStack(Material.AIR);
        }
    }

    @Override
    public Map<String, Object> convertToConfig(ItemStack item) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (item == null || item.getType() == Material.AIR) return map;

        map.put("material", item.getType().name());
        if (item.getAmount() > 1) map.put("amount", item.getAmount());

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (Objects.requireNonNull(meta).hasDisplayName()) {
                map.put("name", meta.getDisplayName().replace("§", "&"));
            }

            if (meta.hasLore()) {
                map.put("lore", Objects.requireNonNull(meta.getLore()).stream()
                        .map(s -> s.replace("§", "&"))
                        .collect(Collectors.toList()));
            }

            Integer customModelData = getCustomModelData(meta);
            if (customModelData != null) {
                map.put("custom_model_data", customModelData);
            }

            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) flags.add(flag.name());
                map.put("flags", flags);
            }

            if (meta.hasEnchants()) {
                Map<String, Integer> enchs = new LinkedHashMap<>();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    String enchName = getEnchantmentName(entry.getKey());
                    enchs.put(enchName, entry.getValue());
                }
                map.put("enchantments", enchs);
            }
        }
        return map;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }


    private void setCustomModelData(ItemMeta meta, int data) {
        try {
            Method method = meta.getClass().getMethod("setCustomModelData", Integer.class);
            method.setAccessible(true);
            method.invoke(meta, data);
        } catch (Exception ignored) {}
    }

    private Integer getCustomModelData(ItemMeta meta) {
        try {
            Method hasMethod = meta.getClass().getMethod("hasCustomModelData");
            if ((boolean) hasMethod.invoke(meta)) {
                Method getMethod = meta.getClass().getMethod("getCustomModelData");
                return (Integer) getMethod.invoke(meta);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Enchantment getEnchantment(String key) {
        try {
            return Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
        } catch (Throwable t) {
            return Enchantment.getByName(key.toUpperCase());
        }
    }

    private String getEnchantmentName(Enchantment ench) {
        try {
            return ench.getKey().getKey().toUpperCase();
        } catch (Throwable t) {
            return ench.getName();
        }
    }
}