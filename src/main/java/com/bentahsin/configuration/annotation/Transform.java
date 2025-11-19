package com.bentahsin.configuration.annotation;

import com.bentahsin.configuration.converter.Converter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Transform {
    Class<? extends Converter<?, ?>> value();
}