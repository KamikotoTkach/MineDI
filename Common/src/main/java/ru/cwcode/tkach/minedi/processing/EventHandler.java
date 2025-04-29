package ru.cwcode.tkach.minedi.processing;

import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Method;

public interface EventHandler {
  void registerProcessor(EventProcessor<? extends Event> processor);
  void registerApplicationEventListener(Object bean);
  void registerApplicationEventListener(Object bean, Method method);
  <E extends Event> void handleEvent(E event);
}
