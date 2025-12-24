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

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "deprecation"})
public class ItemStackConverter implements Converter<Map<String, Object>, ItemStack> {

    private static final boolean SUPPORTS_POTION_DATA;
    private static final boolean SUPPORTS_HEX;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static Method CHAT_COLOR_OF_METHOD;

    static {
        boolean potionSupport = false;
        try {
            Class.forName("org.bukkit.potion.PotionData");
            potionSupport = true;
        } catch (ClassNotFoundException ignored) {}
        SUPPORTS_POTION_DATA = potionSupport;

        boolean hexSupport = false;
        try {
            Class<?> chatColorClass = Class.forName("net.md_5.bungee.api.ChatColor");
            CHAT_COLOR_OF_METHOD = chatColorClass.getMethod("of", String.class);
            hexSupport = true;
        } catch (Exception ignored) {
            CHAT_COLOR_OF_METHOD = null;
        }
        SUPPORTS_HEX = hexSupport;
    }

    private static final boolean SUPPORTS_POTION_CONTENTS;

    static {
        boolean contentsSupport = false;
        try {
            Class.forName("org.bukkit.inventory.meta.PotionMeta$PotionContents");
            contentsSupport = true;
        } catch (ClassNotFoundException ignored) {}
        SUPPORTS_POTION_CONTENTS = contentsSupport;
    }

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
                if (source.containsKey("name")) {
                    meta.setDisplayName(color(String.valueOf(source.get("name"))));
                }

                if (source.get("lore") instanceof List) {
                    List<?> rawList = (List<?>) source.get("lore");
                    List<String> lore = new ArrayList<>();
                    for (Object obj : rawList) {
                        if (obj != null) lore.add(color(obj.toString()));
                    }
                    meta.setLore(lore);
                }

                if (source.get("custom_model_data") instanceof Number) {
                    setCustomModelData(meta, ((Number) source.get("custom_model_data")).intValue());
                }

                if (source.containsKey("unbreakable") && source.get("unbreakable") instanceof Boolean) {
                    setUnbreakable(meta, (boolean) source.get("unbreakable"));
                }

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

                if (meta instanceof LeatherArmorMeta && source.containsKey("color")) {
                    Color color = getColorFromHex(String.valueOf(source.get("color")));
                    if (color != null) ((LeatherArmorMeta) meta).setColor(color);
                }

                if (meta instanceof PotionMeta && source.containsKey("color")) {
                    Color color = getColorFromHex(String.valueOf(source.get("color")));
                    if (color != null) {
                        try {
                            ((PotionMeta) meta).setColor(color);
                        } catch (NoSuchMethodError | Exception ignored) {}
                    }
                }

                if (meta instanceof PotionMeta && source.get("custom_effects") instanceof List) {
                    applyPotionEffects((PotionMeta) meta, (List<?>) source.get("custom_effects"));
                }

                if (meta instanceof SkullMeta && source.containsKey("skull_owner")) {
                    ((SkullMeta) meta).setOwner(String.valueOf(source.get("skull_owner")));
                }

                if (SUPPORTS_POTION_DATA && meta instanceof PotionMeta && source.containsKey("potion_type")) {
                    applyPotionData((PotionMeta) meta, source);
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

            if (isUnbreakable(meta)) map.put("unbreakable", true);

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

            if (meta instanceof LeatherArmorMeta) {
                Color color = ((LeatherArmorMeta) meta).getColor();
                try {
                    Color defaultColor = Bukkit.getItemFactory().getDefaultLeatherColor();
                    if (!color.equals(defaultColor)) {
                        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                        map.put("color", hex);
                    }
                } catch (Throwable t) {
                    String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                    map.put("color", hex);
                }
            }

            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta) meta;
                try {
                    if (potionMeta.hasColor()) {
                        Color color = potionMeta.getColor();
                        if (color != null) {
                            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                            map.put("color", hex);
                        }
                    }
                } catch (NoSuchMethodError | Exception ignored) {}
            }

            if (meta instanceof PotionMeta) {
                List<String> effects = savePotionEffects((PotionMeta) meta);
                if (!effects.isEmpty()) map.put("custom_effects", effects);
            }

            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                if (skull.hasOwner()) {
                    map.put("skull_owner", skull.getOwner());
                }
            }

            if (SUPPORTS_POTION_DATA && meta instanceof PotionMeta) {
                savePotionData((PotionMeta) meta, map);
            }
        }
        return map;
    }

    private Color getColorFromHex(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) return null;
        try {
            return Color.fromRGB(
                    Integer.valueOf(hex.substring(1, 3), 16),
                    Integer.valueOf(hex.substring(3, 5), 16),
                    Integer.valueOf(hex.substring(5, 7), 16)
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void applyPotionData(PotionMeta meta, Map<String, Object> source) {
        try {
            if (!source.containsKey("potion_type")) return;
            String typeStr = String.valueOf(source.get("potion_type")).toUpperCase();

            if (SUPPORTS_POTION_CONTENTS) {
                try {
                    Method setBaseType = meta.getClass().getMethod("setBasePotionType",
                            Class.forName("org.bukkit.potion.PotionType"));
                    Object potionType = Enum.valueOf((Class<Enum>) Class.forName("org.bukkit.potion.PotionType"), typeStr);
                    setBaseType.invoke(meta, potionType);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[Config] Modern PotionType bulunamadı: " + typeStr);
                }
            } else if (SUPPORTS_POTION_DATA) {
                try {
                    org.bukkit.potion.PotionType type = org.bukkit.potion.PotionType.valueOf(typeStr);
                    boolean extended = (boolean) source.getOrDefault("potion_extended", false);
                    boolean upgraded = (boolean) source.getOrDefault("potion_upgraded", false);
                    meta.setBasePotionData(new org.bukkit.potion.PotionData(type, extended, upgraded));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[Config] Legacy PotionType bulunamadı: " + typeStr);
                }
            }

            if (source.get("custom_effects") instanceof List) {
                applyPotionEffects(meta, (List<?>) source.get("custom_effects"));
            }

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Config] applyPotionData işlenirken hata oluştu!", e);
        }
    }

    private void applyPotionEffects(PotionMeta meta, List<?> rawEffects) {
        for (Object obj : rawEffects) {
            if (obj instanceof String) {
                String[] split = ((String) obj).split(":");
                try {
                    org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(split[0].toUpperCase());
                    if (type == null) continue;
                    int duration = split.length > 1 ? Integer.parseInt(split[1]) : 600;
                    int amplifier = split.length > 2 ? Integer.parseInt(split[2]) : 0;
                    meta.addCustomEffect(new org.bukkit.potion.PotionEffect(type, duration, amplifier), true);
                } catch (Exception ignored) {}
            }
        }
    }

    private List<String> savePotionEffects(PotionMeta meta) {
        List<String> list = new ArrayList<>();
        for (org.bukkit.potion.PotionEffect effect : meta.getCustomEffects()) {
            list.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier());
        }
        return list;
    }

    private void savePotionData(PotionMeta meta, Map<String, Object> map) {
        try {
            if (SUPPORTS_POTION_CONTENTS) {
                Method getBaseType = meta.getClass().getMethod("getBasePotionType");
                Object type = getBaseType.invoke(meta);
                if (type != null) {
                    map.put("potion_type", type.toString());
                }
            } else {
                org.bukkit.potion.PotionData data = meta.getBasePotionData();
                map.put("potion_type", data.getType().name());
                if (data.isExtended()) map.put("potion_extended", true);
                if (data.isUpgraded()) map.put("potion_upgraded", true);
            }
        } catch (Exception ignored) {}
    }

    private void setUnbreakable(ItemMeta meta, boolean unbreakable) {
        try {
            meta.setUnbreakable(unbreakable);
        } catch (NoSuchMethodError ignored) { }
    }

    private boolean isUnbreakable(ItemMeta meta) {
        try {
            return meta.isUnbreakable();
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    private String color(String s) {
        if (s == null) return "";

        if (SUPPORTS_HEX && CHAT_COLOR_OF_METHOD != null) {
            Matcher matcher = HEX_PATTERN.matcher(s);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    Object colorObj = CHAT_COLOR_OF_METHOD.invoke(null, "#" + matcher.group(1));
                    matcher.appendReplacement(buffer, colorObj.toString());
                } catch (Exception e) {
                    return s;
                }
            }
            matcher.appendTail(buffer);
            s = buffer.toString();
        }

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