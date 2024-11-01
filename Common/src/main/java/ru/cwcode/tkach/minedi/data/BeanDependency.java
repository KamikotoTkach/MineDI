package ru.cwcode.tkach.minedi.data;

import lombok.Getter;

@Getter
public class BeanDependency {
  Class<?> clazz;
  boolean startupRequired;
  
  public BeanDependency(Class<?> clazz, boolean startupRequired) {
    this.clazz = clazz;
    this.startupRequired = startupRequired;
  }
}
