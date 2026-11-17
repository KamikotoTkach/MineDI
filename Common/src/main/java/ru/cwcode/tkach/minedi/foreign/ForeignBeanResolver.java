package ru.cwcode.tkach.minedi.foreign;

import ru.cwcode.tkach.minedi.data.ForeignOwner;

import java.util.Optional;

/**
 * Finds the application of another plugin that owns a class, so its beans can be injected without a manual registerReference.
 */
@FunctionalInterface
public interface ForeignBeanResolver {
  ForeignBeanResolver NONE = type -> Optional.empty();

  Optional<ForeignOwner> ownerOf(Class<?> type);
}
