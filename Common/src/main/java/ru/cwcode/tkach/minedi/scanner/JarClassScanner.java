package ru.cwcode.tkach.minedi.scanner;

import lombok.Builder;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;

@Builder
public class JarClassScanner implements ClassScanner {
  File jar;
  String packageName;
  Predicate<String> filter;
  
  @Override
  public Set<Class<?>> scan() {
    return ReflectionUtils.getClasses(jar, packageName, filter);
  }
}
