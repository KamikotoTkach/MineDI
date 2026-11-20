package ru.cwcode.tkach.minedi.annotation;

import ru.cwcode.tkach.minedi.scope.Creation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A component with one instance per online player. The instance receives its player, singletons and other beans of the
 * same player; it is dropped when the player leaves, after its {@link PreDestroy} method or {@link AutoCloseable#close()}.
 * A singleton reaches it through {@code PlayerScope<T>}, never by a direct field.
 */
@Component
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PlayerScoped {
  Creation create() default Creation.LAZY;
}
