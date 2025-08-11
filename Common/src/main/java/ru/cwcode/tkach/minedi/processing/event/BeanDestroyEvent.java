package ru.cwcode.tkach.minedi.processing.event;

import ru.cwcode.tkach.minedi.utils.Utils;

public class BeanDestroyEvent extends BeanEvent implements ApplicationEvent {
  public BeanDestroyEvent(Object bean) {
    super(bean);
  }
  
  @Override
  public String toString() {
    return "BeanDestroyEvent{" +
           "bean=" + Utils.getClassNameWithHash(getBean()) +
           '}';
  }
}
