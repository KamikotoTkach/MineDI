package isolated.compat.core;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class GreeterCtorUser {
  private final Greeter greeter;
  
  public GreeterCtorUser(Greeter greeter) {
    this.greeter = greeter;
  }
}
