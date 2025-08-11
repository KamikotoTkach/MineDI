package ru.cwcode.tkach.minedi.utils;

public class Utils {
  public static String getClassNameWithHash(Object object) {
    if (object == null) return "null";
    return object.getClass().getSimpleName() + "@" + object.hashCode();
  }
}
