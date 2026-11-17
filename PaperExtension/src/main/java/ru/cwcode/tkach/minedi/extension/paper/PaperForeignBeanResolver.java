package ru.cwcode.tkach.minedi.extension.paper;

import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.data.ForeignOwner;
import ru.cwcode.tkach.minedi.foreign.ForeignBeanResolver;

import java.util.Optional;

/**
 * The owner of a class is the plugin whose classloader defined it.
 */
public class PaperForeignBeanResolver implements ForeignBeanResolver {
  @Override
  public Optional<ForeignOwner> ownerOf(Class<?> type) {
    JavaPlugin plugin;
    try {
      plugin = JavaPlugin.getProvidingPlugin(type);
    } catch (IllegalArgumentException | IllegalStateException notFromPlugin) {
      return Optional.empty();
    }

    if (!(plugin instanceof PaperPlatform platform) || platform.getDiApplication() == null) return Optional.empty();

    return Optional.of(new ForeignOwner(plugin.getName(), platform.getDiApplication()));
  }
}
