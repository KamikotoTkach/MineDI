package ru.cwcode.minediprobe.velocity;

import org.bukkit.plugin.Plugin;
import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * Bukkit API is not on the proxy: loading this class would throw NoClassDefFoundError.
 */
@Conditional("hasPlugin(nosuchplugin)")
@Service
public class VelocityAbsentService {
  Plugin plugin;

  public Plugin plugin(Plugin replacement) {
    return replacement == null ? plugin : replacement;
  }
}
