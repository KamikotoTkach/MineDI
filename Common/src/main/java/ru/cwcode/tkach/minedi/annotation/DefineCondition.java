package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Turns a static method into a condition usable in {@link Conditional} and {@link Bean#condition()} of every MineDI
 * plugin on the server, no depend needed:
 * <pre>
 * {@literal @}DefineCondition
 * public static boolean serverType(String prefix) { ... }
 * </pre>
 * The name is the method name unless given here. Arguments are split by commas and converted to the parameter types:
 * String, Integer/int, Long/long, Double/double, Boolean/boolean, an enum (by name, any case) or Class; a trailing
 * {@code String...} takes the rest, a single String parameter gets the whole text. The method must return boolean and
 * depend only on the environment, never on its plugin's beans: it may run before that plugin starts.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefineCondition {
  String value() default "";
}
