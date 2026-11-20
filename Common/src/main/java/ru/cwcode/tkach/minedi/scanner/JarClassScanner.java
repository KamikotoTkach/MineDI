package ru.cwcode.tkach.minedi.scanner;

import lombok.Builder;
import lombok.Builder.Default;
import ru.cwcode.tkach.minedi.annotation.ConfigSubtypes;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Builder
public class JarClassScanner implements ClassScanner {
  File jar;
  String packageName;
  Predicate<String> filter;
  ClassLoader classLoader;
  BiConsumer<String, Throwable> loadFailureListener;
  @Default
  List<Predicate<String>> classNameFilters = new ArrayList<>();
  @Default
  List<Predicate<String>> unconditionalClassNameFilters = new ArrayList<>();

  @Override
  public void addClassNameFilter(Predicate<String> filter) {
    classNameFilters.add(filter);
  }

  @Override
  public void addUnconditionalClassNameFilter(Predicate<String> filter) {
    unconditionalClassNameFilters.add(filter);
  }

  @Override
  public Set<Class<?>> scan() {
    Predicate<String> allowed = className -> isAllowed(className) && unconditionalClassNameFilters.stream().allMatch(f -> f.test(className));
    if (loadFailureListener == null) return ReflectionUtils.getClasses(jar, packageName, allowed, classLoader);

    return ReflectionUtils.getClasses(jar, packageName, allowed, classLoader, loadFailureListener);
  }

  @Override
  public ScanResult scanResult() {
    ClassIndex index = ClassIndex.ofJar(jar, packageName);

    Set<String> unconditional = new HashSet<>();
    List<ScanResult.ConditionalClass> conditional = new ArrayList<>();
    List<String> definitions = new ArrayList<>();
    List<ScanResult.SubtypeCandidate> subtypes = new ArrayList<>();
    Map<String, Boolean> outsideBases = new HashMap<>();

    for (ClassIndex.Entry entry : index.entries()) {
      String name = entry.name();
      if (ClassIndex.isPackageInfo(name) || index.isStarterOrNested(name)) continue;

      List<String> conditions = index.conditionsOf(name);

      // the Integration suffix does not apply: reward *Integration classes are exactly such subtypes
      if (entry.concrete() && entry.typeName() != null && isAllowed(name)
          && index.extendsConfigSubtypesBase(name, outside -> outsideBases.computeIfAbsent(outside, this::isConfigSubtypesBase))) {
        subtypes.add(new ScanResult.SubtypeCandidate(name, conditions, entry.typeName()));
      }

      if (entry.definesConditions()) {
        if (!conditions.isEmpty()) {
          throw new BeanDefinitionException("%s defines conditions but is itself under %s: a condition cannot depend on a condition"
                                              .formatted(name, conditions));
        }
        definitions.add(name);
      }

      if (!isAllowed(name)) continue;

      if (!conditions.isEmpty()) {
        conditional.add(new ScanResult.ConditionalClass(name, conditions));
      } else if (unconditionalClassNameFilters.stream().allMatch(f -> f.test(name))) {
        unconditional.add(name);
      }
    }

    BiConsumer<String, Throwable> onFailure = loadFailureListener != null
                                              ? loadFailureListener
                                              : (className, error) -> {};

    return new ScanResult(ReflectionUtils.loadClasses(unconditional, classLoader, onFailure),
                          conditional,
                          definitions,
                          name -> {
                            Set<Class<?>> loaded = ReflectionUtils.loadClasses(Set.of(name), classLoader, onFailure);
                            return loaded.isEmpty() ? null : loaded.iterator().next();
                          },
                          subtypes);
  }

  /**
   * A supertype from another jar (a dependency): loaded without initialization; one that cannot be loaded belongs to
   * an absent soft-dependency and is not a base.
   */
  private boolean isConfigSubtypesBase(String className) {
    if (className.startsWith("java.") || className.startsWith("javax.")) return false;

    try {
      ClassLoader loader = classLoader != null ? classLoader : getClass().getClassLoader();
      return Class.forName(className, false, loader).isAnnotationPresent(ConfigSubtypes.class);
    } catch (ClassNotFoundException | LinkageError e) {
      return false;
    }
  }

  private boolean isAllowed(String className) {
    if (filter != null && !filter.test(className)) return false;

    for (Predicate<String> classNameFilter : classNameFilters) {
      if (!classNameFilter.test(className)) return false;
    }

    return true;
  }
}
