package ru.cwcode.tkach.minedi.scanner;

import java.util.Set;
import java.util.function.Predicate;

public interface ClassScanner {
  Set<Class<?>> scan();
  
  default void addClassNameFilter(Predicate<String> filter) {
    throw new UnsupportedOperationException("Class scanner does not support class name filters");
  }
}
