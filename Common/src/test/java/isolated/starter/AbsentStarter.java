package isolated.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

/**
 * Its plugin is never installed in the tests: the class must stay unloaded.
 */
@Starter(plugin = "Absent")
public class AbsentStarter implements StarterExtension {
  static {
    StarterLog.events.add("AbsentStarter loaded");
  }

  @Override
  public void apply(StarterContext context) {
    StarterLog.events.add("AbsentStarter applied");
  }
}
