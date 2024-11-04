package ru.cwcode.tkach.minedi.constructor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.data.BeanData;

public interface BeanConstructor {
  <T> T construct(Class<T> clazz, BeanData data, DiApplication application);
}
