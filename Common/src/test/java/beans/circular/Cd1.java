package beans.circular;

import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;

@Component
@Lazy
public class Cd1 {
  final Cd2 cd2;
  
  public Cd1(Cd2 cd2) {
    this.cd2 = cd2;
  }
}
