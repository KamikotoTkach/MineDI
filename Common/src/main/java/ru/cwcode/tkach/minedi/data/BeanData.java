package ru.cwcode.tkach.minedi.data;

import lombok.Getter;
import lombok.Setter;
import ru.cwcode.tkach.minedi.BeanScope;
import ru.cwcode.tkach.minedi.DiContainer;
import ru.cwcode.tkach.minedi.annotation.Required;
import ru.cwcode.tkach.minedi.annotation.Scope;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;
import ru.cwcode.tkach.minedi.utils.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

@Getter
public class BeanData {
  private final DiContainer diContainer;
  
  private final Class<?> clazz;
  private final Set<Annotation> annotations;
  private final HashMap<Field, Object> beanFields = new HashMap<>();
  String scope;
  @Setter
  private List<BeanDependency> dependencies = List.of();
  
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
  
  public void searchForDependencies() {
    diContainer.getApplication().getLogger().info("Searching for dependencies of " + clazz);
    
    if (clazz.isInterface()) {
      dependencies = List.of();
      return;
    }
    
    Set<BeanDependency> fieldDependencies = new HashSet<>(ReflectionUtils.getFields(clazz).stream()
                                                            .filter(x -> diContainer.isBean(x.getType()))
                                                            .map(x -> new BeanDependency(x.getType(), x.isAnnotationPresent(Required.class)))
                                                            .toList());
    if (clazz.getDeclaredConstructors().length == 1) {
      List<BeanDependency> constructorDependencies = Arrays.stream(clazz.getDeclaredConstructors()[0].getParameterTypes())
                                                           .filter(diContainer::isBean)
                                                           .map(x -> new BeanDependency(x, true))
                                                           .toList();
      
      fieldDependencies.addAll(constructorDependencies);
    }
    
    diContainer.getApplication().getLogger().info("Dependencies of {} is ", clazz, Utils.toString(fieldDependencies));
    
    dependencies = new ArrayList<>(fieldDependencies);
  }
}
