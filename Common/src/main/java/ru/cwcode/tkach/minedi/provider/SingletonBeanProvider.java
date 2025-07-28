package ru.cwcode.tkach.minedi.provider;

import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.BeanScope;
import ru.cwcode.tkach.minedi.DiContainer;
import ru.cwcode.tkach.minedi.data.BeanData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SingletonBeanProvider implements BeanProvider {
  private final DiContainer diContainer;
  Map<Class<?>, Object> singletons = new HashMap<>();
  
  public SingletonBeanProvider(DiContainer diContainer) {
    this.diContainer = diContainer;
  }
  
  @Override
  public Set<Class<?>> getBeanClasses() {
    return singletons.keySet();
  }
  
  @Override
  public String scope() {
    return BeanScope.SINGLETON;
  }
  
  @Override
  public @Nullable Object provide(BeanData beanData) {
    Object value = singletons.get(beanData.getClazz());
    
    if (value == null) {
      Object instance = diContainer.create(beanData.getClazz());
      
      singletons.put(beanData.getClazz(), instance);
      
      return instance;
    }
    
    return value;
  }
  
  public void set(Class<?> clazz, Object object) {
    singletons.put(clazz, object);
  }
  
  public <T> boolean isLazy(Class<T> clazz) {
    Object inst = singletons.get(clazz);
    return inst != null && inst.equals(DiContainer.getLazyObject());
  }
}
