package ru.cwcode.tkach.minedi.utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CollectionUtils {
  public static @NotNull Optional<?> findObjectWithType(List<?> objects, Class<?> type) {
    return objects.stream()
                  .filter(type::isInstance)
                  .findFirst();
  }
  
  public static <T> String toString(Iterable<T> values, String prefix, String suffix, boolean removeLastSuffix) {
    
    StringBuilder sb = new StringBuilder();
    for (T value : values) {
      sb.append(prefix).append(value).append(suffix);
    }
    
    if (removeLastSuffix && sb.length() >= suffix.length()) sb.setLength(sb.length() - suffix.length());
    return sb.toString();
  }
}
