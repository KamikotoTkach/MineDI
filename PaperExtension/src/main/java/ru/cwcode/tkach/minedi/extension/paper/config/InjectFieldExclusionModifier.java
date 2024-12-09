package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.annotation.JacksonInject;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.BeanDescription;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.SerializationConfig;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.List;
import java.util.stream.Collectors;

public class InjectFieldExclusionModifier extends BeanSerializerModifier {
  
  @Override
  public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
    return beanProperties.stream()
                         .filter(writer -> writer.getAnnotation(JacksonInject.class) == null)
                         .collect(Collectors.toList());
  }
}
