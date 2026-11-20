package ru.cwcode.tkach.minedi.scope;

import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Fills player maps and sets of an object built without the container, as a unit test builds a service with {@code new}.
 */
public final class KeyedCollections {
  private KeyedCollections() {
  }

  /**
   * Sets every null {@link KeyedMap}/{@link KeyedSet} field of {@code target} to a new instance following {@code windows}.
   */
  public static <T> T fill(T target, PlayerWindows windows) {
    for (Field field : ReflectionUtils.getFields(target.getClass())) {
      Class<?> type = field.getType();
      if (Modifier.isStatic(field.getModifiers())) continue;
      if (!KeyedMap.class.isAssignableFrom(type) && !KeyedSet.class.isAssignableFrom(type)) continue;

      try {
        field.setAccessible(true);
        if (field.get(target) == null) field.set(target, type.getDeclaredConstructor(PlayerWindows.class).newInstance(windows));
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot fill " + field, e);
      }
    }
    return target;
  }
}
