package ru.cwcode.tkach.minedi.scanner;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @param classes              loaded classes without conditions
 * @param conditional          classes guarded by conditions, not loaded yet
 * @param conditionDefinitions classes declaring {@link ru.cwcode.tkach.minedi.annotation.DefineCondition} methods, not loaded yet
 * @param loader               loads a class by name; null when it cannot be loaded (the failure is already reported)
 */
public record ScanResult(Set<Class<?>> classes,
                         List<ConditionalClass> conditional,
                         List<String> conditionDefinitions,
                         Function<String, @Nullable Class<?>> loader) {

  public static ScanResult unconditional(Set<Class<?>> classes) {
    return new ScanResult(classes, List.of(), List.of(), name -> null);
  }

  public record ConditionalClass(String name, List<String> conditions) {
  }
}
