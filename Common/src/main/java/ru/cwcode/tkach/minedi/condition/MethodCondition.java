package ru.cwcode.tkach.minedi.condition;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.DefineCondition;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A condition backed by a static {@link DefineCondition} method.
 */
public class MethodCondition implements Condition {
  @Getter
  private final String owner;
  private final String name;
  private final Method method;

  private MethodCondition(String owner, String name, Method method) {
    this.owner = owner;
    this.name = name;
    this.method = method;
  }

  /**
   * @throws BeanDefinitionException when an annotated method is not static or does not return boolean
   */
  public static List<MethodCondition> of(String owner, Class<?> clazz) {
    List<MethodCondition> result = new ArrayList<>();

    for (Method method : clazz.getDeclaredMethods()) {
      DefineCondition definition = method.getAnnotation(DefineCondition.class);
      if (definition == null) continue;

      String signature = signature(method);
      if (!Modifier.isStatic(method.getModifiers())) {
        throw new BeanDefinitionException("%s: condition method %s must be static: it may run before the plugin's beans exist"
                                            .formatted(owner, signature));
      }
      if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
        throw new BeanDefinitionException("%s: condition method %s must return boolean".formatted(owner, signature));
      }

      method.setAccessible(true);
      String name = definition.value().isBlank() ? method.getName() : definition.value();
      result.add(new MethodCondition(owner, name, method));
    }

    return result;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean process(String value) {
    Object[] arguments = arguments(value == null ? "" : value.strip());

    Object result;
    try {
      result = method.invoke(null, arguments);
    } catch (InvocationTargetException e) {
      throw new BeanDefinitionException("%s: condition %s(%s) threw %s".formatted(owner, name, value, e.getCause()));
    } catch (IllegalAccessException e) {
      throw new BeanDefinitionException("%s: condition %s is not accessible: %s".formatted(owner, signature(method), e));
    }

    if (result == null) {
      throw new BeanDefinitionException("%s: condition %s(%s) returned null".formatted(owner, name, value));
    }
    return (Boolean) result;
  }

  private Object[] arguments(String raw) {
    Class<?>[] types = method.getParameterTypes();
    if (types.length == 0) {
      if (!raw.isEmpty()) throw usage(raw, "takes no arguments");
      return new Object[0];
    }

    if (types.length == 1 && types[0] == String.class) return new Object[]{raw};

    List<String> parts = raw.isEmpty() ? List.of() : Arrays.stream(raw.split(",", -1)).map(String::strip).toList();
    boolean varargs = method.isVarArgs() && types[types.length - 1] == String[].class;
    int fixed = varargs ? types.length - 1 : types.length;

    if (varargs ? parts.size() < fixed : parts.size() != fixed) {
      throw usage(raw, "expects %s%d argument(s), got %d".formatted(varargs ? "at least " : "", fixed, parts.size()));
    }

    Object[] arguments = new Object[types.length];
    for (int i = 0; i < fixed; i++) {
      arguments[i] = convert(parts.get(i), types[i], raw);
    }
    if (varargs) {
      arguments[fixed] = parts.subList(fixed, parts.size()).toArray(String[]::new);
    }
    return arguments;
  }

  private Object convert(String part, Class<?> type, String raw) {
    try {
      if (type == String.class) return part;
      if (type == Integer.class || type == int.class) return Integer.parseInt(part);
      if (type == Long.class || type == long.class) return Long.parseLong(part);
      if (type == Double.class || type == double.class) return Double.parseDouble(part);
      if (type == Boolean.class || type == boolean.class) {
        if (part.equalsIgnoreCase("true")) return true;
        if (part.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("not a boolean");
      }
      if (type.isEnum()) {
        return Arrays.stream(type.getEnumConstants())
                     .filter(constant -> ((Enum<?>) constant).name().equalsIgnoreCase(part))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("one of " + Arrays.toString(type.getEnumConstants()) + " expected"));
      }
      if (type == Class.class) return Class.forName(part, false, method.getDeclaringClass().getClassLoader());
    } catch (IllegalArgumentException | ClassNotFoundException e) {
      throw usage(raw, "cannot convert '%s' to %s (%s)".formatted(part, type.getSimpleName(), e.getMessage()));
    }

    throw usage(raw, "parameter type %s is not supported".formatted(type.getName()));
  }

  private BeanDefinitionException usage(String raw, String problem) {
    return new BeanDefinitionException("%s: condition %s(%s) %s, method %s".formatted(owner, name, raw, problem, signature(method)));
  }

  private static String signature(Method method) {
    String parameters = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "));
    return method.getDeclaringClass().getName() + "." + method.getName() + "(" + parameters + ")";
  }

  @Override
  public String toString() {
    return signature(method) + " of " + owner;
  }
}
