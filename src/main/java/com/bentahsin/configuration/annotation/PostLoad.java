package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config verileri dosyadan sınıfa yüklendikten hemen sonra
 * otomatik olarak çalıştırılması gereken metodları işaretler.
 * <p>
 * <b>Kullanım Senaryoları:</b>
 * <ul>
 *     <li>Renk kodlarını (String) ChatColor objesine çevirmek (& -> §).</li>
 *     <li>Veri performansını artırmak için Listeleri, Set veya Map'e dönüştürmek.</li>
 *     <li>İki farklı ayar arasındaki mantıksal çelişkileri kontrol etmek.</li>
 *     <li>Yüklenen verilere göre cache (önbellek) temizlemek.</li>
 * </ul>
 * <p>
 * <b>Not:</b> Bu annotasyona sahip metodlar parametre almamalıdır (void method()).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostLoad {}