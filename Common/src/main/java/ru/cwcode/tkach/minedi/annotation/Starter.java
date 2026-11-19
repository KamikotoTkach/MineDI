package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link ru.cwcode.tkach.minedi.starter.StarterExtension} that every MineDI application with {@link #plugin()} in
 * its {@code depend}/{@code softdepend} receives before its beans are created:
 * <pre>
 * {@literal @}Starter(plugin = "Infrastructure")
 * public class DatabaseStarter implements StarterExtension {
 *   public void apply(StarterContext context) { … }
 * }
 * </pre>
 * Found in the jar of any MineDI plugin (and of MineDI itself) by reading bytecode; the class is loaded only for a consumer
 * that depends on {@link #plugin()}, so it may refer to types of that plugin. It needs a public no-arg constructor and is
 * never scanned as a component.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(Starters.class)
public @interface Starter {
  /**
   * The plugin whose consumers get the starter, compared ignoring case (Velocity ids are lowercase). Not necessarily the
   * plugin that ships the starter.
   */
  String plugin();
}
