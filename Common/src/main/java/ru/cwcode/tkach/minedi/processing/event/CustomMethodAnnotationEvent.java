package ru.cwcode.tkach.minedi.processing.event;

import lombok.Getter;
import ru.cwcode.tkach.minedi.utils.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Getter
public class CustomMethodAnnotationEvent extends BeanEvent {
  final Method method;
  final Annotation annotation;
  
  public CustomMethodAnnotationEvent(Object bean, Method method, Annotation annotation) {
    super(bean);
    this.method = method;
    this.annotation = annotation;
  }
  
  @Override
  public String toString() {
    return "CustomMethodAnnotationEvent{" +
           "method=" + method.toGenericString() +
           ", annotation=" + annotation.toString() +
           ", bean=" + Utils.getClassNameWithHash(getBean()) +
           '}';
  }
}
