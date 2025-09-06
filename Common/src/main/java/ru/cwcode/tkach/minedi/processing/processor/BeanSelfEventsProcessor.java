package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.BeanEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class BeanSelfEventsProcessor extends EventProcessor<BeanEvent> {
  public BeanSelfEventsProcessor() {
    super(BeanEvent.class);
  }
  
  @Override
  public void process(BeanEvent event, DiApplication application) {
    Object bean = event.getBean();
    
    for (Method x : bean.getClass().getDeclaredMethods()) {
      handleEvent(event, x, bean);
    }
  }
  
  
  private void handleEvent(BeanEvent event, Method x, Object bean) {
    if (x.isAnnotationPresent(EventListener.class)) {
      Parameter[] parameters = x.getParameters();
      if (parameters.length != 1) return;
      
      if (!event.getClass().equals(parameters[0].getType())) return;
      
      try {
        x.setAccessible(true);
        x.invoke(bean, event); //todo use MethodCaller/LambdaMetafactory/MethodHandlers + cache
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }
}