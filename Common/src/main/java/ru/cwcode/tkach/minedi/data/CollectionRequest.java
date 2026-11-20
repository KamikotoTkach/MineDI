package ru.cwcode.tkach.minedi.data;

import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.extensionpoint.Extensions;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A field or constructor parameter asking for several beans: {@code List<T>}, {@code Set<T>}, {@code Collection<T>} or
 * {@code Extensions<T>}.
 *
 * @param container the declared collection type
 * @param element   {@code T}; {@code ? extends T} reads as {@code T}
 */
public record CollectionRequest(Class<?> container, Class<?> element) {
  private static final Set<Class<?>> CONTAINERS = Set.of(List.class, Set.class, Collection.class, Extensions.class);

  /**
   * @return empty for a type that is not one of the collection types or whose element type cannot be read
   * @throws BeanDefinitionException for a raw {@code Extensions}
   */
  public static @Nullable CollectionRequest of(Class<?> raw, Type generic, String requiredBy) {
    if (!CONTAINERS.contains(raw)) return null;

    Class<?> element = generic instanceof ParameterizedType parameterized ? elementClass(parameterized.getActualTypeArguments()[0]) : null;
    if (element == null) {
      if (raw == Extensions.class) {
        throw new BeanDefinitionException("%s: Extensions needs its element type, as in Extensions<MyExtensionPoint>".formatted(requiredBy));
      }
      return null;
    }

    return new CollectionRequest(raw, element);
  }

  public boolean isSet() {
    return container == Set.class;
  }

  public boolean isExtensions() {
    return container == Extensions.class;
  }

  private static @Nullable Class<?> elementClass(Type type) {
    if (type instanceof Class<?> clazz) return clazz;
    if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> clazz) return clazz;
    if (type instanceof WildcardType wildcard && wildcard.getLowerBounds().length == 0) {
      Type[] upper = wildcard.getUpperBounds();
      return upper.length == 1 && upper[0] != Object.class ? elementClass(upper[0]) : null;
    }
    return null;
  }
}
