package beans;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Component
public class StaticInject {
  @Getter
  static SimpleBean bean;
}
