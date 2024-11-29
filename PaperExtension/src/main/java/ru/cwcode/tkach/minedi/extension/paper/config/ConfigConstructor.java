package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.config.base.Config;
import ru.cwcode.tkach.config.base.manager.ConfigManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.constructor.BeanConstructor;
import ru.cwcode.tkach.minedi.data.BeanData;

@SuppressWarnings("rawtypes")
public class ConfigConstructor implements BeanConstructor {
  final ConfigManager configManager;
  
  public ConfigConstructor(ConfigManager configManager) {
    this.configManager = configManager;
  }
  
  @Override
  public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
    if (!Config.class.isAssignableFrom(clazz)) return null;
    String configPath = clazz.getSimpleName();
    
    var annotation = clazz.getAnnotation(ru.cwcode.tkach.minedi.extension.paper.config.Config.class);
    if (annotation != null && !annotation.path().isEmpty()) {
      configPath = annotation.path();
    }
    
    return (T) configManager.load(configPath, clazz);
  }
}
