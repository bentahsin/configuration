package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigPath {

    /**
     * Config dosyasındaki yol. Örn: "settings.general.enabled"
     * Eğer boş bırakılırsa değişken adı kullanılır.
     */
    String value() default "";
}