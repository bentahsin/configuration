# Bentahsin Configuration Library

Spigot/Bukkit eklentileri için geliştirilmiş, **Annotasyon tabanlı (Annotation-Driven)**, **Reflection destekli** ve **Sürümden Bağımsız (Cross-Version)** çalışan gelişmiş bir konfigürasyon yönetim kütüphanesi.

`config.get("path")` karmaşasına son verin. Java sınıflarınızı (POJO) doğrudan YAML dosyalarına bağlayın.

## ✨ Özellikler

*   **Nesne Tabanlı Yönetim:** Config dosyalarını Java sınıfları olarak yönetin.
*   **Otomatik Dönüştürücüler (Converters):**
    *   `ItemStack` (Hex Renkler, NBT, CustomModelData, İksirler, Deri Zırhlar dahil).
    *   `Location` (Dünya güvenli yükleme).
    *   `Cuboid` (Bölge ve Yön/Vektör destekli).
    *   `Time` ("1h 30m" gibi süreleri saniyeye çevirir).
*   **Güçlü Validasyon:** `@Validate` ile verilerinizi (Min/Max, Regex, NotNull) koruyun. Hatalı veri girilirse varsayılan değer korunur.
*   **Sürüm Desteği (1.8 - 1.20+):** Tek kod tabanı ile hem 1.8 sunucularda hem de son sürüm sunucularda (Hex renkler, Off-hand vb.) sorunsuz çalışır.
*   **Akıllı Reload Sistemi:** `/reload` atıldığında "Dirty State" (eski verilerin kalması) sorunu yaşanmaz. Sınıf tamamen sıfırlanıp yeniden yüklenir.
*   **Otomatik Yedekleme:** Config dosyası bozulursa veya sürüm güncellenirse (`@ConfigVersion`) otomatik yedek alır.
*   **Yaşam Döngüsü Hook'ları:** Yükleme sonrası (`@PostLoad`) veya reload sırasında (`@OnReload`) özel kodlar çalıştırın.

## 📦 Kurulum

Bu kütüphane tek bir paket altında toplanmıştır. `com.bentahsin.configuration` paketini projenizin `src/main/java` dizinine kopyalamanız yeterlidir.

## 🛠 Kullanım

### 1. Config Sınıfını Oluşturun
Standart bir Java sınıfı oluşturun ve annotasyonlarla süsleyin.

```java
import com.bentahsin.configuration.annotation.*;
import com.bentahsin.configuration.converter.impl.*;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Arrays;

@ConfigHeader({"BenTahsin Plugin Config", "Version 1.0"})
@ConfigVersion(1)
@Backup(enabled = true, onFailure = true, onMigration = true)
public class MainConfig {

    @Comment("Plugin aktif mi?")
    public boolean enabled = true;

    @Comment("Sunucu ismi (Regex kontrolü var)")
    @Validate(notNull = true, pattern = "^[a-zA-Z0-9 ]*$")
    public String serverName = "Survival Sunucusu";

    @Comment("AFK Süresi (Örn: 1h, 30m, 100s)")
    @Transform(TimeConverter.class)
    public long afkTime = 300; // Saniye olarak tutulur

    @Comment("Başlangıç Eşyası")
    @Transform(ItemStackConverter.class)
    public ItemStack starterItem; // Config'den otomatik yüklenir

    @Comment("Spawn Noktası")
    @Transform(LocationConverter.class)
    public Location spawnPoint;

    @ConfigPath("database.settings") // Özel yol belirtme
    public Database db = new Database();

    // İç sınıflar MUTLAKA static olmalıdır
    public static class Database {
        public String host = "localhost";
        public int port = 3306;
    }
}
```

### 2. Kütüphaneyi Başlatın (Main Class)

```java
public class MyPlugin extends JavaPlugin {
    
    private Configuration configManager;
    private MainConfig mainConfig;

    @Override
    public void onEnable() {
        // Manager'ı başlat
        this.configManager = new Configuration(this);
        
        // Config nesnesini oluştur
        this.mainConfig = new MainConfig();
        
        // Yükle (Dosya yoksa oluşturur, varsa yükler)
        this.configManager.init(mainConfig, "config.yml");
    }
    
    public void reload() {
        // Reload işlemi (Verileri sıfırlar ve tekrar yükler)
        this.configManager.reload(mainConfig, "config.yml");
    }
}
```

## 📚 Annotasyon Rehberi

| Annotasyon | Hedef | Açıklama |
| :--- | :--- | :--- |
| `@ConfigPath("yol")` | Field | Değişkenin YAML dosyasındaki yolunu belirler. (Örn: `settings.general.name`) |
| `@Comment("mesaj")` | Field | Config dosyasına yorum satırı ekler. |
| `@Validate` | Field | Veri doğrulaması yapar (Min, Max, Regex, NotNull). Hatalı veri girilirse yüklenmez. |
| `@Transform(Class)` | Field | Özel bir dönüştürücü (Converter) kullanır. |
| `@Ignore` | Field | Bu değişkenin config dosyasına kaydedilmesini engeller. |
| `@ConfigHeader` | Class | Dosyanın en üstüne başlık/açıklama ekler. |
| `@ConfigVersion(int)` | Class | Dosya sürümünü takip eder. Sürüm artarsa migration tetikler. |
| `@Backup` | Class | Hata veya sürüm değişikliğinde dosyanın yedeğini alır. |
| `@PostLoad` | Method | Config yüklendikten hemen sonra çalışacak metodu belirler. |
| `@OnReload` | Method | `/reload` atıldığında çalışacak metodu belirler. |

## 🔄 Dönüştürücüler (Converters)

Kütüphane içinde hazır gelen güçlü dönüştürücüler:

*   **`ItemStackConverter`**:
    *   **Destek:** Enchantments, ItemFlags, Unbreakable, CustomModelData (1.14+), Hex Colors (1.16+), PotionData (1.9+), Leather Armor Colors, Skull Owners.
    *   **Güvenlik:** Eski sürümlerde (1.8) çalışırken yeni özellikler (Hex, Potion) hata vermez, yoksayılır.
*   **`TimeConverter`**:
    *   String (`"1h 30m 10s"`) <-> Long (Saniye) dönüşümü yapar.
*   **`LocationConverter`**:
    *   `"world,x,y,z,yaw,pitch"` formatında saklar. Dünya yüklenmemişse `null` döner, çökmez.
*   **`CuboidConverter`**:
    *   İki lokasyon arasındaki bölgeyi saklar. Başlangıç ve Bitiş noktalarının sırasını korur (Vektör/Yön işlemleri için uygundur).

## ⚠️ Önemli Notlar

1.  **Constructor:** Config sınıflarınızın parametresiz bir yapıcı metodu (`public Config() {}`) olmalıdır.
2.  **Inner Classes:** İç içe sınıf kullanıyorsanız `public static class` olmak zorundadır.
3.  **Final:** Config alanları `final` olmamalıdır.

## 📝 Lisans

Bu proje açık kaynaklıdır. Kendi projelerinizde özgürce kullanabilir ve değiştirebilirsiniz.

---
*Developed by bentahsin*