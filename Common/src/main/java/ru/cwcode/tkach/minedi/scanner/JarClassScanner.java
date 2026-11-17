package ru.cwcode.tkach.minedi.scanner;

import lombok.Builder;
import lombok.Builder.Default;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

  @Override
  public void addClassNameFilter(Predicate<String> filter) {
    classNameFilters.add(filter);
  }
  
  @Override
  public Set<Class<?>> scan() {
    if (loadFailureListener == null) return ReflectionUtils.getClasses(jar, packageName, this::isAllowed, classLoader);

    return ReflectionUtils.getClasses(jar, packageName, this::isAllowed, classLoader, loadFailureListener);
  }

  private boolean isAllowed(String className) {
    if (filter != null && !filter.test(className)) return false;

    for (Predicate<String> classNameFilter : classNameFilters) {
      if (!classNameFilter.test(className)) return false;
    }

    return true;
  }
}
