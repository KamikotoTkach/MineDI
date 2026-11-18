package isolated.beanorder.cycle;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class CycleConfiguration {
  @Bean
  public Left left(Right right) {
    return new Left();
  }

  @Bean
  public Right right(Left left) {
    return new Right();
  }

  public static class Left {
  }

  public static class Right {
  }
}
