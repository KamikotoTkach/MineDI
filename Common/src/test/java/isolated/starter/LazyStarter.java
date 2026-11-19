package isolated.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "Lazy")
public class LazyStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    context.lazyBean(LazyService.class, LazyService::new);
  }
}
