package ru.cwcode.tkach.minedi.scanner;

import java.util.Set;
import java.util.function.Predicate;

public interface ClassScanner {
  Set<Class<?>> scan();

  /**
   * Like {@link #scan()}, but classes under {@link ru.cwcode.tkach.minedi.annotation.Conditional} are left unloaded
   * until their conditions are known. Scanners that cannot read bytecode load everything.
   */
  default ScanResult scanResult() {
    return ScanResult.unconditional(scan());
  }

  default void addClassNameFilter(Predicate<String> filter) {
    throw new UnsupportedOperationException("Class scanner does not support class name filters");
  }

  /**
   * A filter that skips only classes without conditions: an explicit condition overrides it.
   */
  default void addUnconditionalClassNameFilter(Predicate<String> filter) {
    addClassNameFilter(filter);
  }
}
