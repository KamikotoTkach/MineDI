package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.processing.event.ComponentRegisteredEvent;

import java.lang.reflect.Method;

public class ConfigurationProcessor extends EventProcessor<ComponentRegisteredEvent> {
  public ConfigurationProcessor() {
    super(ComponentRegisteredEvent.class);
  }
  
  @Override
  public void process(ComponentRegisteredEvent event, DiApplication application) {
    for (Method declaredMethod : event.component().getDeclaredMethods()) {
      if (declaredMethod.isAnnotationPresent(Bean.class)) {
        application.get(event.component());
        return;
      }
    }
  }
}
