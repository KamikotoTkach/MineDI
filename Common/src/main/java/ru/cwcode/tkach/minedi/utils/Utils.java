package ru.cwcode.tkach.minedi.utils;

import ru.cwcode.tkach.minedi.data.BeanDependency;

import java.util.Set;

public class Utils {
  public static String getClassNameWithHash(Object object) {
    if (object == null) return "null";
    return object.getClass().getSimpleName() + "@" + object.hashCode();
  }
  
  public static String toString(Set<BeanDependency> dependencies) {
    return String.join(", ",dependencies.stream().map(BeanDependency::toString).toList());
  }
}
