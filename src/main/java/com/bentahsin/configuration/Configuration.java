package com.bentahsin.configuration;

import com.bentahsin.configuration.core.ConfigMapper;
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
     * Config dosyasını başlatır, yükler ve eksik ayarları tamamlar.
     * Bu metod aynı zamanda "Reload" işlemi için de kullanılabilir.
     *
     * @param configInstance Config sınıfının örneği (örn: new AntiAfkConfig())
     * @param fileName       Dosya adı (örn: "config.yml")
     */
    public void init(Object configInstance, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            createFile(file, fileName);
        }

        YamlConfiguration yamlConfig = new YamlConfiguration();
        try {
            yamlConfig.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "Config dosyası okunamadı veya bozuk: " + fileName, e);
            return;
        }

        mapper.loadFromConfig(configInstance, yamlConfig);
        mapper.saveToConfig(configInstance, yamlConfig);

        try {
            yamlConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Config güncellenirken hata oluştu: " + fileName, e);
        }
    }

    /**
     * Yapılandırmayı sadece yeniden yükler (init ile aynı işlevi görür, okunabilirlik için).
     */
    @SuppressWarnings("unused")
    public void reload(Object configInstance, String fileName) {
        init(configInstance, fileName);
    }

    /**
     * Mevcut nesne durumunu dosyaya kaydeder.
     * Oyun içi komutla ayar değiştirdiğinde bunu çağırabilirsin.
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
            plugin.getLogger().warning("Mevcut config yüklenemedi, üzerine yazılıyor: " + fileName);
        }

        mapper.saveToConfig(configInstance, yamlConfig);

        try {
            yamlConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Config kaydedilemedi: " + fileName, e);
        }
    }

    /**
     * Dosya oluşturma mantığı (Private Helper)
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
                    plugin.getLogger().info("Yeni config dosyası oluşturuldu: " + fileName);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Config dosyası oluşturulamadı: " + fileName, e);
        }
    }
}