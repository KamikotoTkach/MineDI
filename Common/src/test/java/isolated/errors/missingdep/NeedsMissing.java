package isolated.errors.missingdep;

import ru.cwcode.tkach.minedi.annotation.Component;

@Component
public class NeedsMissing {
  public NeedsMissing(NotABean dependency) {
  }
}
