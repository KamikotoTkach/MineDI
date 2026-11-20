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
 * @param subtypes             classes that extend a {@link ru.cwcode.tkach.minedi.annotation.ConfigSubtypes} base, not loaded yet
 */
public record ScanResult(Set<Class<?>> classes,
                         List<ConditionalClass> conditional,
                         List<String> conditionDefinitions,
                         Function<String, @Nullable Class<?>> loader,
                         List<SubtypeCandidate> subtypes) {

  public ScanResult(Set<Class<?>> classes,
                    List<ConditionalClass> conditional,
                    List<String> conditionDefinitions,
                    Function<String, @Nullable Class<?>> loader) {
    this(classes, conditional, conditionDefinitions, loader, List.of());
  }

  public static ScanResult unconditional(Set<Class<?>> classes) {
    return new ScanResult(classes, List.of(), List.of(), name -> null);
  }

  public record ConditionalClass(String name, List<String> conditions) {
  }

  /**
   * @param conditions empty when the class is not guarded
   * @param typeName   its {@code @JsonTypeName}
   */
  public record SubtypeCandidate(String name, List<String> conditions, String typeName) {
  }
}
