package ru.cwcode.tkach.minedi.constructor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.data.BeanDependency;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.utils.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class BeanConstructorImpl implements BeanConstructor {
  @Override
  public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
    
    List<?> dependencies = data.getDependencies().stream()
                               .filter(BeanDependency::isStartupRequired)
                               .map(x -> application.getContainer().createOrGet(x.getClazz()))
                               .toList();
    
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Object[] parameters = new Object[parameterTypes.length];
    
    for (int i = 0; i < parameterTypes.length; i++) {
      Class<?> parameterType = parameterTypes[i];
      parameters[i] = CollectionUtils.findObjectWithType(dependencies, parameterType)
                                     .orElseThrow(() -> new RuntimeException("No dependency found for '%s' while constructing bean '%s'".formatted(parameterType, clazz.getName())));
    }
    
    try {
      return (T) constructor.newInstance(parameters);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
