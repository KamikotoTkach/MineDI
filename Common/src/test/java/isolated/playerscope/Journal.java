package isolated.playerscope;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.scope.KeyedMap;
import ru.cwcode.tkach.minedi.scope.KeyedSet;
import ru.cwcode.tkach.minedi.scope.ScopedBeans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Service
public class Journal {
  final List<String> entries = new CopyOnWriteArrayList<>();
  ScopedBeans<TestPlayer, Wallet> wallets;
  KeyedMap<TestPlayer, Long> lastSeen;
  KeyedSet<TestPlayer> flagged;

  public void write(String entry) {
    entries.add(entry);
  }
}
