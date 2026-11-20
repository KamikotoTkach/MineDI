package ru.cwcode.tkach.minedi.extension.velocity.scope;

import com.velocitypowered.api.event.Subscribe;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.processing.event.ComponentRegisteredEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A player bean on Velocity gets no event subscriptions, tasks or packet handlers: such a method would silently never
 * run, so it fails the start instead.
 */
public class PlayerScopedMethodsCheck extends EventProcessor<ComponentRegisteredEvent> {
  public PlayerScopedMethodsCheck() {
    super(ComponentRegisteredEvent.class);
  }

  @Override
  public void process(ComponentRegisteredEvent event, DiApplication application) {
    Class<?> type = event.component();
    if (!application.getContainer().getPlayerScope().isPlayerBean(type)) return;

    for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        for (Annotation annotation : method.getAnnotations()) {
          Class<? extends Annotation> annotationType = annotation.annotationType();
          boolean handled = annotationType == Subscribe.class || annotationType.getName().startsWith("ru.cwcode.");
          if (handled && annotationType != PreDestroy.class) {
            throw new BeanDefinitionException(("%s: @%s on %s#%s is not supported in a player bean on Velocity: it would never run. "
                                               + "Handle it in a singleton and reach the player's state through PlayerScope")
                                                .formatted(application.getName(), annotationType.getSimpleName(), clazz.getName(), method.getName()));
          }
        }
      }
    }
  }
}
