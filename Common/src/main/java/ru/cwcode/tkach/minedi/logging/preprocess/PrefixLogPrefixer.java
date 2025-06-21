package ru.cwcode.tkach.minedi.logging.preprocess;

import ru.cwcode.tkach.minedi.logging.LogLevel;

public class PrefixLogPrefixer implements LogPrefixer {
  final String prefix;
  
  public PrefixLogPrefixer(String prefix) {
    this.prefix = prefix;
  }
  
  @Override
  public String process(String log, LogLevel level, boolean isFirst, String delimiter) {
    return (isFirst ? "" : delimiter) + "[" + prefix + "]" + delimiter + log;
  }
}
