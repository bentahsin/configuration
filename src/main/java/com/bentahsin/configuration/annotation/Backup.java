package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Backup {
    /**
     * Yedekleme sistemini açıp kapatır.
     */
    boolean enabled() default true;

    /**
     * Yedeklerin saklanacağı klasör yolu.
     * Eklenti klasörü içindeki konumu belirtir.
     */
    String path() default "backups";

    /**
     * Config yüklenirken YAML hatası (Syntax Error) alınırsa
     * bozuk dosyanın yedeği alınsın mı?
     */
    boolean onFailure() default true;

    /**
     * Config versiyonu değiştiğinde (Migration)
     * eski config dosyasının yedeği alınsın mı?
     */
    boolean onMigration() default true;
}