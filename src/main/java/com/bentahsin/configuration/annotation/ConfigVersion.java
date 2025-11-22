package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config dosyasının versiyonunu belirler.
 * Eğer dosyadaki "config-version" değeri bu değerden düşükse,
 * kütüphane eksik ayarları dosyaya ekler ve versiyonu günceller.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigVersion {
    int value() default 1;
}