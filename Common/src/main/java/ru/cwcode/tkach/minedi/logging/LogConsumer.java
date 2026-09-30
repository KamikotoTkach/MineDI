package ru.cwcode.tkach.minedi.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

public interface LogConsumer {
  boolean isEnabled(LogLevel logLevel);

  void consume(String log, LogLevel level);

  /**
   * Override to hand the throwable to a native logger; the fallback appends the stack trace to the text.
   */
  default void consume(String log, LogLevel level, Throwable throwable) {
    if (throwable == null) {
      consume(log, level);
      return;
    }

    StringWriter stackTrace = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stackTrace));
    consume(log + System.lineSeparator() + stackTrace, level);
  }
}
