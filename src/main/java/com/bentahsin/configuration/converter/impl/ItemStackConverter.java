package com.bentahsin.configuration.converter.impl;

import com.bentahsin.configuration.converter.Converter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

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

            int amount = 1;
            if (source.get("amount") instanceof Number) {
                amount = ((Number) source.get("amount")).intValue();
            }

            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                // --- Name ---
                if (source.containsKey("name")) {
                    meta.setDisplayName(color(String.valueOf(source.get("name"))));
                }

                // --- Lore (Güvenli Liste Dönüşümü) ---
                if (source.get("lore") instanceof List) {
                    List<?> rawList = (List<?>) source.get("lore");
                    List<String> lore = new ArrayList<>();
                    for (Object obj : rawList) {
                        if (obj != null) {
                            lore.add(color(obj.toString()));
                        }
                    }
                    meta.setLore(lore);
                }

                // --- Custom Model Data ---
                if (source.get("custom_model_data") instanceof Number) {
                    setCustomModelData(meta, ((Number) source.get("custom_model_data")).intValue());
                }

                // --- Unbreakable ---
                if (source.containsKey("unbreakable") && source.get("unbreakable") instanceof Boolean) {
                    meta.setUnbreakable((boolean) source.get("unbreakable"));
                }

                // --- Flags (Güvenli Liste Dönüşümü) ---
                if (source.get("flags") instanceof List) {
                    List<?> flags = (List<?>) source.get("flags");
                    for (Object obj : flags) {
                        if (obj instanceof String) {
                            try {
                                meta.addItemFlags(ItemFlag.valueOf(((String) obj).toUpperCase()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }

                // --- Enchantments (Güvenli Map Dönüşümü) ---
                if (source.get("enchantments") instanceof Map) {
                    Map<?, ?> enchs = (Map<?, ?>) source.get("enchantments");
                    for (Map.Entry<?, ?> entry : enchs.entrySet()) {
                        if (entry.getKey() instanceof String && entry.getValue() instanceof Number) {
                            Enchantment ench = getEnchantment((String) entry.getKey());
                            if (ench != null) {
                                meta.addEnchant(ench, ((Number) entry.getValue()).intValue(), true);
                            }
                        }
                    }
                }

                // --- ÖZEL META TİPLERİ ---

                // 1. Deri Zırh Boyası
                if (meta instanceof LeatherArmorMeta && source.containsKey("color")) {
                    String hex = String.valueOf(source.get("color"));
                    if (hex.startsWith("#") && hex.length() == 7) {
                        try {
                            ((LeatherArmorMeta) meta).setColor(Color.fromRGB(
                                    Integer.valueOf(hex.substring(1, 3), 16),
                                    Integer.valueOf(hex.substring(3, 5), 16),
                                    Integer.valueOf(hex.substring(5, 7), 16)
                            ));
                        } catch (Exception ignored) {}
                    }
                }

                // 2. Oyuncu Kafası (Skull)
                if (meta instanceof SkullMeta && source.containsKey("skull_owner")) {
                    ((SkullMeta) meta).setOwner(String.valueOf(source.get("skull_owner")));
                }

                // 3. İksirler (Potion)
                if (meta instanceof PotionMeta && source.containsKey("potion_type")) {
                    try {
                        PotionType type = PotionType.valueOf(String.valueOf(source.get("potion_type")).toUpperCase());
                        boolean extended = (boolean) source.getOrDefault("potion_extended", false);
                        boolean upgraded = (boolean) source.getOrDefault("potion_upgraded", false);
                        ((PotionMeta) meta).setBasePotionData(new PotionData(type, extended, upgraded));
                    } catch (Exception ignored) {}
                }

                item.setItemMeta(meta);
            }
            return item;

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[Configuration] Error loading ItemStack!", e);
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

            Integer cmd = getCustomModelData(meta);
            if (cmd != null) map.put("custom_model_data", cmd);

            if (meta.isUnbreakable()) map.put("unbreakable", true);

            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) flags.add(flag.name());
                map.put("flags", flags);
            }

            if (meta.hasEnchants()) {
                Map<String, Integer> enchs = new LinkedHashMap<>();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchs.put(getEnchantmentName(entry.getKey()), entry.getValue());
                }
                map.put("enchantments", enchs);
            }

            // --- ÖZEL META KAYITLARI ---

            if (meta instanceof LeatherArmorMeta) {
                Color color = ((LeatherArmorMeta) meta).getColor();
                String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                map.put("color", hex);
            }

            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                if (skull.hasOwner()) {
                    map.put("skull_owner", skull.getOwner());
                }
            }

            if (meta instanceof PotionMeta) {
                PotionData data = ((PotionMeta) meta).getBasePotionData();
                map.put("potion_type", data.getType().name());
                if (data.isExtended()) map.put("potion_extended", true);
                if (data.isUpgraded()) map.put("potion_upgraded", true);
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