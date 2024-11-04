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
}
