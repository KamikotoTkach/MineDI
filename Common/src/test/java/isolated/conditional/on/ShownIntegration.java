package isolated.conditional.on;

import isolated.conditional.Always;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * The Integration suffix normally keeps a class out of the scan; the package condition brings it back.
 */
@Service
public class ShownIntegration {
  public static Always always;
}
