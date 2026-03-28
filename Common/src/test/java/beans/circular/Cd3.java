package beans.circular;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;

@Component
@Lazy
public class Cd3 {
  final Cd1 cd1;
  
  public Cd3(Cd1 cd1) {
    this.cd1 = cd1;
  }
}
