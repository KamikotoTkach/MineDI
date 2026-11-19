package isolated.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "Broken")
public class BrokenStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    throw new IllegalStateException("no database");
  }
}
