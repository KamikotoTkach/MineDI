package ru.cwcode.tkach.minedi.data;

import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class ComponentData {
  private final Set<Annotation> annotations;
  @Setter
  @Getter
  private List<BeanDependency> dependencies = List.of();
  
  public ComponentData(Set<Annotation> annotations) {
    this.annotations = annotations;
  }
  
  public boolean isRequired(Class<?> clazz) {
    return dependencies.stream()
                       .filter(x->x.getClazz().equals(clazz))
                       .findFirst()
                       .map(BeanDependency::isStartupRequired)
                       .orElse(false);
  }
  
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return annotations.stream().anyMatch(x->x.annotationType().equals(annotation));
  }
}
