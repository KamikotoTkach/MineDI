package ru.cwcode.tkach.minedi.processing.event;

import lombok.Getter;

@Getter
public abstract class BeanEvent implements Event {
  private final Object bean;
  
  public BeanEvent(Object bean) {
    this.bean = bean;
  }
}
