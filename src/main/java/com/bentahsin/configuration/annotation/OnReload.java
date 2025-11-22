package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Config reload edildiğinde çalıştırılacak metodları işaretler.
 * Örn: Veritabanı bağlantısını yenilemek veya cache temizlemek için.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnReload {}