package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A no-argument method of a {@link PlayerScoped} bean called before the instance is dropped: on the player's quit and on
 * server shutdown. Persistent player storage is still loaded at that moment.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
