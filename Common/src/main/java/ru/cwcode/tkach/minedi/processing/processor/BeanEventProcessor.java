package ru.cwcode.tkach.minedi.processing.processor;

import lombok.extern.java.Log;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventHandler;
import ru.cwcode.tkach.minedi.processing.event.BeanEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

@Log
public class BeanEventProcessor extends EventProcessor<BeanEvent> {
  public BeanEventProcessor() {
    super(BeanEvent.class);
  }
  
  @Override
  public void process(BeanEvent event, DiApplication application) {
    Object bean = event.getBean();
    
    for (Method x : bean.getClass().getDeclaredMethods()) {
      if (x.isAnnotationPresent(EventHandler.class)) {
        Parameter[] parameters = x.getParameters();
        if (parameters.length != 1) continue;
        
        if (!event.getClass().equals(parameters[0].getType())) continue;
        
        try {
          x.setAccessible(true);
          x.invoke(bean, event);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
