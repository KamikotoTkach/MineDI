package isolated.conditional;

import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class Settings {
  boolean enabled = true;
  Inner inner = new Inner();

  static class Inner {
    Boolean on = false;
  }
}
