package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.BeanProperty;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.DeserializationContext;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.InjectableValues;
import ru.cwcode.tkach.minedi.DiApplication;

public class DiInjectableValues extends InjectableValues {
  DiApplication diApplication;
  
  public DiInjectableValues(DiApplication diApplication) {
    this.diApplication = diApplication;
  }
  
  @Override
  public Object findInjectableValue(Object o, DeserializationContext deserializationContext, BeanProperty beanProperty, Object o1) {
    return diApplication.get(beanProperty.getType().getRawClass())
                        .orElseThrow(() -> new RuntimeException("Unable to find InjectableValue for " + beanProperty.getName() + " in " + o1.getClass().getName()));
  }
}
