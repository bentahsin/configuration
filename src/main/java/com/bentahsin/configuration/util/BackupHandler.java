package com.bentahsin.configuration.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class BackupHandler {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /**
     * Creates a backup of the specified file in the target directory.
     *
     * @param plugin     JavaPlugin instance (for Logging and Path resolution)
     * @param sourceFile The source file to back up (e.g. config.yml)
     * @param folderPath The backup directory name (relative to plugin data folder, e.g. "backups")
     * @param suffix     Tag to append to the filename (e.g. "migration", "broken")
     */
    public static void createBackup(JavaPlugin plugin, File sourceFile, String folderPath, String suffix) {
        if (!sourceFile.exists()) return;

        File backupFolder = new File(plugin.getDataFolder(), folderPath);
        if (!backupFolder.exists()) {
            boolean ignored = backupFolder.mkdirs();
        }

        String fileName = sourceFile.getName().replace(".yml", "");
        String timestamp = DATE_FORMAT.format(new Date());
        String backupName = String.format("%s_%s_%s.yml", fileName, suffix, timestamp);

        File backupFile = new File(backupFolder, backupName);

        try {
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Backup created: " + backupFile.getPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred during backup creation!", e);
        }
    }
}