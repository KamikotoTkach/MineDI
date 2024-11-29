package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.minedi.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
  String path() default "";
}
