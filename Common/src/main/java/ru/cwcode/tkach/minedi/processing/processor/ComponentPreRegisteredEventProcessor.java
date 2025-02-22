package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.processing.event.ComponentPreRegisteredEvent;

import java.lang.reflect.Method;

public class ComponentPreRegisteredEventProcessor extends EventProcessor<ComponentPreRegisteredEvent> {
  public ComponentPreRegisteredEventProcessor() {
    super(ComponentPreRegisteredEvent.class);
  }
  
  @Override
  public void process(ComponentPreRegisteredEvent event, DiApplication application) {
    for (Method declaredMethod : event.component().getDeclaredMethods()) {
      Bean beanAnnotation = declaredMethod.getAnnotation(Bean.class);
      if (beanAnnotation == null) continue;
      
      String condition = beanAnnotation.condition();
      
      if (condition == null || application.getConditionParser().parse(condition)) {
        Class<?> as = beanAnnotation.as() == Object.class ? declaredMethod.getReturnType() : beanAnnotation.as();
        application.getContainer().registerBean(as, true);
      }
    }
  }
}
