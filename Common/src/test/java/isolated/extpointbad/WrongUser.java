package isolated.extpointbad;

import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extensionpoint.Extensions;

@Service
public class WrongUser {
  public WrongUser(Extensions<NotAPoint> points) {
  }
}
