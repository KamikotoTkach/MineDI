package isolated.playerscope;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.scope.Creation;

/**
 * Created on join, depends on a lazy bean of the same player; closed after it (reverse creation order).
 */
@Getter
@PlayerScoped(create = Creation.JOIN)
public class Profile implements AutoCloseable {
  final TestPlayer player;
  final Wallet wallet;
  final Journal journal;

  public Profile(TestPlayer player, Wallet wallet, Journal journal) {
    this.player = player;
    this.wallet = wallet;
    this.journal = journal;
  }

  @Override
  public void close() {
    journal.write("profile " + player.name());
  }
}
