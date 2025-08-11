package ru.cwcode.tkach.minedi.processing.event;

import lombok.Getter;
import lombok.Setter;
import ru.cwcode.tkach.minedi.utils.Utils;

public class BeanCreatedEvent extends BeanEvent {
  @Getter
  @Setter
  Object replacement = null;
  
  public BeanCreatedEvent(Object bean) {
    super(bean);
  }
  
  @Override
  public Object getBean() {
    return replacement == null ? super.getBean() : replacement;
  }
  
  @Override
  public String toString() {
    return "BeanCreatedEvent{" +
           "replacement=" + Utils.getClassNameWithHash(replacement) +
           ",bean=" + Utils.getClassNameWithHash(getBean()) +
           '}';
  }
}
