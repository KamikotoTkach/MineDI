package isolated.compat.core;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class GreeterUser {
  Greeter greeter;
}
