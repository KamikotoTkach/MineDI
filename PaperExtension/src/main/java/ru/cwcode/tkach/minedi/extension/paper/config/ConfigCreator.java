package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.config.base.Config;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.constructor.BeanConstructorImpl;

import java.util.Optional;

public class ConfigCreator extends ru.cwcode.tkach.config.base.manager.ConfigCreator<YmlConfig> {
  final DiApplication application;
  final BeanConstructorImpl beanConstructor;
  
  public ConfigCreator(DiApplication application) {
    this.application = application;
    this.beanConstructor = new BeanConstructorImpl();
  }
  
  @Override
  protected <V extends Config<YmlConfig>> Optional<V> createInstance(Class<V> configClass) {
    Class<?> objectClass = configClass;
    return Optional.ofNullable((V) beanConstructor.construct((Class<Object>) objectClass, application.getContainer().getData(configClass), application));
  }
}
