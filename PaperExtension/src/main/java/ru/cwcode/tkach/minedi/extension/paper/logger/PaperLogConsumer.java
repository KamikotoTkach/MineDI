package ru.cwcode.tkach.minedi.extension.paper.logger;

import ru.cwcode.tkach.minedi.extension.paper.PaperPlatform;
import ru.cwcode.tkach.minedi.logging.LogConsumer;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.logging.LogState;

import java.util.logging.Level;

public class PaperLogConsumer implements LogConsumer {
  private final LogState logState;
  private final PaperPlatform plugin;
  
  public PaperLogConsumer(LogState logState, PaperPlatform plugin) {
    this.logState = logState;
    this.plugin = plugin;
    
    plugin.getLogger().setLevel(Level.ALL);
  }
  
  @Override
  public boolean isEnabled(LogLevel logLevel) {
    return logState.isEnabled(logLevel);
  }
  
  @Override
  public void consume(String log, LogLevel level) {
    Level l = switch (level) {
      case DEBUG -> Level.FINE;
      case INFO -> Level.INFO;
      case WARNING -> Level.WARNING;
      case ERROR -> Level.SEVERE;
    };
    
    plugin.getLogger().log(l, log);
  }
}
