package ru.cwcode.tkach.minedi.processing;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.BeanEventProcessor;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.processing.processor.MethodAnnotationProcessor;

import java.util.LinkedList;
import java.util.List;

public class EventHandlerImpl implements EventHandler {
  List<EventProcessor<? extends Event>> processors = new LinkedList<>();
  DiApplication application;
  
  public EventHandlerImpl(DiApplication application) {
    this.application = application;
    
    processors.add(new MethodAnnotationProcessor());
    processors.add(new BeanEventProcessor());
  }
  
  @Override
  public void registerProcessor(EventProcessor<? extends Event> processor) {
    processors.add(processor);
  }
  
  @Override
  public <E extends Event> void handleEvent(E event) {
    for (EventProcessor<? extends Event> processor : processors) {
      processor.tryProcess(event, application);
    }
  }
}
