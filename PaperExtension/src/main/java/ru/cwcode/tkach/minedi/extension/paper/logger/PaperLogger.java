package ru.cwcode.tkach.minedi.extension.paper.logger;

import ru.cwcode.cwutils.config.SimpleConfig;
import ru.cwcode.cwutils.l10n.PaperL10nPlatform;
import ru.cwcode.tkach.minedi.extension.paper.PaperPlatform;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.logging.LogState;
import ru.cwcode.tkach.minedi.logging.preprocess.DynamicPrefixLogPrefixer;

public class PaperLogger extends Log {
  public PaperLogger(PaperPlatform plugin) {
    SimpleConfig config = new SimpleConfig("logger", new PaperL10nPlatform(plugin, plugin.getFile()));
    
    LogState logState = new LogState();
    for (LogLevel value : LogLevel.values()) {
      String logLevelState = config.get(value.name().toLowerCase(), "default");
      if (!logLevelState.equals("default")) logState.setEnabled(value, Boolean.parseBoolean(logLevelState));
    }
    
    consumers.add(new PaperLogConsumer(logState, plugin));
    
    prefixers.addFirst(new DynamicPrefixLogPrefixer(() -> Thread.currentThread().getName()));
  }
}
