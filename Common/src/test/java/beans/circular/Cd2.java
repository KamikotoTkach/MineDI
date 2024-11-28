package beans.circular;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;

@Component
@Lazy
public class Cd2 {
  Cd3 cd3;
  
  public Cd2(Cd3 cd3) {
    this.cd3 = cd3;
  }
}
