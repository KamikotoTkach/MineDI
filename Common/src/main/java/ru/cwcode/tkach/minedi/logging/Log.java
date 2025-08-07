package ru.cwcode.tkach.minedi.logging;

import lombok.Getter;
import ru.cwcode.tkach.minedi.logging.preprocess.LogPrefixer;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class Log {
  String delimiter = " ";
  
  @Getter
  protected final List<LogConsumer> consumers = new LinkedList<>();
  
  @Getter
  protected final Deque<LogPrefixer> prefixers = new LinkedList<>();
  
  public void debug(String log) {
    this.log(LogLevel.DEBUG, log, (Object[]) null);
  }
  
  public void debug(String log, Object... params) {
    this.log(LogLevel.DEBUG, log, params);
  }
  
  public void warn(String log) {
    this.log(LogLevel.WARNING, log, (Object[]) null);
  }
  
  public void warn(String log, Object... params) {
    this.log(LogLevel.WARNING, log, params);
  }
  
  public void info(String log) {
    this.log(LogLevel.INFO, log, (Object[]) null);
  }
  
  public void info(String log, Object... params) {
    this.log(LogLevel.INFO, log, params);
  }
  
  public void log(LogLevel level, String log) {
    this.log(level, log, (Object[]) null);
  }
  
  public void log(LogLevel level, String log, Object... params) {
    String preparedString = null;
    
    for (LogConsumer consumer : consumers) {
      if (consumer.isEnabled(level)) {
        if (preparedString == null) {
          preparedString = preprocess(level, replacePlaceholders(log, params));
        }
        
        consumer.consume(preparedString, level);
      }
    }
  }
  
  private String preprocess(LogLevel level, String log) {
    boolean isFirst = true;
    for (LogPrefixer logPrefixer : prefixers) {
      log = logPrefixer.process(log, level, isFirst, delimiter);
      isFirst = false;
    }
    
    return log;
  }
  
  private String replacePlaceholders(String log, Object[] params) {
    if (params == null || params.length == 0) {
      return log;
    }
    
    StringBuilder builder = new StringBuilder(log.length() + params.length * 16);
    
    int logIndex = 0;
    int paramIndex = 0;
    int placeholderIndex;
    
    while (paramIndex < params.length && (placeholderIndex = log.indexOf("{}", logIndex)) != -1) {
      builder.append(log, logIndex, placeholderIndex);
      builder.append(paramToString(params[paramIndex++]));
      logIndex = placeholderIndex + 2;
    }
    
    if (logIndex < log.length()) {
      builder.append(log, logIndex, log.length());
    }
    
    return builder.toString();
  }
  
  private String paramToString(Object param) {
    if (param instanceof Supplier<?> c) param = c.get();
    return param == null ? "null" : param.toString();
  }
}
