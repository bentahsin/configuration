package com.bentahsin.configuration.core;

import com.bentahsin.configuration.annotation.*;
import com.bentahsin.configuration.converter.Converter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

public class ConfigMapper {

    private final Logger logger;

    public ConfigMapper(Logger logger) {
        this.logger = logger;
    }

    public void handleVersion(Object instance, ConfigurationSection config) {
        if (!instance.getClass().isAnnotationPresent(ConfigVersion.class)) return;

        int classVersion = instance.getClass().getAnnotation(ConfigVersion.class).value();
        int fileVersion = config.getInt("config-version", 0);

        if (fileVersion < classVersion) {
            logger.info("Updating config version: v" + fileVersion + " -> v" + classVersion);
            config.set("config-version", classVersion);
        }
    }

    public void runOnReload(Object instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnReload.class)) {
                try {
                    method.setAccessible(true);
                    logger.info("Running reload trigger: " + method.getName());
                    method.invoke(instance);
                } catch (Exception e) {
                    logger.severe("Error running OnReload method: " + method.getName());
                    logger.severe(e.getMessage());
                }
            }
        }
    }

    public void loadFromConfig(Object instance, ConfigurationSection config) {
        if (instance == null || config == null) return;
        processClass(instance, config);
        runPostLoad(instance);
    }

    public void saveToConfig(Object instance, ConfigurationSection config) {
        if (instance == null || config == null) return;
        saveClass(instance, config);
    }

    private void processClass(Object instance, ConfigurationSection config) {
        Class<?> clazz = instance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (!shouldProcess(field)) continue;

            String pathKey = getPathKey(field);

            try {
                field.setAccessible(true);

                if (field.isAnnotationPresent(Transform.class)) {
                    Object val = config.get(pathKey);
                    if (val != null) {
                        applyConverter(instance, field, val);
                    }
                    continue;
                }

                if (Map.class.isAssignableFrom(field.getType())) {
                    handleMapLoad(instance, field, config, pathKey);
                    continue;
                }

                if (List.class.isAssignableFrom(field.getType())) {
                    handleListLoad(instance, field, config, pathKey);
                    continue;
                }

                if (isComplexObject(field.getType())) {
                    Object fieldInstance = field.get(instance);
                    if (fieldInstance == null) {
                        fieldInstance = field.getType().getDeclaredConstructor().newInstance();
                        field.set(instance, fieldInstance);
                    }

                    ConfigurationSection subSection = config.getConfigurationSection(pathKey);
                    if (subSection == null && config.contains(pathKey)) {
                        subSection = config.createSection(pathKey);
                    }

                    if (subSection != null) {
                        processClass(fieldInstance, subSection);
                    }
                    continue;
                }

                if (config.contains(pathKey)) {
                    Object value = config.get(pathKey);
                    safeSetField(instance, field, value);
                }

            } catch (Exception e) {
                logger.warning("Error loading config (" + field.getName() + "): " + e.getMessage());
            }
        }
    }

    private void saveClass(Object instance, ConfigurationSection config) {
        if (config.getParent() == null && instance.getClass().isAnnotationPresent(ConfigHeader.class)) {
            ConfigHeader header = instance.getClass().getAnnotation(ConfigHeader.class);
            List<String> headerLines = Arrays.asList(header.value());

            if (config.getRoot() instanceof FileConfiguration) {
                FileConfiguration fileConfig = (FileConfiguration) config.getRoot();
                String headerString = String.join(System.lineSeparator(), headerLines);
                fileConfig.options().header(headerString);
                fileConfig.options().copyHeader(true);
            }
        }

        for (Field field : instance.getClass().getDeclaredFields()) {
            if (!shouldProcess(field)) continue;
            String path = getPathKey(field);

            try {
                field.setAccessible(true);
                Object value = field.get(instance);

                if (value == null) continue;

                if (field.isAnnotationPresent(Transform.class)) {
                    Class<? extends Converter<?, ?>> converterClass = field.getAnnotation(Transform.class).value();
                    Converter<?, ?> converter = converterClass.getDeclaredConstructor().newInstance();
                    @SuppressWarnings("unchecked")
                    Object configValue = ((Converter<Object, Object>) converter).convertToConfig(value);
                    config.set(path, configValue);
                    continue;
                }

                if (List.class.isAssignableFrom(field.getType())) {
                    handleListSave(field, config, path, (List<?>) value);
                    continue;
                }

                if (Map.class.isAssignableFrom(field.getType())) {
                    handleMapSave(field, config, path, (Map<?, ?>) value);
                    continue;
                }

                if (isComplexObject(field.getType())) {
                    ConfigurationSection subSection = config.createSection(path);
                    saveClass(value, subSection);
                    continue;
                }

                if (field.isAnnotationPresent(Comment.class)) {
                    Comment commentAnno = field.getAnnotation(Comment.class);
                    List<String> comments = Arrays.asList(commentAnno.value());
                    setComments(config, path, comments);
                }

                config.set(path, value);

            } catch (Exception e) {
                logger.severe("Save error: " + e.getMessage());
            }
        }
    }

    private void handleListLoad(Object instance, Field field, ConfigurationSection config, String path) throws Exception {
        if (!config.contains(path)) return;

        Class<?> genericType = getListType(field);

        if (genericType == String.class || isPrimitive(genericType)) {
            field.set(instance, config.getList(path));
            return;
        }

        if (Map.class.isAssignableFrom(genericType)) {
            field.set(instance, config.getMapList(path));
            return;
        }

        List<Map<?, ?>> rawList = config.getMapList(path);
        List<Object> resultList = new ArrayList<>();

        for (Map<?, ?> rawMap : rawList) {
            Object itemInstance = genericType.getDeclaredConstructor().newInstance();
            MemoryConfiguration tempConfig = new MemoryConfiguration();

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                tempConfig.set(entry.getKey().toString(), entry.getValue());
            }

            processClass(itemInstance, tempConfig);
            resultList.add(itemInstance);
        }

        field.set(instance, resultList);
    }

    private void handleListSave(Field field, ConfigurationSection config, String path, List<?> list) throws Exception {
        if (list.isEmpty()) {
            config.set(path, new ArrayList<>());
            return;
        }

        Class<?> genericType = getListType(field);

        if (genericType == String.class || isPrimitive(genericType) || Map.class.isAssignableFrom(genericType)) {
            config.set(path, list);
            return;
        }

        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Object obj : list) {
            Map<String, Object> objectMap = new LinkedHashMap<>();
            for (Field objField : obj.getClass().getDeclaredFields()) {
                if (!shouldProcess(objField)) continue;
                objField.setAccessible(true);
                String objPath = getPathKey(objField);
                objectMap.put(objPath, objField.get(obj));
            }
            mapList.add(objectMap);
        }

        config.set(path, mapList);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleMapLoad(Object instance, Field field, ConfigurationSection config, String path) throws Exception {
        if (!config.isConfigurationSection(path)) return;

        ConfigurationSection section = config.getConfigurationSection(path);
        Map<Object, Object> map = new HashMap<>();

        Class<?> keyType = getMapKeyType(field);
        Class<?> valueType = getMapValueType(field);

        for (String rawKey : Objects.requireNonNull(section).getKeys(false)) {

            Object convertedKey = convertKey(rawKey, keyType);
            if (convertedKey == null) continue;

            Object value;
            if (isComplexObject(valueType)) {
                Object valueInstance = createInstance(valueType);
                ConfigurationSection valueSection = section.getConfigurationSection(rawKey);
                if (valueSection != null) {
                    processClass(valueInstance, valueSection);
                    value = valueInstance;
                } else {
                    continue;
                }
            } else {
                Object rawValue = section.get(rawKey);
                if (valueType.isEnum() && rawValue instanceof String) {
                    try {
                        value = Enum.valueOf((Class<Enum>) valueType, ((String) rawValue).toUpperCase(Locale.ENGLISH));
                    } catch (Exception e) { continue; }
                } else if (rawValue instanceof Number) {
                    Number num = (Number) rawValue;
                    if (valueType == int.class || valueType == Integer.class) value = num.intValue();
                    else if (valueType == long.class || valueType == Long.class) value = num.longValue();
                    else if (valueType == double.class || valueType == Double.class) value = num.doubleValue();
                    else value = rawValue;
                } else {
                    value = rawValue;
                }
            }
            map.put(convertedKey, value);
        }

        field.set(instance, map);
    }

    private Class<?> getMapKeyType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                Type keyType = args[0];
                if (keyType instanceof Class) return (Class<?>) keyType;
            }
        }
        return String.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertKey(String key, Class<?> targetType) {
        try {
            if (targetType == String.class) return key;
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, key.toUpperCase(Locale.ENGLISH));
            }
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(key);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(key);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(key);
            if (targetType == UUID.class) return UUID.fromString(key);
        } catch (Exception e) {
            logger.warning("Map Key conversion failed: " + key + " -> " + targetType.getSimpleName());
        }
        return null;
    }

    private Object createInstance(Class<?> clazz) throws Exception {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            if (clazz.getEnclosingClass() != null && !Modifier.isStatic(clazz.getModifiers())) {
                throw new IllegalStateException("ERROR: '" + clazz.getSimpleName() + "' is an Inner Class and NOT STATIC! " +
                        "Please define config classes as 'public static class'.");
            }
            throw e;
        }
    }

    private void handleMapSave(Field field, ConfigurationSection config, String path, Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            config.createSection(path);
            return;
        }

        ConfigurationSection section = config.createSection(path);
        Class<?> valueType = getMapValueType(field);
        boolean isComplex = isComplexObject(valueType);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value == null) continue;

            if (isComplex) {
                ConfigurationSection subSection = section.createSection(key);
                saveClass(value, subSection);
            } else {
                if (valueType.isEnum()) {
                    section.set(key, value.toString());
                } else {
                    section.set(key, value);
                }
            }
        }
    }

    private Class<?> getMapValueType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 1) {
                Type valueType = args[1];
                if (valueType instanceof Class) {
                    return (Class<?>) valueType;
                }
            }
        }
        return Object.class;
    }

    private Class<?> getListType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0) {
                Type valueType = args[0];
                if (valueType instanceof Class) {
                    return (Class<?>) valueType;
                }
            }
        }
        return Object.class;
    }

    private boolean isComplexObject(Class<?> type) {
        return !isPrimitive(type) &&
                !type.isEnum() &&
                !type.isArray() &&
                !Collection.class.isAssignableFrom(type) &&
                !Map.class.isAssignableFrom(type) &&
                !type.getName().startsWith("java.") &&
                !org.bukkit.Location.class.isAssignableFrom(type);
    }

    private boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() ||
                type == String.class ||
                Number.class.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type);
    }

    @SuppressWarnings("unchecked")
    private void applyConverter(Object instance, Field field, Object value) throws Exception {
        Class<? extends Converter<?, ?>> converterClass = field.getAnnotation(Transform.class).value();
        Converter<?, ?> converter = converterClass.getDeclaredConstructor().newInstance();
        Object convertedValue = ((Converter<Object, Object>) converter).convertToField(value);
        field.set(instance, convertedValue);
    }

    private void safeSetField(Object instance, Field field, Object value) throws IllegalAccessException {
        Class<?> type = field.getType();

        if (field.isAnnotationPresent(Validate.class)) {
            Validate validate = field.getAnnotation(Validate.class);

            if (value == null) {
                if (validate.notNull()) {
                    logger.warning("Config Error: " + field.getName() + " cannot be null!");
                }
                return;
            }

            if (value instanceof Number) {
                double val = ((Number) value).doubleValue();
                if (val < validate.min() || val > validate.max()) {
                    logger.warning(String.format("Config Limit Error (%s): %s (Min:%s Max:%s)", field.getName(), val, validate.min(), validate.max()));
                    return;
                }
            }

            if (value instanceof String && !validate.pattern().isEmpty()) {
                String strVal = (String) value;
                if (!strVal.matches(validate.pattern())) {
                    logger.warning("Config Format Error (" + field.getName() + "): Value '" + strVal + "' does not match format.");
                    logger.warning("Expected Regex: " + validate.pattern());
                    return;
                }
            }
        }

        if (value == null) return;

        if (type.isEnum()) {
            if (value instanceof String) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, ((String) value).toUpperCase(Locale.ENGLISH));
                    field.set(instance, enumValue);
                } catch (IllegalArgumentException e) {
                    logger.warning("Enum Error: '" + field.getName() + "' invalid: " + value);
                }
            }
            return;
        }

        if (value instanceof Number) {
            Number num = (Number) value;
            if (type == int.class || type == Integer.class) field.set(instance, num.intValue());
            else if (type == long.class || type == Long.class) field.set(instance, num.longValue());
            else if (type == double.class || type == Double.class) field.set(instance, num.doubleValue());
            else if (type == float.class || type == Float.class) field.set(instance, num.floatValue());
            else if (type == short.class || type == Short.class) field.set(instance, num.shortValue());
            else if (type == byte.class || type == Byte.class) field.set(instance, num.byteValue());
            else if (type == String.class) field.set(instance, num.toString());
            return;
        }

        if (type == String.class && !(value instanceof String)) {
            field.set(instance, value.toString());
            return;
        }

        if (type.isAssignableFrom(value.getClass())) {
            field.set(instance, value);
        } else {
            logger.warning("Type Mismatch: '" + field.getName() + "' Expected: " + type.getSimpleName() + ", Got: " + value.getClass().getSimpleName());
        }
    }

    private boolean shouldProcess(Field field) {
        return !Modifier.isStatic(field.getModifiers()) &&
                !Modifier.isFinal(field.getModifiers()) &&
                !field.isAnnotationPresent(Ignore.class);
    }

    private String getPathKey(Field field) {
        if (field.isAnnotationPresent(ConfigPath.class)) {
            String val = field.getAnnotation(ConfigPath.class).value();
            if (!val.isEmpty()) return val;
        }
        return field.getName().replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }

    private void setComments(ConfigurationSection config, String path, List<String> comments) {
        try {
            Method method = config.getClass().getMethod("setComments", String.class, List.class);
            method.setAccessible(true);
            method.invoke(config, path, comments);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}
    }

    private void runPostLoad(Object instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostLoad.class)) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    logger.warning("PostLoad metodu çalışırken hata: " + method.getName());
                    logger.severe(e.getMessage());
                }
            }
        }
    }
}