package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.EventHandlerImpl;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class MethodAnnotationProcessor extends EventProcessor<BeanConstructedEvent> {
  
  private final EventHandlerImpl eventHandler;
  
  public MethodAnnotationProcessor(EventHandlerImpl eventHandler) {
    super(BeanConstructedEvent.class);
    this.eventHandler = eventHandler;
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    Class<?> clazz = event.getBean().getClass();
    
    while (clazz != Object.class && clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        for (Annotation annotation : method.getAnnotations()) {
          if (annotation.annotationType().getName().startsWith("java.")) continue;
          if (annotation.annotationType().equals(EventListener.class)) {
            eventHandler.registerApplicationEventListener(event.getBean(), method);
          }
          application.getEventHandler().handleEvent(new CustomMethodAnnotationEvent(event.getBean(), method, annotation));
        }
      }
      clazz = clazz.getSuperclass();
    }

    BeanMethodInvoker.invokeAll(application, event.getBean());
  }
}
