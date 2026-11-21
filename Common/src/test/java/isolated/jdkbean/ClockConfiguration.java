package isolated.jdkbean;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

import java.time.Clock;

@Service
public class ClockConfiguration {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
