package isolated.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "Alpha")
public class AlphaStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    StarterLog.events.add("AlphaStarter for " + context.getConsumerName());
    context.bean(new StarterService("starter"), StarterService.class);
  }
}
