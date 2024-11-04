package ru.cwcode.tkach.minedi.constructor;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.data.BeanData;

import java.util.Deque;
import java.util.LinkedList;

@Getter
public class BeanConstructors {
  private final DiApplication application;
  
  private final Deque<BeanConstructor> constructors = new LinkedList<>();
  
  public BeanConstructors(DiApplication application) {
    this.application = application;
  }
  
  public <T> T construct(Class<T> clazz, BeanData data) {
    for (BeanConstructor constructor : constructors) {
      T constructed = constructor.construct(clazz, data, application);
      if (constructed != null) {
        return constructed;
      }
    }
    
    return null;
  }
}
