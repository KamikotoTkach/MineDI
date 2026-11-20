package ru.cwcode.tkach.minedi.scope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Access to the instances of one {@link ru.cwcode.tkach.minedi.annotation.PlayerScoped} bean, injected into any bean.
 * Platforms subclass it with their player type ({@code PlayerScope<T>}); the container fills fields and constructor
 * parameters of such a type by itself.
 *
 * @param <K> the platform player type
 * @param <T> the player bean
 */
public class ScopedBeans<K, T> {
  private final PlayerScopeProvider provider;
  private final Class<T> type;

  public ScopedBeans(PlayerScopeProvider provider, Class<T> type) {
    this.provider = provider;
    this.type = type;
  }

  /**
   * The player's instance, created now for a lazy bean.
   *
   * @throws IllegalStateException when the player is outside the bean's window: not joined yet, left or not synchronised
   */
  public T get(K player) {
    return provider.get(type, id(player));
  }

  public T get(UUID player) {
    return provider.get(type, player);
  }

  /**
   * The existing instance, never creating one.
   */
  public Optional<T> find(K player) {
    return provider.find(type, id(player));
  }

  public Optional<T> find(UUID player) {
    return provider.find(type, player);
  }

  /**
   * Created instances of online players; a lazy bean has none for players who never used it.
   */
  public List<T> all() {
    return provider.all(type);
  }

  /**
   * Visits a snapshot of {@link #all()}: players may join and leave meanwhile.
   */
  public void forEach(Consumer<? super T> action) {
    all().forEach(action);
  }

  public boolean isAvailable(K player) {
    return provider.inWindow(type, id(player));
  }

  public Class<T> type() {
    return type;
  }

  private UUID id(K player) {
    return provider.windows().idOf(player);
  }
}
