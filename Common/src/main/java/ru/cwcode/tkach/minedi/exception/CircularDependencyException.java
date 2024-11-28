package ru.cwcode.tkach.minedi.exception;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CircularDependencyException extends RuntimeException {
  final List<Class<?>> stack;
 
  public <T> CircularDependencyException(Class<T> clazz, LinkedHashSet<Class<?>> classes) {
    this.stack = new ArrayList<>(classes);
    this.stack.add(clazz);
  }
  
  @Override
  public String getMessage() {
    return "Circular dependency detected: [%s]".formatted(stack.stream()
                                                               .map(Class::getSimpleName)
                                                               .reduce("", (acc, c) -> acc + " <- " + c).substring(4));
  }
}
