package com.bentahsin.configuration;

import com.bentahsin.configuration.annotation.Backup;
import com.bentahsin.configuration.annotation.ConfigVersion;
import com.bentahsin.configuration.core.ConfigMapper;
import com.bentahsin.configuration.util.BackupHandler;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class Configuration {

    private final JavaPlugin plugin;
    private final ConfigMapper mapper;

    @SuppressWarnings("unused")
    public Configuration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mapper = new ConfigMapper(plugin.getLogger());
    }

    /**
     * Initializes, loads, and completes missing settings for the config file.
     * This method can also be used for the "Reload" operation (logic-wise).
     *
     * @param configInstance The instance of the config class (e.g., new AntiAfkConfig())
     * @param fileName       The file name (e.g., "config.yml")
     */
    public void init(Object configInstance, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            createFile(file, fileName);
        }

        YamlConfiguration yamlConfig = new YamlConfiguration();
        boolean loadFailed = false;

        try {
            yamlConfig.load(file);
        } catch (InvalidConfigurationException e) {
            plugin.getLogger().severe("!!! Critical Error !!!");
            plugin.getLogger().severe(fileName + " is broken! Please check the YAML format.");

            handleBackupOnFailure(configInstance, file);
            loadFailed = true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Config file could not be read or is corrupt: " + fileName, e);
            return;
        }

        if (!loadFailed) {
            handleBackupOnMigration(configInstance, yamlConfig, file);
        }

        if (!loadFailed) {
            mapper.loadFromConfig(configInstance, yamlConfig);
        }

        if (!loadFailed) {
            mapper.handleVersion(configInstance, yamlConfig);
        } else if (configInstance.getClass().isAnnotationPresent(ConfigVersion.class)) {
            int v = configInstance.getClass().getAnnotation(ConfigVersion.class).value();
            yamlConfig.set("config-version", v);
        }

        mapper.saveToConfig(configInstance, yamlConfig);

        try {
            yamlConfig.save(file);
            if (loadFailed) {
                plugin.getLogger().warning("Broken file has been backed up and " + fileName + " has been recreated with default settings.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred while updating config: " + fileName, e);
        }
    }

    /**
     * Reloads the configuration.
     * Essentially calls init() but also triggers @OnReload methods.
     */
    @SuppressWarnings("unused")
    public void reload(Object configInstance, String fileName) {
        init(configInstance, fileName);
        mapper.runOnReload(configInstance);
    }

    /**
     * Saves the current object state to the file.
     * Can be called when changing settings via in-game commands.
     */
    @SuppressWarnings("unused")
    public void save(Object configInstance, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration yamlConfig = new YamlConfiguration();

        try {
            if (file.exists()) {
                yamlConfig.load(file);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load existing config, overwriting: " + fileName);
        }

        mapper.saveToConfig(configInstance, yamlConfig);

        try {
            yamlConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config: " + fileName, e);
        }
    }

    /**
     * Internal helper logic for file creation.
     */
    private void createFile(File file, String fileName) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                boolean ignored = file.getParentFile().mkdirs();
            }

            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                if (file.createNewFile()) {
                    plugin.getLogger().info("New config file created: " + fileName);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create config file: " + fileName, e);
        }
    }

    /**
     * Helper: Checks and handles backup on failure (Syntax Error).
     */
    private void handleBackupOnFailure(Object instance, File file) {
        if (instance.getClass().isAnnotationPresent(Backup.class)) {
            Backup backup = instance.getClass().getAnnotation(Backup.class);
            if (backup.enabled() && backup.onFailure()) {
                plugin.getLogger().info("Backing up broken file...");
                BackupHandler.createBackup(plugin, file, backup.path(), "broken");
            }
        }
    }

    /**
     * Helper: Checks and handles backup on version migration.
     */
    private void handleBackupOnMigration(Object instance, YamlConfiguration config, File file) {
        if (instance.getClass().isAnnotationPresent(Backup.class) &&
                instance.getClass().isAnnotationPresent(ConfigVersion.class)) {

            Backup backup = instance.getClass().getAnnotation(Backup.class);
            ConfigVersion versionAnno = instance.getClass().getAnnotation(ConfigVersion.class);

            int fileVersion = config.getInt("config-version", 0);
            int classVersion = versionAnno.value();

            if (fileVersion < classVersion && backup.enabled() && backup.onMigration()) {
                plugin.getLogger().info("Upgrading version (" + fileVersion + " -> " + classVersion + "). Backing up...");
                BackupHandler.createBackup(plugin, file, backup.path(), "v" + fileVersion);
            }
        }
    }
}