package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic config type open to every plugin: a concrete class of any MineDI plugin that extends it and carries
 * Jackson {@code @JsonTypeName("name")} is registered as its subtype in the config mapper of every plugin, before the
 * first config of that plugin is read. Found in bytecode, so a {@code *Integration} class counts as well; a
 * {@link Conditional} on the subtype is respected.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigSubtypes {
}
