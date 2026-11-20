package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.entity.Player;
import ru.cwcode.tkach.minedi.scope.KeyedCollections;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

/**
 * For unit tests that build a service with {@code new}: its {@link PlayerMap}/{@link PlayerSet} fields get maps of a
 * private window registry in which the given players are online. Stub {@code getUniqueId()} of mocked players first.
 */
public final class PlayerScopeTesting {
  private PlayerScopeTesting() {
  }

  /**
   * @return the registry, to {@link PlayerWindows#close} a player or {@link PlayerWindows#open} another one
   */
  public static PlayerWindows online(Object bean, Player... players) {
    PlayerWindows windows = new PlayerWindows();
    windows.setKeyType(Player.class, Player::getUniqueId);
    for (Player player : players) windows.open(player);

    KeyedCollections.fill(bean, windows);
    return windows;
  }
}
