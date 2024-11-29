package ru.cwcode.tkach.minedi.processing;

import lombok.Getter;
import revxrsal.asm.BoundMethodCaller;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.ApplicationEvent;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.BeanEventProcessor;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.processing.processor.MethodAnnotationProcessor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventHandlerImpl implements EventHandler {
  List<EventProcessor<? extends Event>> processors = new LinkedList<>();
  DiApplication application;
  @Getter
  Map<Class<? extends ApplicationEvent>, List<BoundMethodCaller>> applicationEventListeners = new HashMap<>();
  
  public EventHandlerImpl(DiApplication application) {
    this.application = application;
    
    processors.add(new MethodAnnotationProcessor(this));
    processors.add(new BeanEventProcessor());
  }
  
  @Override
  public void registerProcessor(EventProcessor<? extends Event> processor) {
    processors.add(processor);
  }
  
  @Override
  public <E extends Event> void handleEvent(E event) {
    if (event instanceof ApplicationEvent applicationEvent) {
      applicationEventListeners.getOrDefault(applicationEvent.getClass(), List.of())
                               .forEach(boundMethodCaller -> boundMethodCaller.call(event));
      return;
    }
    
    for (EventProcessor<? extends Event> processor : processors) {
      processor.tryProcess(event, application);
    }
  }
}
