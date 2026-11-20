package ru.cwcode.tkach.minedi.extension.velocity.scope;

import com.velocitypowered.api.proxy.Player;
import ru.cwcode.tkach.minedi.scope.KeyedMap;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

/**
 * A map by online player that forgets the player on quit; a field {@code PlayerMap<Long>} of a bean is filled by the
 * container, no quit handler needed.
 */
public class PlayerMap<V> extends KeyedMap<Player, V> {
  public PlayerMap(PlayerWindows windows) {
    super(windows);
  }
}
