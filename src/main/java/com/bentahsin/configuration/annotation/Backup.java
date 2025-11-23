package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Backup {
    /**
     * Enables or disables the backup system.
     */
    boolean enabled() default true;

    /**
     * The folder path where backups will be stored.
     * Specifies the location inside the plugin folder.
     */
    String path() default "backups";

    /**
     * If a YAML error (syntax error) occurs while loading the config,
     * should a backup of the corrupted file be taken?
     */
    boolean onFailure() default true;

    /**
     * When the config version changes (migration),
     * should a backup of the old config file be taken?
     */
    boolean onMigration() default true;
}