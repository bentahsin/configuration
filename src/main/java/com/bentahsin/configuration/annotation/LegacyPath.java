package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bir ayarın eski (eski sürümdeki) yolunu belirtir.
 * Eğer yeni yol config dosyasında bulunamazsa, sistem buraya bakar.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LegacyPath {
    String value();
}