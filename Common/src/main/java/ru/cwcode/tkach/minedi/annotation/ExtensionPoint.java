package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type whose singleton implementations from every MineDI plugin are collected into one live view: a field or a
 * constructor parameter {@code Extensions<T>} (or {@code List<T>}/{@code Collection<T>}) of such a type sees the
 * implementations of plugins that started earlier at once and those of later plugins as they start.
 *
 * @see ru.cwcode.tkach.minedi.extensionpoint.Extensions
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionPoint {
}
