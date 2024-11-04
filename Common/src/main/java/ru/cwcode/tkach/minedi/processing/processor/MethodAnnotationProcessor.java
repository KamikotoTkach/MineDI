package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class MethodAnnotationProcessor extends EventProcessor<BeanConstructedEvent> {
  
  public MethodAnnotationProcessor() {
    super(BeanConstructedEvent.class);
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    for (Method method : event.getBean().getClass().getDeclaredMethods()) {
      for (Annotation annotation : method.getAnnotations()) {
        if (annotation.annotationType().getName().startsWith("java.")) continue;
        application.getEventHandler().handleEvent(new CustomMethodAnnotationEvent(event.getBean(), method, annotation));
      }
    }
  }
}
