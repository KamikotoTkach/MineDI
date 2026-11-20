package ru.cwcode.tkach.minedi.extension.paper.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Which player of the event an {@code @EventHandler} of a player bean belongs to, by {@link PlayerExtractor#name()}:
 * {@code @PlayerOf("damager")} on an {@code EntityDamageByEntityEvent} handler runs it for the attacker.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PlayerOf {
  String value();
}
