package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.entity.Player;
import ru.cwcode.tkach.minedi.scope.KeyedSet;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

/**
 * A set of online players that loses a player on quit; a field {@code PlayerSet} of a bean is filled by the container.
 */
public class PlayerSet extends KeyedSet<Player> {
  public PlayerSet(PlayerWindows windows) {
    super(windows);
  }
}
