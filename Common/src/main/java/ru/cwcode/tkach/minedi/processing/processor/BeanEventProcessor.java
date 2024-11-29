package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.BeanEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class BeanEventProcessor extends EventProcessor<BeanEvent> {
  public BeanEventProcessor() {
    super(BeanEvent.class);
  }
  
  @Override
  public void process(BeanEvent event, DiApplication application) {
    Object bean = event.getBean();
    
    for (Method x : bean.getClass().getDeclaredMethods()) {
      if (x.isAnnotationPresent(EventListener.class)) {
        Parameter[] parameters = x.getParameters();
        if (parameters.length != 1) continue;
        
        if (!event.getClass().equals(parameters[0].getType())) continue;
        
        try {
          x.setAccessible(true);
          x.invoke(bean, event); //todo use MethodCaller/LambdaMetafactory/MethodHandlers + cache
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
