package ru.cwcode.tkach.minedi;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;
import ru.cwcode.tkach.minedi.data.BeanDependency;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.*;

public class DiContainer {
  private static final Object LAZY_OBJECT = new Object();
  final ThreadLocal<LinkedHashSet<Class<?>>> creatingStack = ThreadLocal.withInitial(LinkedHashSet::new);
  final DiApplication application;
  
  ClassScanner scanner;
  Set<Class<?>> classes;
  HashMap<Class<?>, BeanData> beans = new HashMap<>();
  HashMap<Class<?>, Object> singletons = new HashMap<>();
  
  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;
  }
  
  public <T> Optional<T> get(Class<T> type) {
    Object value = singletons.get(type);
    
    if (value == LAZY_OBJECT) {
      return Optional.of(create(type));
    }
    
    return Optional.ofNullable((T) value);
  }
  
  public boolean isBean(Class<?> clazz) {
    return beans.containsKey(clazz);
  }
  
  public void registerSingleton(Object bean, Class<?> as) {
    singletons.put(as, bean);
  }
  
  public <T> T create(Class<T> clazz) {
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
  
  public BeanData getData(Class<?> clazz) {
    return beans.get(clazz);
  }
  
  protected void registerComponents() {
    classes.forEach(this::registerComponent);
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
    
    singletons.keySet().forEach(this::populateFields);
  }
  
  private <T> T create0(Class<T> clazz) {
    BeanData data = beans.get(clazz);
    
    T instance = (T) application.getBeanConstructors().construct(clazz, data);
    
    singletons.put(clazz, instance);
    
    try {
      populateFields(instance);
    } catch (Exception e) {
      singletons.remove(clazz);
      throw new RuntimeException(e);
    }
    
    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    
    return instance;
  }
  
  private void populateFields(Object instance) {
    if (instance == LAZY_OBJECT) return;
    
    Arrays.stream(instance.getClass().getDeclaredFields())
          .filter(x -> isBean(x.getType()))
          .forEach(x -> {
            try {
              x.setAccessible(true);
              if (x.get(instance) != null) return;
              
              x.set(instance, create(x.getType()));
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          });
  }
  
  private void registerComponent(Class<?> clazz) {
    Set<Annotation> annotations = ReflectionUtils.getAnnotations(clazz);
    
    if (annotations.stream().anyMatch(x -> x.annotationType().equals(Component.class))) {
      beans.put(clazz, new BeanData(annotations));
    }
  }
  
  private void searchForDependencies(Class<?> clazz, BeanData beanData) {
    List<BeanDependency> dependencies = Arrays.stream(clazz.getDeclaredFields())
                                              .filter(x -> isBean(x.getType()))
                                              .map(x -> new BeanDependency(x.getType(), x.isAnnotationPresent(Required.class)))
                                              .toList();
    
    beanData.setDependencies(dependencies);
  }
}
