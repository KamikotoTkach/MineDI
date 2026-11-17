package ru.cwcode.tkach.minedi.constructor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.data.BeanDependency;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;
import ru.cwcode.tkach.minedi.utils.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class BeanConstructorImpl implements BeanConstructor {
  @Override
  public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    
    Class<?>[] parameterTypes = constructor.getParameterTypes();
    Object[] parameters = new Object[parameterTypes.length];
    
    for (int i = 0; i < parameterTypes.length; i++) {
      parameters[i] = application.getContainer().createOrGet(parameterTypes[i]);
    }

    try {
      T bean = (T) constructor.newInstance(parameters);
      BeanCreatedEvent event = new BeanCreatedEvent(bean);
      application.getEventHandler().handleEvent(event);

      return (T) event.getBean();
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Constructor of bean " + clazz.getName() + " failed", e.getCause());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Cannot construct bean " + clazz.getName(), e);
    }
  }
}
