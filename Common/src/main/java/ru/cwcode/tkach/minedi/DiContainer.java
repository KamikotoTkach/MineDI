package ru.cwcode.tkach.minedi;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.exception.CircularDependencyException;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanDestroyEvent;
import ru.cwcode.tkach.minedi.processing.event.ComponentRegisteredEvent;
import ru.cwcode.tkach.minedi.provider.BeanProvider;
import ru.cwcode.tkach.minedi.provider.SingletonBeanProvider;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DiContainer {
  private static final Object LAZY_OBJECT = new Object();
  final ThreadLocal<LinkedHashSet<Class<?>>> creatingStack = ThreadLocal.withInitial(LinkedHashSet::new);
  @Getter
  final DiApplication application;
  
  ClassScanner scanner;
  Set<Class<?>> classes;
  Map<Class<?>, BeanData> beans = new ConcurrentHashMap<>();
  Map<String, BeanProvider> beanProviders = new ConcurrentHashMap<>();
  Map<Class<?>, List<Field>> staticFields = new ConcurrentHashMap<>();
  
  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;
    
    registerProvider(new SingletonBeanProvider(this));
    
    registerSingleton(this, this.getClass());
    registerSingleton(application, application.getClass());
  }
  
  public static Object getLazyObject() {
    return LAZY_OBJECT;
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
    if (!beans.containsKey(as)) {
      BeanData beanData = new BeanData(as, this);
      this.beans.put(as, beanData);
      
      beanData.searchForDependencies();
    }
    
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
  
  public <T> T recreate(Class<T> clazz, @Nullable Object bean) {
    BeanData beanData = beans.get(clazz);
    
    BeanProvider beanProvider = getBeanProvider(beanData.getScope());
    if (beanProvider.getBeanClasses().contains(clazz)) {
      application.getEventHandler().handleEvent(new BeanDestroyEvent(beanProvider.provide(beanData)));
    }
    
    if (bean == null) {
      bean = create(clazz);
    } else {
      populateBeanFields(bean);
      application.getEventHandler().handleEvent(new BeanConstructedEvent(bean));
    }
    
    Object finalBean = bean;
    beanData.getBeanFields().forEach((field, origin) -> {
      try {
        field.setAccessible(true);
        field.set(origin, finalBean);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    });
    
    if (beanProvider instanceof SingletonBeanProvider sbp) {
      sbp.set(clazz, bean);
    }
    
    injectBeanInStaticFields(clazz);
    
    return (T) bean;
  }
  
  public <T> T createOrGet(Class<T> clazz) {
    BeanData beanData = beans.get(clazz);
    
    return clazz.cast(beanProviders.get(beanData.getScope()).provide(beanData));
  }
  
  public BeanData getData(Class<?> clazz) {
    return beans.get(clazz);
  }
  
  public BeanProvider getBeanProvider(String scope) {
    return beanProviders.get(scope);
  }
  
  public void populateExternalObject(Object instance) {
    ReflectionUtils.getFields(instance.getClass()).stream()
                   .filter(x -> isBean(x.getType()))
                   
                   .forEach(field -> {
                     try {
                       field.setAccessible(true);
                       
                       Object fieldValue = field.get(instance);
                       
                       if (fieldValue == null) {
                         field.set(instance, createOrGet(field.getType()));
                       }
                     } catch (Exception e) {
                       e.printStackTrace();
                     }
                   });
  }
  
  public void populateBeanFields(Object instance) {
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
      application.getEventHandler().handleEvent(new ComponentRegisteredEvent(clazz));
    }
  }
  
  public void registerBean(Class<?> clazz, Object bean) {
    BeanData value = new BeanData(clazz, this);
    beans.put(clazz, value);
    
    singletonBeanProvider().set(clazz, bean);
    
    application.getEventHandler().handleEvent(new ComponentRegisteredEvent(clazz));
  }
  
  public boolean validateBean(Class<?> clazz) {
    return clazz.getDeclaredConstructors().length == 1;
  }
  
  protected void registerBeans() {
    application.getLogger().info("Registering components");
    classes.forEach(this::registerComponent);
    
    application.getLogger().info("Searching beans dependencies");
    beans.values().forEach(BeanData::searchForDependencies);
    
    beans.forEach((clazz, data) -> injectBeanInStaticFields(clazz));
    
    constructSingletons();
  }
  
  protected void scanClasses() {
    application.getLogger().info("Scanning classes...");
    classes = scanner.scan();
    application.getLogger().info("Founded " + classes.size() + " classes");
    
    scanForStaticFields();
  }
  
  private SingletonBeanProvider singletonBeanProvider() {
    return (SingletonBeanProvider) beanProviders.get(BeanScope.SINGLETON);
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
    application.getLogger().info("Injecting {} in static fields", clazz);
    
    for (Field field : staticFields.getOrDefault(clazz, List.of())) {
      
      try {
        field.setAccessible(true);
        
        if (Modifier.isFinal(field.getModifiers())) continue;
        
        Object toInject = get(clazz).orElseThrow();
        
        application.getLogger().info("Injecting {} to static field {} of {}", toInject.getClass(), field.getName(), field.getDeclaringClass());
        
        field.set(null, toInject);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
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
    
    singletonBeanProvider.getBeanClasses().forEach(this::populateBeanFields);
  }
  
  private <T> T create0(Class<T> clazz) {
    BeanData data = beans.get(clazz);
    
    T instance = application.getBeanConstructors().construct(clazz, data);
    
    if (data.getScope().equals(BeanScope.SINGLETON)) { //todo catch by events
      ((SingletonBeanProvider) getBeanProvider(BeanScope.SINGLETON)).set(clazz, instance);
    }
    
    populateBeanFields(instance);
    
    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    
    return instance;
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
}
