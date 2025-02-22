package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class BeanSelfEventsProcessor extends EventProcessor<BeanConstructedEvent> {
  public BeanSelfEventsProcessor() {
    super(BeanConstructedEvent.class);
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    Object bean = event.getBean();
    
    for (Method x : bean.getClass().getDeclaredMethods()) {
      registerEventListener(event, x, bean);
    }
  }
  
  
  private void registerEventListener(BeanEvent event, Method x, Object bean) {
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