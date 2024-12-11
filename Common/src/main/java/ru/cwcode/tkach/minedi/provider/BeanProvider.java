package ru.cwcode.tkach.minedi.provider;

import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.data.BeanData;

import java.util.Set;

public interface BeanProvider {
  Set<Class<?>> getBeanClasses();
  String scope();
  @Nullable Object provide(BeanData beanData);
}
