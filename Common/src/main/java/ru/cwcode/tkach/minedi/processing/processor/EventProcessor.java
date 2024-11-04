package ru.cwcode.tkach.minedi.processing.processor;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.Event;

@Getter
public abstract class EventProcessor<T extends Event> {
  final Class<T> handledClass;
  
  public EventProcessor(Class<T> handledClass) {
    this.handledClass = handledClass;
  }
  
  public abstract void process(T event, DiApplication application);
  
  public void tryProcess(Object event, DiApplication application) {
    if (handledClass.isInstance(event)) {
      process(handledClass.cast(event), application);
    }
  }
}
