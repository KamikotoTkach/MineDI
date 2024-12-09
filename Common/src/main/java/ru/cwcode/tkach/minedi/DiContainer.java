package ru.cwcode.tkach.minedi;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.data.BeanDependency;
import ru.cwcode.tkach.minedi.exception.CircularDependencyException;
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
  Map<Class<?>, BeanData> beans = new HashMap<>();
  Map<Class<?>, Object> singletons = new HashMap<>();
  
  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;
    
    this.singletons.put(this.getClass(), this);
    this.singletons.put(application.getClass(), application);
    this.beans.put(this.getClass(), new BeanData(Set.of()));
    this.beans.put(application.getClass(), new BeanData(Set.of()));
  }
  
  public <T> Optional<T> get(Class<T> type) {
    Object value = singletons.get(type);
    
    if (value == LAZY_OBJECT) {
      return Optional.of(createOrGet(type));
    }
    
    return Optional.ofNullable((T) value);
  }
  
  public boolean isBean(Class<?> clazz) {
    return beans.containsKey(clazz);
  }
  
  public void registerSingleton(Object bean, Class<?> as) {
    this.beans.put(as, new BeanData(Set.of()));
    this.singletons.put(as, bean);
  }
  
  public <T> T create(Class<T> clazz) {
    boolean added = creatingStack.get().add(clazz);
    if (!added) {
      throw new CircularDependencyException(clazz, creatingStack.get());
    }
    
    T created = create0(clazz);
    
    creatingStack.get().remove(clazz);
    
    return created;
  }
  
  public <T> T createOrGet(Class<T> clazz) {
    Object alreadyCreated = singletons.get(clazz);
    if (alreadyCreated != null && alreadyCreated != LAZY_OBJECT) return (T) alreadyCreated;
    
    return create(clazz);
  }
  
  public BeanData getData(Class<?> clazz) {
    return beans.get(clazz);
  }
  
  public void updateBean(Class<?> clazz, Object newObject) {
    BeanData beanData = beans.get(clazz);
    if (beanData == null) return;
    
    beanData.getBeanFields().forEach((field, origin) -> {
      try {
        field.setAccessible(true);
        field.set(origin, newObject);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    });
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
        createOrGet(clazz);
      } else {
        singletons.put(clazz, LAZY_OBJECT);
      }
    });
    
    singletons.keySet().forEach(this::populateFields);
  }
  
  private <T> T create0(Class<T> clazz) {
    BeanData data = beans.get(clazz);
    
    T instance = application.getBeanConstructors().construct(clazz, data);
    
    populateFields(instance);
    singletons.put(instance.getClass(), instance);
    
    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    
    return instance;
  }
  
  private void populateFields(Object instance) {
    if (instance == LAZY_OBJECT) return;
    
    ReflectionUtils.getFields(instance.getClass()).stream()
                   .filter(x -> isBean(x.getType()))
                   .sorted(Comparator.comparingInt(x -> x.isAnnotationPresent(Required.class) ? 0 : 1)) //first required
                   .forEach(x -> {
                     if (isBeanPopulated(instance)) {
                       singletons.put(instance.getClass(), instance);
                     }
                     
                     try {
                       x.setAccessible(true);
                       
                       Object val = x.get(instance);
                       
                       if (val == null) {
                         x.set(instance, val = createOrGet(x.getType()));
                       }
                       
                       beans.get(val.getClass()).getBeanFields().put(x, instance);
                     } catch (IllegalAccessException e) {
                       throw new RuntimeException(e);
                     }
                   });
  }
  
  private boolean isBeanPopulated(Object object) {
    return ReflectionUtils.getFields(object.getClass()).stream()
                          .filter(x -> isBean(x.getType()) && x.isAnnotationPresent(Required.class))
                          .allMatch(field -> {
                            try {
                              field.setAccessible(true);
                              return field.get(object) != null;
                            } catch (IllegalAccessException e) {
                              throw new RuntimeException(e);
                            }
                          });
  }
  
  private void registerComponent(Class<?> clazz) {
    Set<Annotation> annotations = ReflectionUtils.getAnnotations(clazz);
    
    if (annotations.stream().anyMatch(x -> x.annotationType().equals(Component.class))
        && clazz.getDeclaredConstructors().length == 1) {
      beans.put(clazz, new BeanData(annotations));
    }
  }
  
  private void searchForDependencies(Class<?> clazz, BeanData beanData) {
    List<BeanDependency> fieldDependencies = ReflectionUtils.getFields(clazz.getDeclaredFields()).stream()
                                                            .filter(x -> isBean(x.getType()))
                                                            .map(x -> new BeanDependency(x.getType(), x.isAnnotationPresent(Required.class)))
                                                            .toList();
    
    List<BeanDependency> constructorDependencies = Arrays.stream(clazz.getDeclaredConstructors()[0].getParameterTypes())
                                                         .filter(this::isBean)
                                                         .map(x -> new BeanDependency(x, true))
                                                         .toList();
    
    Set<BeanDependency> distinctDependencies = new HashSet<>(fieldDependencies);
    distinctDependencies.addAll(constructorDependencies);
    
    beanData.setDependencies(new ArrayList<>(distinctDependencies));
  }
}
