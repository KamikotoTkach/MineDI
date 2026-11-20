package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.cwutils.scheduler.Scheduler;
import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.PapiPlaceholder;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.Placeholder;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.PlaceholderAdapter;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.processing.event.ComponentRegisteredEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.scope.PlayerScopeProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registers the {@code @EventHandler}, {@code @Repeat} and {@code @Placeholder} methods of a player bean once per class:
 * each call goes to the instance of the event's or request's player, a task visits every created instance.
 */
public class PlayerScopedMethodsProcessor extends EventProcessor<ComponentRegisteredEvent> {
  private static final Set<Class<? extends Annotation>> SUPPORTED = Set.of(EventHandler.class, Repeat.class, Placeholder.class,
                                                                           PapiPlaceholder.class, PlayerOf.class, PreDestroy.class);

  private final PaperExtension extension;
  private final @Nullable PlaceholderAdapter placeholders;

  public PlayerScopedMethodsProcessor(PaperExtension extension, @Nullable PlaceholderAdapter placeholders) {
    super(ComponentRegisteredEvent.class);
    this.extension = extension;
    this.placeholders = placeholders;
  }

  @Override
  public void process(ComponentRegisteredEvent event, DiApplication application) {
    Class<?> type = event.component();
    PlayerScopeProvider scope = application.getContainer().getPlayerScope();
    if (!scope.isPlayerBean(type)) return;

    for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        rejectUnsupported(application, method);

        EventHandler handler = method.getAnnotation(EventHandler.class);
        if (handler != null) registerHandler(application, scope, type, method, handler);

        Repeat repeat = method.getAnnotation(Repeat.class);
        if (repeat != null) registerRepeat(application, scope, type, method, repeat);

        String placeholder = placeholderName(method);
        if (placeholder != null) registerPlaceholder(application, scope, type, method, placeholder);
      }
    }
  }

  private void rejectUnsupported(DiApplication application, Method method) {
    for (Annotation annotation : method.getAnnotations()) {
      Class<? extends Annotation> annotationType = annotation.annotationType();
      String name = annotationType.getName();
      boolean ours = name.startsWith("ru.cwcode.tkach.minedi.") || name.startsWith("ru.cwcode.cwutils.scheduler.");
      if (ours && !SUPPORTED.contains(annotationType)) {
        throw new BeanDefinitionException(("%s: @%s on %s#%s is not supported in a player bean (@PlayerScoped): it would never run. "
                                           + "Move the method to a singleton and reach the player's state through PlayerScope")
                                            .formatted(application.getName(), annotationType.getSimpleName(),
                                                       method.getDeclaringClass().getName(), method.getName()));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void registerHandler(DiApplication application, PlayerScopeProvider scope, Class<?> type, Method method, EventHandler handler) {
    if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
      throw new BeanDefinitionException("%s: @EventHandler %s#%s must take one event parameter"
                                          .formatted(application.getName(), type.getName(), method.getName()));
    }
    Class<? extends Event> eventType = (Class<? extends Event>) method.getParameterTypes()[0];
    PlayerOf playerOf = method.getAnnotation(PlayerOf.class);
    String role = playerOf == null ? "" : playerOf.value();
    method.setAccessible(true);

    // own extension beans exist only after the singletons are built, so the extractor is chosen at enable
    extension.addDelayedTask(() -> {
      List<PlayerExtractor> custom = application.getExtensionRegistry().liveList(PlayerExtractor.class, application.getLogger());
      PlayerExtractor<Event> extractor = (PlayerExtractor<Event>) PlayerExtractors.find(eventType, role, (List) custom);
      if (extractor == null) {
        throw new BeanDefinitionException(("%s: %s#%s(%s): no player for this event%s. Add a bean implementing PlayerExtractor<%s> "
                                           + "or handle the event in a singleton")
                                            .formatted(application.getName(), type.getName(), method.getName(), eventType.getSimpleName(),
                                                       role.isEmpty() ? "" : " in the role \"" + role + "\"", eventType.getSimpleName()));
      }

      Bukkit.getPluginManager().registerEvent(eventType, new Listener() {
      }, handler.priority(), (listener, fired) -> {
        if (!eventType.isInstance(fired)) return;

        Player player = extractor.extract(fired);
        if (player == null) return;

        UUID id = player.getUniqueId();
        if (!scope.inWindow(type, id)) return;

        invoke(application, method, scope.get(type, id), player, fired);
      }, extension.getPlugin(), handler.ignoreCancelled());
    });
  }

  private void registerRepeat(DiApplication application, PlayerScopeProvider scope, Class<?> type, Method method, Repeat repeat) {
    if (method.getParameterCount() != 0) {
      throw new BeanDefinitionException("%s: @Repeat %s#%s must take no parameters".formatted(application.getName(), type.getName(), method.getName()));
    }
    method.setAccessible(true);

    extension.addDelayedTask(() -> Scheduler.create()
                                            .async(repeat.async())
                                            .infinite()
                                            .perform(() -> scope.all(type).forEach(bean -> invoke(application, method, bean, null)))
                                            .register(extension.getPlugin(), repeat.delay()));
  }

  private void registerPlaceholder(DiApplication application, PlayerScopeProvider scope, Class<?> type, Method method, String identifier) {
    if (placeholders == null) return;
    method.setAccessible(true);

    placeholders.register(identifier, method.getParameterTypes(), (OfflinePlayer player, Object[] arguments) -> {
      if (player == null) return null;

      UUID id = player.getUniqueId();
      if (!scope.inWindow(type, id)) return null;

      return method.invoke(scope.get(type, id), arguments);
    });
  }

  private static @Nullable String placeholderName(Method method) {
    Placeholder placeholder = method.getAnnotation(Placeholder.class);
    if (placeholder != null) return placeholder.name();

    PapiPlaceholder papi = method.getAnnotation(PapiPlaceholder.class);
    return papi == null ? null : papi.name();
  }

  private static void invoke(DiApplication application, Method method, Object bean, @Nullable Player player, Object... arguments) {
    try {
      method.invoke(bean, arguments);
    } catch (InvocationTargetException e) {
      application.getLogger().log(LogLevel.ERROR, "{}#{} failed{}", method.getDeclaringClass().getName(), method.getName(),
                                  player == null ? "" : " for " + player.getName(), e.getCause());
    } catch (IllegalAccessException e) {
      application.getLogger().log(LogLevel.ERROR, "{}#{} is not accessible", method.getDeclaringClass().getName(), method.getName(), e);
    }
  }
}
