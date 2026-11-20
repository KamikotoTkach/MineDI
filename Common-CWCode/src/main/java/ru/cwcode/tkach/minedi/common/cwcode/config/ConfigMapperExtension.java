package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.base.Config;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.config.repository.Repository;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.DiContainer;
import ru.cwcode.tkach.minedi.constructor.BeanConstructor;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.extension.Extension;

import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Creates every {@link ConfigMapperCustomizer} bean and applies it before conditions and beans, so the first config read
 * already sees the subtypes and modules.
 */
public class ConfigMapperExtension implements Extension {
  private final YmlConfigManager manager;

  public ConfigMapperExtension(YmlConfigManager manager) {
    this.manager = manager;
  }

  /**
   * Subtypes published by any plugin reach this mapper: those already known now, later ones as their plugins start.
   * Configs and repositories get no injected collections: there a null list means "not set in the file".
   */
  @Override
  public void onRegister(DiApplication application) {
    application.getExtensionRegistry().onSubtype(subtype -> {
      manager.registerSubtype(subtype.type(), subtype.name());
      if (application.isStarted()) {
        application.getLogger().debug("Config subtype '{}' of plugin {} arrived after start: configs read before see it after reload",
                                      subtype.name(), subtype.plugin());
      }
    });

    application.getContainer().addCollectionInjectionFilter(type -> !Config.class.isAssignableFrom(type)
                                                                    && !Repository.class.isAssignableFrom(type));
  }

  @Override
  public void onBeforeStart(DiApplication application) {
    DiContainer container = application.getContainer();
    Set<Class<?>> customizers = new TreeSet<>(Comparator.comparing(Class::getName));

    for (Class<?> scanned : container.scannedClasses()) {
      if (!isCustomizer(scanned)) continue;
      container.registerComponent(scanned);
      if (container.isBean(scanned)) customizers.add(scanned);
    }
    for (Class<?> type : container.beanTypes()) {
      if (isCustomizer(type)) customizers.add(type);
    }
    if (customizers.isEmpty()) return;

    // a customizer may need a component of the plugin; @Bean methods still run only after all components are registered
    container.scannedClasses().forEach(container::registerComponent);

    ConfigGuard guard = new ConfigGuard();
    application.getBeanConstructors().getConstructors().addFirst(guard);
    try {
      for (Class<?> type : customizers) {
        guard.customizer = type;
        ConfigMapperCustomizer customizer = (ConfigMapperCustomizer) container.createOrGet(type);
        customizer.customize(manager);
        application.getLogger().debug("Config mapper customized by {}", type.getName());
      }
    } finally {
      application.getBeanConstructors().getConstructors().remove(guard);
    }
  }

  @Override
  public void onStart(DiApplication application) {
  }

  private static boolean isCustomizer(Class<?> type) {
    return ConfigMapperCustomizer.class.isAssignableFrom(type)
           && !type.isInterface()
           && !Modifier.isAbstract(type.getModifiers());
  }

  private static final class ConfigGuard implements BeanConstructor {
    Class<?> customizer;

    @Override
    public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
      if (!Config.class.isAssignableFrom(clazz) && !Repository.class.isAssignableFrom(clazz)) return null;

      throw new BeanDefinitionException(("%s: ConfigMapperCustomizer %s depends on %s, but customizers run before any config "
                                         + "or repository is read. Remove the dependency")
                                          .formatted(application.getName(), customizer.getName(), clazz.getName()));
    }
  }
}
