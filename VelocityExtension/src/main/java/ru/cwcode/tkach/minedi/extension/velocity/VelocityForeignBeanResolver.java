package ru.cwcode.tkach.minedi.extension.velocity;

import ru.cwcode.tkach.minedi.data.ForeignOwner;
import ru.cwcode.tkach.minedi.foreign.ForeignBeanResolver;

import java.util.Optional;

/**
 * The owner of a class is the MineDI plugin whose classloader defined it.
 */
public class VelocityForeignBeanResolver implements ForeignBeanResolver {
  @Override
  public Optional<ForeignOwner> ownerOf(Class<?> type) {
    ClassLoader classLoader = type.getClassLoader();
    if (classLoader == null) return Optional.empty();

    for (VelocityPlatform plugin : Main.plugins) {
      if (plugin.getClass().getClassLoader() == classLoader && plugin.getDiApplication() != null) {
        return Optional.of(new ForeignOwner(plugin.getPluginId(), plugin.getDiApplication()));
      }
    }

    return Optional.empty();
  }
}
