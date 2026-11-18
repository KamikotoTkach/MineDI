package isolated.beanorder.disabled;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class DisabledConfiguration {
  @Bean
  public User user(Source source) {
    return new User();
  }

  @Bean(condition = "!hasClass(java.lang.String)")
  public Source source() {
    return new Source();
  }

  public static class User {
  }

  public static class Source {
  }
}
