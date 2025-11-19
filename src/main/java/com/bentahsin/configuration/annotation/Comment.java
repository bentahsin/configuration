package com.bentahsin.configuration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {

    /**
     * Yorum satırları. Çoklu satır için {"Satır 1", "Satır 2"} şeklinde girilebilir.
     */
    String[] value();
}