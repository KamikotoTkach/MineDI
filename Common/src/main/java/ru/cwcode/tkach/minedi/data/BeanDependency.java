package ru.cwcode.tkach.minedi.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class BeanDependency {
  Class<?> clazz;
  boolean startupRequired;
  
  public BeanDependency(Class<?> clazz, boolean startupRequired) {
    this.clazz = clazz;
    this.startupRequired = startupRequired;
  }
}
