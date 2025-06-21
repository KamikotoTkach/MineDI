package ru.cwcode.tkach.minedi.logging;

public interface LogConsumer {
  boolean isEnabled(LogLevel logLevel);
  
  void consume(String log, LogLevel level);
}
