package ru.cwcode.tkach.minedi.processing;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import revxrsal.asm.MethodCaller;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.ApplicationEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanDestroyEvent;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.BeanSelfEventsProcessor;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.processing.processor.MethodAnnotationProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class EventHandlerImpl implements EventHandler {
  List<EventProcessor<? extends Event>> processors = new LinkedList<>();
  DiApplication application;
  @Getter
  Map<Class<? extends ApplicationEvent>, List<BoundEventListener>> applicationEventListeners = new HashMap<>();
  
  public EventHandlerImpl(DiApplication application) {
    this.application = application;
    
    processors.add(new MethodAnnotationProcessor(this));
    processors.add(new BeanSelfEventsProcessor());
    
    registerApplicationEventListener(this);
  }
  
  @Override
  public void registerProcessor(EventProcessor<? extends Event> processor) {
    processors.add(processor);
  }
  
  @Override
  public void registerApplicationEventListener(Object bean) {
    for (Method method : bean.getClass().getDeclaredMethods()) {
      if (method.isAnnotationPresent(EventListener.class)) registerApplicationEventListener(bean, method);
    }
  }
  
  @Override
  public void registerApplicationEventListener(Object bean, Method method) {
    Parameter[] parameters = method.getParameters();
    if (parameters.length != 1) return;
    
    Class<?> type = parameters[0].getType();
    
    method.setAccessible(true);
    
    if (ApplicationEvent.class.isAssignableFrom(type)) {
      getEventListeners((Class<? extends ApplicationEvent>) type)
        .add(new BoundEventListener(bean, MethodCaller.wrap(method).bindTo(bean)));
    }
  }
  
  @Override
  public <E extends Event> void handleEvent(E event) {
    application.getLogger().info("Handling event: " + event);
    
    if (event instanceof ApplicationEvent applicationEvent) {
      applicationEventListeners.getOrDefault(applicationEvent.getClass(), List.of())
                               .forEach(boundMethodCaller -> boundMethodCaller.caller().call(event));
      return;
    }
    
    for (EventProcessor<? extends Event> processor : processors) {
      processor.tryProcess(event, application);
    }
  }
  
  private @NotNull List<BoundEventListener> getEventListeners(Class<? extends ApplicationEvent> type) {
    return getApplicationEventListeners().computeIfAbsent(type, __ -> new ArrayList<>());
  }
  
  @EventListener
  public void onBeanDestroy(BeanDestroyEvent event) {
    applicationEventListeners.values().forEach(boundEventListeners -> {
      boundEventListeners.removeIf(boundEventListener -> boundEventListener.bean().equals(event.bean()));
    });
  }
}
