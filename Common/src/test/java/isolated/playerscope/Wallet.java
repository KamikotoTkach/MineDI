package isolated.playerscope;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;

@Getter
@PlayerScoped
public class Wallet {
  TestPlayer player;
  Journal journal;
  long coins;

  public void add(long amount) {
    coins += amount;
  }

  @PreDestroy
  void save() {
    journal.write("wallet " + player.name());
  }
}
