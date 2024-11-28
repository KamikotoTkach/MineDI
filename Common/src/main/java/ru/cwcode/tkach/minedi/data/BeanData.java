package ru.cwcode.tkach.minedi.data;

import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Getter
public class BeanData {
  private final Set<Annotation> annotations;
  @Setter
  private List<BeanDependency> dependencies = List.of();
  
  private final HashMap<Field, Object> beanFields = new HashMap<>();
  
  public BeanData(Set<Annotation> annotations) {
    this.annotations = annotations;
  }
  
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return annotations.stream().anyMatch(x -> x.annotationType().equals(annotation));
  }
}
