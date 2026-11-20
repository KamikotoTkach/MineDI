package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.entity.Player;
import ru.cwcode.tkach.minedi.scope.PlayerScopeProvider;
import ru.cwcode.tkach.minedi.scope.ScopedBeans;

/**
 * The instances of a {@link ru.cwcode.tkach.minedi.annotation.PlayerScoped} bean; declare a field or constructor
 * parameter {@code PlayerScope<MyState>} in any bean and the container fills it.
 */
public class PlayerScope<T> extends ScopedBeans<Player, T> {
  public PlayerScope(PlayerScopeProvider provider, Class<T> type) {
    super(provider, type);
  }
}
