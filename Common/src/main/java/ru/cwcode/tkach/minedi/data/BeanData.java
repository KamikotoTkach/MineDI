package ru.cwcode.tkach.minedi.data;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.BeanScope;
import ru.cwcode.tkach.minedi.DiContainer;
import ru.cwcode.tkach.minedi.annotation.Scope;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;

@Getter
public class BeanData {
  private final DiContainer diContainer;
  
  private final Class<?> clazz;
  private final Set<Annotation> annotations;
  private final HashMap<Field, Object> beanFields = new HashMap<>();
  String scope;
  /**
   * Set for a lazy bean registered with a factory: it replaces the bean constructors and the bean is created only on demand.
   */
  @Setter
  private @Nullable Supplier<?> factory;
  
  public BeanData(Class<?> clazz, DiContainer diContainer) {
    this.diContainer = diContainer;
    
    this.clazz = clazz;
    this.annotations = ReflectionUtils.getAnnotations(clazz);
    
    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
    this.scope = scopeAnnotation == null ? BeanScope.SINGLETON : scopeAnnotation.value();
  }
  
  public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
    return annotations.stream().anyMatch(x -> x.annotationType().equals(annotation));
  }
}
