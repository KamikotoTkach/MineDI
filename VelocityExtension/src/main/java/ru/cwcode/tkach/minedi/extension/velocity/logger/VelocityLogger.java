package ru.cwcode.tkach.minedi.extension.velocity.logger;

import ru.cwcode.cwutils.config.SimpleConfig;
import ru.cwcode.cwutils.l10n.VelocityL10nPlatform;
import ru.cwcode.tkach.minedi.extension.velocity.VelocityPlatform;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.logging.LogState;

public class VelocityLogger extends Log {
  public VelocityLogger(VelocityPlatform plugin) {
    SimpleConfig config = new SimpleConfig("logger", new VelocityL10nPlatform(plugin, plugin.getDataDirectory(), plugin.getLogger(), plugin.getPluginFile()));
    
    LogState logState = new LogState();
    for (LogLevel value : LogLevel.values()) {
      // getParsed would parse the "[Key … not found]" placeholder of an absent key as false and mute that level
      Boolean logLevelState = config.get(value.name().toLowerCase(), Boolean.class, null);
      if (logLevelState != null) logState.setEnabled(value, logLevelState);
    }
    
    consumers.add(new VelocityLogConsumer(logState, plugin));
  }
}
