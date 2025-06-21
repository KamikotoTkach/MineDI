import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.logging.LogConsumer;
import ru.cwcode.tkach.minedi.logging.LogLevel;

public class TestLogger extends Log {
  public TestLogger() {
    consumers.add(new LogConsumer() {
      @Override
      public boolean isEnabled(LogLevel logLevel) {
        return true;
      }
      
      @Override
      public void consume(String log, LogLevel level) {
        System.out.println(log);
      }
    });
  }
}
