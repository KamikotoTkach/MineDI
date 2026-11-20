package ru.cwcode.tkach.minedi.extensionpoint;

import ru.cwcode.tkach.minedi.annotation.Priority;

import java.util.Comparator;

/**
 * Order of beans in injected collections: {@link Priority} (lower first, 0 when absent), then class name.
 */
public final class BeanOrder {
  public static final Comparator<Object> COMPARATOR = Comparator.comparingInt((Object bean) -> priority(bean.getClass()))
                                                                .thenComparing(bean -> className(bean.getClass()));
  private static final String PROXY_MARKER = "$ByteBuddy$";

  private BeanOrder() {
  }

  /**
   * The name of the bean class, not of its proxy.
   */
  static String className(Class<?> type) {
    String name = type.getName();
    int proxy = name.indexOf(PROXY_MARKER);
    return proxy < 0 ? name : name.substring(0, proxy);
  }

  public static int priority(Class<?> type) {
    for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
      Priority priority = current.getAnnotation(Priority.class);
      if (priority != null) return priority.value();
    }
    return 0;
  }
}
