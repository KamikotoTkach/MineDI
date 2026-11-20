package ru.cwcode.tkach.minedi.scope;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A set of online players that loses a player when the player leaves; adding a player outside the window does nothing.
 * Platforms subclass it with their player type ({@code PlayerSet}).
 */
public class KeyedSet<K> implements PlayerWindows.WindowListener {
  private final PlayerWindows windows;
  private final Set<UUID> ids = ConcurrentHashMap.newKeySet();

  public KeyedSet(PlayerWindows windows) {
    this.windows = windows;
    windows.addListener(this);
  }

  /**
   * @return true when the player was added; false when already present or not online
   */
  public boolean add(K player) {
    UUID id = id(player);
    if (!windows.isOpen(id)) return false;

    boolean added = ids.add(id);
    if (!windows.isOpen(id)) ids.remove(id);
    return added;
  }

  public boolean remove(K player) {
    return ids.remove(id(player));
  }

  public boolean contains(K player) {
    return ids.contains(id(player));
  }

  public boolean contains(UUID player) {
    return ids.contains(player);
  }

  public Set<UUID> ids() {
    return Set.copyOf(ids);
  }

  public int size() {
    return ids.size();
  }

  public boolean isEmpty() {
    return ids.isEmpty();
  }

  public void clear() {
    ids.clear();
  }

  @Override
  public void closed(UUID id) {
    ids.remove(id);
  }

  private UUID id(K player) {
    return windows.idOf(player);
  }
}
