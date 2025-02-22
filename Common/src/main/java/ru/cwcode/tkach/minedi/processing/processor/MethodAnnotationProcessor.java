package ru.cwcode.tkach.minedi.processing.processor;

import lombok.SneakyThrows;
import revxrsal.asm.MethodCaller;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.EventHandlerImpl;
import ru.cwcode.tkach.minedi.processing.event.ApplicationEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

public class MethodAnnotationProcessor extends EventProcessor<BeanConstructedEvent> {
  
  private final EventHandlerImpl eventHandler;
  
  public MethodAnnotationProcessor(EventHandlerImpl eventHandler) {
    super(BeanConstructedEvent.class);
    this.eventHandler = eventHandler;
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    for (Method method : event.getBean().getClass().getDeclaredMethods()) {
      for (Annotation annotation : method.getAnnotations()) {
        if (annotation.annotationType().getName().startsWith("java.")) continue;
        if (annotation.annotationType().equals(EventListener.class)) {
          registerEventHandler(method, event);
        }
        handleBeanAnnotation(application, method, event.getBean());
        
        application.getEventHandler().handleEvent(new CustomMethodAnnotationEvent(event.getBean(), method, annotation));
      }
    }
  }
  
  @SneakyThrows
  private void handleBeanAnnotation(DiApplication application, Method x, Object bean) {
    Bean beanAnnotation = x.getAnnotation(Bean.class);
    if (beanAnnotation == null) return;
    
    String condition = beanAnnotation.condition();
    if (condition == null || application.getConditionParser().parse(condition)) {
      
      Object result = x.invoke(bean);
      
      if (result != null) {
        Class<?> as = beanAnnotation.as() == Object.class ? result.getClass() : beanAnnotation.as();
        application.getContainer().registerSingleton(result, as);
      }
    }
  }
  private void registerEventHandler(Method method, BeanConstructedEvent event) {
    Parameter[] parameters = method.getParameters();
    if (parameters.length != 1) return;
    
    Class<?> type = parameters[0].getType();
    
    method.setAccessible(true);
    
    if (ApplicationEvent.class.isAssignableFrom(type)) {
      eventHandler.getApplicationEventListeners().computeIfAbsent((Class<? extends ApplicationEvent>) type, __ -> new ArrayList<>())
                  .add(MethodCaller.wrap(method).bindTo(event.getBean()));
    }
  }
}
