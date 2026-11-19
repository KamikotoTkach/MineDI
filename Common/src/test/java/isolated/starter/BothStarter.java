package isolated.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "Alpha")
@Starter(plugin = "beta")
public class BothStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    StarterLog.events.add("BothStarter");
  }
}
