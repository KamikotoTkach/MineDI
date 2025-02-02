package ru.cwcode.tkach.minedi;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.exception.CircularDependencyException;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.provider.BeanProvider;
import ru.cwcode.tkach.minedi.provider.SingletonBeanProvider;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class DiContainer {
  private static final Object LAZY_OBJECT = new Object();
  final ThreadLocal<LinkedHashSet<Class<?>>> creatingStack = ThreadLocal.withInitial(LinkedHashSet::new);
  @Getter
  final DiApplication application;
  
  ClassScanner scanner;
  Set<Class<?>> classes;
  Map<Class<?>, BeanData> beans = new HashMap<>();
  Map<String, BeanProvider> beanProviders = new HashMap<>();
  Map<Class<?>, List<Field>> staticFields = new HashMap<>();
  
  public static Object getLazyObject() {
    return LAZY_OBJECT;
  }
  
  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;
    
    registerProvider(new SingletonBeanProvider(this));
    
    registerSingleton(this, this.getClass());
    registerSingleton(application, application.getClass());
  }
  
  public void registerProvider(BeanProvider beanProvider) {
    beanProviders.put(beanProvider.scope(), beanProvider);
  }
  
  public <T> Optional<T> get(Class<T> type) {
    BeanData beanData = beans.get(type);
    if (beanData == null) return Optional.empty();
    
    BeanProvider beanProvider = beanProviders.get(beanData.getScope());
    if (beanProvider == null) return Optional.empty(); //todo throw exception?
    
    return Optional.ofNullable(type.cast(beanProvider.provide(beanData)));
  }
  
  public boolean isBean(Class<?> clazz) {
    return beans.containsKey(clazz);
  }
  
  public void registerSingleton(Object bean, Class<?> as) {
    BeanData value = new BeanData(as, this);
    this.beans.put(as, value);
    
    value.searchForDependencies();
    
    singletonBeanProvider().set(as, bean);
    injectBeanInStaticFields(as);
    
    application.getEventHandler().handleEvent(new BeanConstructedEvent(bean));
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
    BeanData beanData = beans.get(clazz);
    
    return clazz.cast(beanProviders.get(beanData.getScope()).provide(beanData));
  }
  
  private SingletonBeanProvider singletonBeanProvider() {
    return (SingletonBeanProvider) beanProviders.get(BeanScope.SINGLETON);
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
    
    injectBeanInStaticFields(clazz);
  }
  
  
  protected void registerBeans() {
    application.getLogger().info("Registering components");
    classes.forEach(this::registerComponent);
    
    application.getLogger().info("Searching beans dependencies");
    beans.values().forEach(BeanData::searchForDependencies);
    
    beans.forEach((clazz, data) -> injectBeanInStaticFields(clazz));
    
    constructSingletons();
  }
  
  private void scanForStaticFields() {
    application.getLogger().info("Scanning for static fields");
    
    for (Class<?> clazz : classes) {
      for (Field declaredField : clazz.getDeclaredFields()) {
        if (Modifier.isStatic(declaredField.getModifiers())) {
          staticFields.computeIfAbsent(declaredField.getType(), (e) -> new ArrayList<>())
                      .add(declaredField);
        }
      }
    }
  }
  
  private void injectBeanInStaticFields(Class<?> clazz) {
    application.getLogger().info("Injecting " + clazz + " in static fields");
    
    for (Field field : staticFields.getOrDefault(clazz, List.of())) {
      
      field.setAccessible(true);
      try {
        if (field.get(null) == null) {
          field.set(null, get(clazz).orElseThrow());
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  protected void scanClasses() {
    application.getLogger().info("Scanning classes...");
    classes = scanner.scan();
    application.getLogger().info("Founded " + classes.size() + " classes");
    
    scanForStaticFields();
  }
  
  public BeanProvider getBeanProvider(String scope) {
    return beanProviders.get(scope);
  }
  
  private void constructSingletons() {
    application.getLogger().info("Constructing singletons");
    
    SingletonBeanProvider singletonBeanProvider = (SingletonBeanProvider) getBeanProvider(BeanScope.SINGLETON);
    
    beans.forEach((clazz, data) -> {
      if (!data.getScope().equals(BeanScope.SINGLETON)) return;
      
      if (!data.isAnnotationPresent(Lazy.class)) {
        createOrGet(clazz);
      }
    });
    
    singletonBeanProvider.getBeanClasses().forEach(this::populateFields);
  }
  
  private <T> T create0(Class<T> clazz) {
    BeanData data = beans.get(clazz);
    
    T instance = application.getBeanConstructors().construct(clazz, data);
    
    populateFields(instance);
    
    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    
    return instance;
  }
  
  public void populateFields(Object instance) {
    ReflectionUtils.getFields(instance.getClass()).stream()
                   .filter(x -> isBean(x.getType()))
                   .sorted(Comparator.comparingInt(x -> x.isAnnotationPresent(Required.class) ? 0 : 1)) //first required
                   .forEach(field -> {
                     BeanData beanData = beans.get(instance.getClass());
                     if (beanData != null && beanData.getScope().equals(BeanScope.SINGLETON) && isBeanPopulated(instance)) {
                       singletonBeanProvider().set(instance.getClass(), instance);
                     }
                     
                     try {
                       field.setAccessible(true);
                       
                       Object fieldValue = field.get(instance);
                       
                       if (fieldValue == null) {
                         field.set(instance, createOrGet(field.getType()));
                       }
                       
                       beans.get(field.getType()).getBeanFields().put(field, instance); //todo clean fields of removed objects
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
  
  public void registerComponent(Class<?> clazz) {
    registerBean(clazz, false);
  }
  
  public void registerBean(Class<?> clazz, boolean ignoreComponentRequiredAnnotation) {
    if (validateBean(clazz)) {
      BeanData value = new BeanData(clazz, this);
      if (ignoreComponentRequiredAnnotation || !value.isAnnotationPresent(Component.class)) {
        application.getLogger().info("Class " + clazz.getSimpleName() + " ignored");
        return;
      }
      
      beans.put(clazz, value);
    }
  }
  
  public boolean validateBean(Class<?> clazz) {
    return clazz.getDeclaredConstructors().length == 1;
  }
}
