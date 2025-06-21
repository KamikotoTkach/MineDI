package ru.cwcode.tkach.minedi.extension.velocity.logger;

import org.slf4j.event.Level;
import ru.cwcode.tkach.minedi.extension.velocity.VelocityPlatform;
import ru.cwcode.tkach.minedi.logging.LogConsumer;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.logging.LogState;

public class VelocityLogConsumer implements LogConsumer {
  private final LogState logState;
  private final VelocityPlatform plugin;
  
  public VelocityLogConsumer(LogState logState, VelocityPlatform plugin) {
    this.logState = logState;
    this.plugin = plugin;
  }
  
  @Override
  public boolean isEnabled(LogLevel logLevel) {
    return logState.isEnabled(logLevel);
  }
  
  @Override
  public void consume(String log, LogLevel level) {
    Level l = switch (level) {
      case DEBUG -> Level.TRACE;
      case INFO -> Level.INFO;
      case WARNING -> Level.WARN;
      case ERROR -> Level.ERROR;
    };
    
    plugin.getLogger().atLevel(l).log(log);
  }
}
