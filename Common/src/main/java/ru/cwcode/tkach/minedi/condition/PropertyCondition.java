package ru.cwcode.tkach.minedi.condition;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * {@code property(PartyConfig.enabled)} or {@code property(PartyConfig.combat.enabled)} — a boolean field of a bean of this
 * plugin, usually a config. The class is its simple or full name; it is read once, when the plugin starts.
 */
public class PropertyCondition implements Condition {
  private final DiApplication application;

  public PropertyCondition(DiApplication application) {
    this.application = application;
  }

  @Override
  public String name() {
    return "property";
  }

  @Override
  public boolean process(String value) {
    String[] path = value.strip().split("\\.");
    if (path.length < 2) throw new IllegalArgumentException("property(BeanClass.field) expected");

    Object current = null;
    int fieldStart = -1;
    for (int i = path.length - 1; i >= 1 && current == null; i--) {
      String className = String.join(".", Arrays.copyOfRange(path, 0, i));
      Class<?> clazz = application.getContainer().findScannedClass(className);
      if (clazz != null) {
        current = application.getContainer().getForCondition(clazz);
        fieldStart = i;
      }
    }
    if (current == null) {
      throw new IllegalArgumentException("no scanned class for '%s'; a class under a condition cannot be used here".formatted(value));
    }

    for (int i = fieldStart; i < path.length; i++) {
      current = read(current, path[i], value);
    }

    if (current instanceof Boolean result) return result;
    throw new IllegalArgumentException("'%s' is %s, a boolean expected".formatted(value, current == null ? "null" : current.getClass().getSimpleName()));
  }

  private static Object read(Object target, String name, String value) {
    if (target == null) throw new IllegalArgumentException("'%s' passes through null before %s".formatted(value, name));

    for (Field field : ReflectionUtils.getFields(target.getClass())) {
      if (!field.getName().equals(name)) continue;
      try {
        field.setAccessible(true);
        return field.get(target);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("cannot read " + name + ": " + e.getMessage());
      }
    }

    throw new IllegalArgumentException("%s has no field %s".formatted(target.getClass().getName(), name));
  }
}
