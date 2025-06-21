package ru.cwcode.tkach.minedi.logging.preprocess;

import ru.cwcode.tkach.minedi.logging.LogLevel;

public interface LogPrefixer {
  String process(String log, LogLevel level, boolean isFirst, String delimiter);
}
