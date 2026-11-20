package ru.cwcode.tkach.minedi.scope;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map by online player whose entry disappears when the player leaves. Writes for a player outside the window are
 * dropped: an async callback finishing after the quit would otherwise leave the entry forever. Platforms subclass it with
 * their player type ({@code PlayerMap<V>}); the container makes a new one for every field.
 */
public class KeyedMap<K, V> implements PlayerWindows.WindowListener {
  private final PlayerWindows windows;
  private final Map<UUID, V> values = new ConcurrentHashMap<>();

  public KeyedMap(PlayerWindows windows) {
    this.windows = windows;
    windows.addListener(this);
  }

  public @Nullable V get(K player) {
    return values.get(id(player));
  }

  public @Nullable V get(UUID player) {
    return values.get(player);
  }

  public V getOrDefault(K player, V fallback) {
    return values.getOrDefault(id(player), fallback);
  }

  public boolean containsKey(K player) {
    return values.containsKey(id(player));
  }

  /**
   * @return the previous value; null and nothing stored when the player is not online
   */
  public @Nullable V put(K player, V value) {
    UUID id = id(player);
    if (!windows.isOpen(id)) return null;

    V previous = values.put(id, value);
    if (!windows.isOpen(id)) values.remove(id);
    return previous;
  }

  public @Nullable V remove(K player) {
    return values.remove(id(player));
  }

  public @Nullable V remove(UUID player) {
    return values.remove(player);
  }

  public @Nullable V computeIfAbsent(K player, Function<? super K, ? extends V> factory) {
    UUID id = id(player);
    if (!windows.isOpen(id)) return values.get(id);

    V value = values.computeIfAbsent(id, key -> factory.apply(player));
    if (!windows.isOpen(id)) values.remove(id);
    return value;
  }

  public @Nullable V compute(K player, BiFunction<? super K, ? super V, ? extends V> remapping) {
    UUID id = id(player);
    if (!windows.isOpen(id)) return values.get(id);

    V value = values.compute(id, (key, old) -> remapping.apply(player, old));
    if (!windows.isOpen(id)) values.remove(id);
    return value;
  }

  public void forEach(BiConsumer<UUID, ? super V> action) {
    values.forEach(action);
  }

  public Collection<V> values() {
    return List.copyOf(values.values());
  }

  public int size() {
    return values.size();
  }

  public boolean isEmpty() {
    return values.isEmpty();
  }

  public void clear() {
    values.clear();
  }

  @Override
  public void closed(UUID id) {
    values.remove(id);
  }

  private UUID id(K player) {
    return windows.idOf(player);
  }
}
