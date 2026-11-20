package ru.cwcode.tkach.minedi.extensionpoint;

import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.annotation.ConfigSubtypes;
import ru.cwcode.tkach.minedi.annotation.ExtensionPoint;
import ru.cwcode.tkach.minedi.logging.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Shared by all MineDI plugins: singletons implementing an {@link ExtensionPoint} and subtypes of
 * {@link ConfigSubtypes} bases, whichever plugin published them.
 */
public class ExtensionRegistry {
  public static final ExtensionRegistry GLOBAL = new ExtensionRegistry();

  private final List<Object> published = new CopyOnWriteArrayList<>();
  private final List<LiveExtensions<?>> views = new CopyOnWriteArrayList<>();
  private final Map<Class<?>, List<Class<?>>> extensionPoints = new ConcurrentHashMap<>();

  private final List<ConfigSubtype> subtypes = new CopyOnWriteArrayList<>();
  private final List<Consumer<ConfigSubtype>> subtypeListeners = new CopyOnWriteArrayList<>();

  public static boolean isExtensionPoint(Class<?> type) {
    return type.isAnnotationPresent(ExtensionPoint.class);
  }

  /**
   * Extension points among the supertypes of {@code type}, itself included.
   */
  public List<Class<?>> extensionPointsOf(Class<?> type) {
    return extensionPoints.computeIfAbsent(type, key -> supertypes(key, ExtensionRegistry::isExtensionPoint));
  }

  /**
   * {@link ConfigSubtypes} bases among the strict supertypes of {@code type}.
   */
  public static List<Class<?>> configSubtypeBasesOf(Class<?> type) {
    return supertypes(type, supertype -> supertype != type && supertype.isAnnotationPresent(ConfigSubtypes.class));
  }

  /**
   * Adds a bean that implements an extension point; a bean already present is ignored.
   *
   * @return whether the bean was added
   */
  public synchronized boolean publish(Object bean) {
    if (extensionPointsOf(bean.getClass()).isEmpty() || containsInstance(bean)) return false;

    published.add(bean);
    views.stream().filter(view -> view.accepts(bean)).forEach(view -> view.added(bean));
    return true;
  }

  /**
   * A recreated bean takes the place of the old instance; listeners get the new one through {@link Extensions#onAdd}.
   */
  public synchronized void replace(@Nullable Object old, Object bean) {
    if (old != null && old != bean && removeInstance(old)) {
      views.stream().filter(view -> view.accepts(old)).forEach(LiveExtensions::removed);
    }
    publish(bean);
  }

  public synchronized void withdraw(Object bean) {
    if (removeInstance(bean)) {
      views.stream().filter(view -> view.accepts(bean)).forEach(LiveExtensions::removed);
    }
  }

  public synchronized <T> Extensions<T> view(Class<T> point, Log log) {
    LiveExtensions<T> view = new LiveExtensions<>(point, this, log);
    views.add(view);
    return view;
  }

  /**
   * A live, unmodifiable list over the extension point.
   */
  public <T> List<T> liveList(Class<T> point, Log log) {
    return ((LiveExtensions<T>) view(point, log)).asList();
  }

  public <T> Set<T> liveSet(Class<T> point, Log log) {
    return ((LiveExtensions<T>) view(point, log)).asSet();
  }

  List<Object> published() {
    return published;
  }

  /**
   * @return the subtype already registered under the same base and name by another class, if any; the new one is then
   * not added
   */
  public synchronized Optional<ConfigSubtype> addSubtype(ConfigSubtype subtype) {
    for (ConfigSubtype existing : subtypes) {
      if (existing.base() != subtype.base() || !existing.name().equals(subtype.name())) continue;
      return existing.type() == subtype.type() ? Optional.empty() : Optional.of(existing);
    }

    subtypes.add(subtype);
    subtypeListeners.forEach(listener -> listener.accept(subtype));
    return Optional.empty();
  }

  /**
   * Called for every subtype already published, then for each published later.
   */
  public synchronized void onSubtype(Consumer<ConfigSubtype> listener) {
    subtypeListeners.add(listener);
    subtypes.forEach(listener);
  }

  public List<ConfigSubtype> subtypes() {
    return List.copyOf(subtypes);
  }

  private boolean containsInstance(Object bean) {
    return published.stream().anyMatch(existing -> existing == bean);
  }

  private boolean removeInstance(Object bean) {
    return published.removeIf(existing -> existing == bean);
  }

  private static List<Class<?>> supertypes(Class<?> type, Predicate<Class<?>> filter) {
    List<Class<?>> found = new ArrayList<>();
    Set<Class<?>> seen = new HashSet<>();
    Deque<Class<?>> queue = new ArrayDeque<>();
    queue.add(type);

    while (!queue.isEmpty()) {
      Class<?> current = queue.poll();
      if (current == Object.class || !seen.add(current)) continue;

      try {
        if (filter.test(current)) found.add(current);
        if (current.getSuperclass() != null) queue.add(current.getSuperclass());
        queue.addAll(List.of(current.getInterfaces()));
      } catch (LinkageError | TypeNotPresentException e) {
        // a supertype of an absent soft-dependency: it cannot be an extension point of a running plugin
      }
    }
    return List.copyOf(found);
  }
}
