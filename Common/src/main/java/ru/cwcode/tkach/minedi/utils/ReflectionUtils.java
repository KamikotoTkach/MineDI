package ru.cwcode.tkach.minedi.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectionUtils {
  public static Set<Class<?>> getClasses(File jarFile, String packageName, Predicate<String> filter) {
    Set<Class<?>> classes = new HashSet<>();
    
    try {
      JarFile file = new JarFile(jarFile);
      
      for (Enumeration<JarEntry> entry = file.entries(); entry.hasMoreElements(); ) {
        
        JarEntry jarEntry = entry.nextElement();
        String name = jarEntry.getName().replace("/", ".");
        
        if (name.startsWith(packageName) && name.endsWith(".class") && filter.test(name)) {
          classes.add(Class.forName(name.substring(0, name.length() - 6)));
        }
      }
      
      file.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return classes;
  }
  
  public static Set<Annotation> getAnnotations(Class<?> clazz) {
    Set<Annotation> annotations = new HashSet<>(Arrays.asList(clazz.getAnnotations()));
    
    int size;
    
    do {
      size = annotations.size();
      List<Annotation> inherited = annotations.stream()
                                              .flatMap(x -> Arrays.stream(x.annotationType().getAnnotations()))
                                              .filter(x -> x.annotationType().isAnnotationPresent(Inherited.class))
                                              .filter(x -> !x.annotationType().getName().startsWith("java."))
                                              .toList();
      
      annotations.addAll(inherited);
    } while (size != annotations.size());
    
    System.out.println("Class %s contains [%s] annotations".formatted(clazz.getName(), annotations.stream()
                                                                                                  .map(x -> x.annotationType().getSimpleName())
                                                                                                  .reduce("", (a, b) -> a + " " + b)));
    
    return annotations;
  }
}
