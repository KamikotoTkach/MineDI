package isolated.playerscope;

import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;
import ru.cwcode.tkach.minedi.scope.Creation;

@PlayerScoped(create = Creation.SYNC)
public class Inventory {
  TestPlayer player;

  @PreDestroy
  void save() {
    throw new IllegalStateException("save failed for " + player.name());
  }
}
