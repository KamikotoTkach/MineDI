package ru.cwcode.tkach.minedi;

import org.jetbrains.annotations.NotNull;
import ru.cwcode.tkach.minedi.annotation.*;
import ru.cwcode.tkach.minedi.data.BeanDependency;
import ru.cwcode.tkach.minedi.data.ComponentData;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DiContainer {
  final ThreadLocal<LinkedHashSet<Class<?>>> creatingStack = ThreadLocal.withInitial(LinkedHashSet::new);
  private final Object LAZY_OBJECT = new Object();
  
  ClassScanner scanner;
  Set<Class<?>> classes;
  HashMap<Class<?>, ComponentData> beans = new HashMap<>();
  HashMap<Class<?>, Object> singletons = new HashMap<>();
  
  public DiContainer(ClassScanner scanner) {
    this.scanner = scanner;
  }
  
  protected void registerComponents() {
    classes.forEach(this::registerBean);
    beans.forEach(this::searchForDependencies);
    this.constructSingletons();
  }
  
  protected void scanClasses() {
    classes = scanner.scan();
  }
  
  private void constructSingletons() {
    beans.forEach((clazz, data) -> {
      if (!data.isAnnotationPresent(Lazy.class)) {
        create(clazz);
      } else {
        singletons.put(clazz, LAZY_OBJECT);
      }
    });
    singletons.forEach((k, v) -> pupulateFields(k, beans.get(k)));
  }
  
  private <T> T create(Class<T> clazz) {
    Object alreadyCreated = singletons.get(clazz);
    if (alreadyCreated != null && alreadyCreated != LAZY_OBJECT) return (T) alreadyCreated;
    
    boolean added = creatingStack.get().add(clazz);
    if (!added) {
      throw new RuntimeException("Circular dependency detected: [%s <- %s]".formatted(creatingStack.get().stream()
                                                                                                   .map(Class::getSimpleName)
                                                                                                   .reduce("", (acc, c) -> acc + " <- " + c),
                                                                                      clazz.getSimpleName()));
    }
    
    T created = create0(clazz);
    
    creatingStack.get().remove(clazz);
    
    return created;
  }
  
  private <T> T create0(Class<T> clazz) {
    ComponentData data = beans.get(clazz);
    
    List<?> dependencies = data.getDependencies().stream()
                               .filter(BeanDependency::isStartupRequired)
                               .map(x -> create(x.getClazz()))
                               .toList();
    
    T instance = createInstance(clazz, dependencies);
    
    singletons.put(clazz, instance);
    
    try {
      pupulateFields(instance, data);
    } catch (Exception e) {
      singletons.remove(clazz);
      throw new RuntimeException(e);
    }
    
    return instance;
  }
  
  private void pupulateFields(Object instance, ComponentData data) {
    if (instance == LAZY_OBJECT) return;
    
    Arrays.stream(instance.getClass().getDeclaredFields())
          .filter(x -> isBean(x.getType()))
          .forEach(x -> {
            Object value;
            if (data.isRequired(x.getType())) {
              value = get(x.getType()).orElseThrow(() -> new RuntimeException("No dependency found for '%s' while populating bean '%s'".formatted(instance.getClass(), x.getType())));
            } else {
              value = get(x.getType()).orElse(null);
            }
            
            if (value == null) return;
            
            try {
              x.setAccessible(true);
              Object currentValue = x.get(instance);
              if (value == null || currentValue != null) return;
              
              x.setAccessible(true);
              x.set(instance, value);
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          });
  }
  
  protected <T> Optional<T> get(Class<T> type) {
    Object value = getIfCreated(type);
    
    if (value == LAZY_OBJECT) {
      return Optional.of(create(type));
    }
    
    return Optional.ofNullable((T) value);
  }
  
  protected <T> T getIfCreated(Class<T> type) {
    return (T) singletons.get(type);
  }
  
  private <T> T createInstance(Class<T> clazz, List<?> providedDependencies) {
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Object[] parameters = new Object[parameterTypes.length];
    
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      parameters[i] = findDependency(providedDependencies, parameterType)
        .orElseThrow(() -> new RuntimeException("No dependency found for '%s' while constructing bean '%s'".formatted(parameterType, clazz.getName())));
    }
    
    try {
      return (T) constructor.newInstance(parameters);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
  
  private void registerBean(Class<?> clazz) {
    Set<Annotation> annotations = ReflectionUtils.getAnnotations(clazz);
    
    if (annotations.stream().anyMatch(x -> x.annotationType().equals(Component.class))) {
      beans.put(clazz, new ComponentData(annotations));
    }
  }
  
  private void searchForDependencies(Class<?> clazz, ComponentData componentData) {
    List<BeanDependency> dependencies = Arrays.stream(clazz.getDeclaredFields())
                                              .filter(x -> isBean(x.getType()))
                                              .map(x -> new BeanDependency(x.getType(), x.isAnnotationPresent(Required.class)))
                                              .toList();
    
    componentData.setDependencies(dependencies);
  }
  
  private boolean isBean(Class<?> clazz) {
    return beans.containsKey(clazz);
  }
  
  private static @NotNull Optional<?> findDependency(List<?> dependencies, Class<?> type) {
    return dependencies.stream()
                       .filter(type::isInstance)
                       .findFirst();
  }
}
