package ru.cwcode.tkach.minedi.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Field;
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
  
  public static List<Field> getFields(Class<?> clazz) {
    if (clazz == null) return Collections.emptyList();
    
    List<Field> fields = new ArrayList<>();
    
    while (clazz != Object.class && clazz != null) {
      fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
      clazz = clazz.getSuperclass();
    }
    
    return fields;
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
    
    return annotations;
  }
}
