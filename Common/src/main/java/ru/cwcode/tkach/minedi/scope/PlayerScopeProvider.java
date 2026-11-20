package ru.cwcode.tkach.minedi.scope;

import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.BeanScope;
import ru.cwcode.tkach.minedi.DiContainer;
import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.provider.BeanProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Instances of the {@link PlayerScoped} beans of one plugin, per player window of {@link PlayerWindows}.
 */
public class PlayerScopeProvider implements BeanProvider, PlayerWindows.WindowListener {
  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private final DiContainer container;
  private final Map<UUID, ScopeInstance> instances = new ConcurrentHashMap<>();
  private final Map<Class<?>, Creation> creations = new ConcurrentHashMap<>();
  private PlayerWindows windows = PlayerWindows.GLOBAL;

  public PlayerScopeProvider(DiContainer container) {
    this.container = container;
  }

  /**
   * The player whose bean is being created on this thread, or null outside a creation.
   */
  public static @Nullable UUID currentPlayer() {
    return CURRENT.get();
  }

  public void attach(PlayerWindows windows) {
    this.windows = windows;
    windows.addListener(this);
  }

  public PlayerWindows windows() {
    return windows;
  }

  @Override
  public Set<Class<?>> getBeanClasses() {
    return container.beanTypes().stream()
                    .filter(type -> BeanScope.PLAYER.equals(container.getData(type).getScope()))
                    .collect(Collectors.toSet());
  }

  @Override
  public String scope() {
    return BeanScope.PLAYER;
  }

  /**
   * A dependency of a bean of the player being created; anywhere else a player bean has no player to belong to.
   */
  @Override
  public Object provide(BeanData beanData) {
    UUID id = CURRENT.get();
    if (id == null) {
      Class<?> type = beanData.getClazz();
      throw new BeanDefinitionException(("%s: %s is a player bean (@PlayerScoped) and has no instance outside a player. "
                                         + "Inject PlayerScope<%s> and call get(player)")
                                          .formatted(container.getApplication().getName(), type.getName(), type.getSimpleName()));
    }
    return instance(id).getOrCreate(beanData.getClazz());
  }

  /**
   * @throws IllegalStateException when the player is outside the bean's window (not joined yet, left, not synced for
   *                               {@link Creation#SYNC})
   */
  public <T> T get(Class<T> type, UUID id) {
    requireBean(type);
    if (!inWindow(type, id)) {
      throw new IllegalStateException("%s: player %s has no %s now: the player is not online%s"
                                        .formatted(container.getApplication().getName(), id, type.getSimpleName(),
                                                   creation(type) == Creation.SYNC ? " or not synchronised yet" : ""));
    }
    return type.cast(instance(id).getOrCreate(type));
  }

  public <T> Optional<T> find(Class<T> type, UUID id) {
    ScopeInstance instance = instances.get(id);
    return instance == null ? Optional.empty() : Optional.ofNullable(type.cast(instance.existing(type)));
  }

  /**
   * The created instances of the type; a {@link Creation#LAZY} bean has none for players who never used it.
   */
  public <T> List<T> all(Class<T> type) {
    List<T> found = new ArrayList<>();
    for (ScopeInstance instance : instances.values()) {
      Object bean = instance.existing(type);
      if (bean != null) found.add(type.cast(bean));
    }
    return found;
  }

  public boolean inWindow(Class<?> type, UUID id) {
    return windows.isOpen(id, creation(type));
  }

  public boolean isPlayerBean(Class<?> type) {
    BeanData data = container.getData(type);
    return data != null && BeanScope.PLAYER.equals(data.getScope());
  }

  public Creation creation(Class<?> type) {
    return creations.computeIfAbsent(type, key -> {
      PlayerScoped annotation = key.getAnnotation(PlayerScoped.class);
      return annotation == null ? Creation.LAZY : annotation.create();
    });
  }

  @Override
  public void joined(UUID id, Object key) {
    createEager(id, creation -> creation == Creation.JOIN || creation == Creation.SYNC && !windows.hasSyncSource());
  }

  @Override
  public void synced(UUID id, Object key) {
    createEager(id, creation -> creation == Creation.SYNC);
  }

  @Override
  public void closed(UUID id) {
    ScopeInstance instance = instances.remove(id);
    if (instance != null) instance.close();
  }

  /**
   * Destroys every instance: the server stops without quit events.
   */
  public void closeAll() {
    List.copyOf(instances.keySet()).forEach(this::closed);
  }

  public void detach() {
    windows.removeListener(this);
  }

  private void createEager(UUID id, java.util.function.Predicate<Creation> matches) {
    for (Class<?> type : getBeanClasses()) {
      if (!matches.test(creation(type)) || !inWindow(type, id)) continue;

      try {
        get(type, id);
      } catch (RuntimeException e) {
        container.getApplication().getLogger().log(ru.cwcode.tkach.minedi.logging.LogLevel.ERROR, "Cannot create {} for player {}", type.getName(), id, e);
      }
    }
  }

  private void requireBean(Class<?> type) {
    if (!isPlayerBean(type)) {
      throw new BeanDefinitionException("%s: %s is not a @PlayerScoped bean of this plugin"
                                          .formatted(container.getApplication().getName(), type.getName()));
    }
  }

  private ScopeInstance instance(UUID id) {
    return instances.computeIfAbsent(id, ScopeInstance::new);
  }

  private final class ScopeInstance {
    private final UUID id;
    private final LinkedHashMap<Class<?>, Object> beans = new LinkedHashMap<>();
    private boolean closed;

    ScopeInstance(UUID id) {
      this.id = id;
    }

    synchronized @Nullable Object existing(Class<?> type) {
      return beans.get(type);
    }

    synchronized Object getOrCreate(Class<?> type) {
      if (closed) throw new IllegalStateException("Player " + id + " has left, " + type.getSimpleName() + " is gone");

      Object existing = beans.get(type);
      if (existing != null) return existing;

      UUID previous = CURRENT.get();
      CURRENT.set(id);
      Object bean;
      try {
        bean = container.create(type);
      } finally {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
      }
      beans.put(type, bean);

      // the player left while the bean was being made: nobody would destroy it later
      if (!windows.isOpen(id) && instances.remove(id, this)) close();

      return bean;
    }

    void close() {
      List<Object> destroyed;
      synchronized (this) {
        if (closed) return;
        closed = true;
        destroyed = new ArrayList<>(beans.values());
        beans.clear();
      }
      Collections.reverse(destroyed);
      destroyed.forEach(this::destroy);
    }

    private void destroy(Object bean) {
      try {
        Method preDestroy = preDestroyOf(bean.getClass());
        if (preDestroy != null) {
          preDestroy.setAccessible(true);
          preDestroy.invoke(bean);
        } else if (bean instanceof AutoCloseable closeable) {
          closeable.close();
        }
      } catch (InvocationTargetException e) {
        container.getApplication().getLogger().log(ru.cwcode.tkach.minedi.logging.LogLevel.ERROR, "Destroying {} of player {} failed", bean.getClass().getName(), id, e.getCause());
      } catch (Exception e) {
        container.getApplication().getLogger().log(ru.cwcode.tkach.minedi.logging.LogLevel.ERROR, "Destroying {} of player {} failed", bean.getClass().getName(), id, e);
      }
    }
  }

  private static @Nullable Method preDestroyOf(Class<?> type) {
    for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(PreDestroy.class) && method.getParameterCount() == 0) return method;
      }
    }
    return null;
  }
}
