package ru.cwcode.tkach.minedi.processing.event;

import lombok.Getter;

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
}
