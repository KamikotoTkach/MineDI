package ru.cwcode.tkach.minedi.logging.preprocess;

import ru.cwcode.tkach.minedi.logging.LogLevel;

import java.util.function.Supplier;

public class DynamicPrefixLogPrefixer implements LogPrefixer {
  final Supplier<String> prefix;
  
  public DynamicPrefixLogPrefixer(Supplier<String> prefix) {
    this.prefix = prefix;
  }
  
  @Override
  public String process(String log, LogLevel level, boolean isFirst, String delimiter) {
    return (isFirst ? "" : delimiter) + "[" + prefix.get() + "]" + delimiter + log;
  }
}
