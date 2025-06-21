package ru.cwcode.tkach.minedi.logging;

import java.util.EnumMap;
import java.util.Map;

public class LogState {
  public static final Map<LogLevel, Boolean> DEFAULT_STATE = Map.of(LogLevel.DEBUG, true,
                                                                    LogLevel.INFO, true,
                                                                    LogLevel.WARNING, true,
                                                                    LogLevel.ERROR, true);
  
  EnumMap<LogLevel, Boolean> state = new EnumMap<>(DEFAULT_STATE);
  
  public boolean isEnabled(LogLevel logLevel) {
    return state.getOrDefault(logLevel, false);
  }
  
  public void setEnabled(LogLevel level, boolean state) {
    this.state.put(level, state);
  }
}
