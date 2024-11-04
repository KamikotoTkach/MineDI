package ru.cwcode.tkach.minedi.processing;

import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

public interface EventHandler {
  void registerProcessor(EventProcessor<? extends Event> processor);
  
  <E extends Event> void handleEvent(E event);
}
