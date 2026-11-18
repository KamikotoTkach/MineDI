package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Loads the class only when the condition holds, e.g. {@code @Conditional("hasPlugin(Activities) && !serverType(hub)")}.
 * The scanner reads it from bytecode, so a false condition never loads the class and its missing types stay harmless.
 * <p>
 * On a {@code package-info} it covers the package and every nested package; nested classes follow their outer class.
 * A class with this annotation is scanned even when its name ends with {@code Integration}. It does not make the class a
 * bean: {@code @Service}/{@code @Component} still do.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Conditional {
  String value();
}
