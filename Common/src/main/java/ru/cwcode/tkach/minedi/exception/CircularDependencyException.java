package ru.cwcode.tkach.minedi.exception;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CircularDependencyException extends RuntimeException {
  final List<Class<?>> stack;
  final String applicationName;

  public <T> CircularDependencyException(Class<T> clazz, LinkedHashSet<Class<?>> classes) {
    this(clazz, classes, null);
  }

  public <T> CircularDependencyException(Class<T> clazz, LinkedHashSet<Class<?>> classes, String applicationName) {
    this.stack = new ArrayList<>(classes);
    this.stack.add(clazz);
    this.applicationName = applicationName;
  }

  @Override
  public String getMessage() {
    String chain = stack.stream().map(Class::getName).collect(Collectors.joining(" -> "));
    String prefix = applicationName == null ? "" : applicationName + ": ";
    return prefix + "Circular dependency detected: [" + chain + "]. Break it with @Lazy or a field instead of a constructor parameter";
  }
}
