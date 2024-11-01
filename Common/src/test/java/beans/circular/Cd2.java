package beans.circular;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;

@Component
@Lazy
public class Cd2 {
  @Required
  Cd3 cd3;
}
