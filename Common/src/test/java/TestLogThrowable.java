import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.logging.LogConsumer;
import ru.cwcode.tkach.minedi.logging.LogLevel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogThrowable {
  @Test
  public void unusedTrailingThrowableReachesConsumer() {
    RecordingConsumer consumer = new RecordingConsumer();
    RuntimeException failure = new RuntimeException("boom");

    log(consumer).warn("Reconcile failed", failure);

    assertEquals("Reconcile failed", consumer.logs.get(0));
    assertSame(failure, consumer.throwables.get(0));
  }

  @Test
  public void consumedTrailingThrowableStillReachesConsumer() {
    RecordingConsumer consumer = new RecordingConsumer();
    RuntimeException failure = new RuntimeException("boom");

    log(consumer).warn("Cannot load {}: {}", "coins", failure);

    assertEquals("Cannot load coins: java.lang.RuntimeException: boom", consumer.logs.get(0));
    assertSame(failure, consumer.throwables.get(0));
  }

  @Test
  public void nonTrailingThrowableIsOnlyText() {
    RecordingConsumer consumer = new RecordingConsumer();

    log(consumer).warn("{} for {}", new RuntimeException("boom"), "player");

    assertNull(consumer.throwables.get(0));
  }

  @Test
  public void fallbackConsumerAppendsStackTrace() {
    List<String> logs = new ArrayList<>();
    LogConsumer plainConsumer = new LogConsumer() {
      @Override
      public boolean isEnabled(LogLevel logLevel) {
        return true;
      }

      @Override
      public void consume(String log, LogLevel level) {
        logs.add(log);
      }
    };

    log(plainConsumer).warn("Reconcile failed", new IllegalStateException("boom"));

    String logged = logs.get(0);
    assertTrue(logged.startsWith("Reconcile failed" + System.lineSeparator() + "java.lang.IllegalStateException: boom"));
    assertTrue(logged.contains("\tat TestLogThrowable."));
  }

  @Test
  public void plainMessageHasNoThrowable() {
    RecordingConsumer consumer = new RecordingConsumer();

    log(consumer).info("Started");

    assertNull(consumer.throwables.get(0));
    assertFalse(consumer.logs.get(0).contains(System.lineSeparator()));
  }

  private Log log(LogConsumer consumer) {
    Log log = new Log();
    log.getConsumers().add(consumer);
    return log;
  }

  private static class RecordingConsumer implements LogConsumer {
    final List<String> logs = new ArrayList<>();
    final List<Throwable> throwables = new ArrayList<>();

    @Override
    public boolean isEnabled(LogLevel logLevel) {
      return true;
    }

    @Override
    public void consume(String log, LogLevel level) {
      consume(log, level, null);
    }

    @Override
    public void consume(String log, LogLevel level, Throwable throwable) {
      logs.add(log);
      throwables.add(throwable);
    }
  }
}
