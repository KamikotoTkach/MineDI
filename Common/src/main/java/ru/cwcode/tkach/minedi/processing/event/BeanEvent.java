package ru.cwcode.tkach.minedi.processing.event;

import lombok.Getter;
import ru.cwcode.tkach.minedi.utils.Utils;

@Getter
public abstract class BeanEvent implements Event {
  private final Object bean;
  
  public BeanEvent(Object bean) {
    this.bean = bean;
  }
  
  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
           "bean=" + Utils.getClassNameWithHash(bean) +
           '}';
  }
}
