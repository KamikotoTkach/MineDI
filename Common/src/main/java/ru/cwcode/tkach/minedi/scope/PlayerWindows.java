package ru.cwcode.tkach.minedi.scope;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * The windows in which player state may exist: from the join until the quit. The platform drives it
 * ({@link #open}, {@link #joined}, {@link #synced}, {@link #close}); player scopes and player maps of every plugin
 * listen to it. One instance per server ({@link #GLOBAL}); tests make their own.
 */
public class PlayerWindows {
  public static final PlayerWindows GLOBAL = new PlayerWindows();

  private final Map<UUID, Window> windows = new ConcurrentHashMap<>();
  private final List<WindowListener> listeners = new CopyOnWriteArrayList<>();
  private volatile @Nullable Class<?> keyType;
  private volatile Function<Object, UUID> idOf = key -> {
    throw new IllegalStateException("The platform has not set the player type of PlayerWindows");
  };
  private volatile boolean syncSource;

  /**
   * The platform player type: it is what a player bean receives as its player and what player maps are keyed by.
   */
  public synchronized <K> void setKeyType(Class<K> type, Function<? super K, UUID> idOf) {
    this.keyType = type;
    this.idOf = key -> idOf.apply(type.cast(key));
  }

  public @Nullable Class<?> keyType() {
    return keyType;
  }

  public UUID idOf(Object key) {
    return idOf.apply(key);
  }

  /**
   * Declares that {@link #synced} will be called for every joining player; until then {@link Creation#SYNC} means
   * {@link Creation#JOIN}.
   */
  public void useSyncSource() {
    syncSource = true;
  }

  public boolean hasSyncSource() {
    return syncSource;
  }

  public void addListener(WindowListener listener) {
    listeners.add(listener);
  }

  public void removeListener(WindowListener listener) {
    listeners.remove(listener);
  }

  /**
   * Opens the window before anything of the join runs; nothing is created yet.
   */
  public void open(Object key) {
    windows.put(idOf(key), new Window(key));
  }

  /**
   * The join is far enough for state to be created: instances with {@link Creation#JOIN} (and {@link Creation#SYNC}
   * without a sync source) are made now.
   */
  public void joined(UUID id) {
    Window window = windows.get(id);
    if (window == null) return;

    listeners.forEach(listener -> listener.joined(id, window.key));
  }

  public void synced(UUID id) {
    Window window = windows.get(id);
    if (window == null) return;

    window.synced = true;
    listeners.forEach(listener -> listener.synced(id, window.key));
  }

  /**
   * Closes the window: listeners drop the player's state, the latest registered first, so a plugin's state goes before
   * the state of the plugins it depends on.
   */
  public void close(UUID id) {
    if (windows.remove(id) == null) return;

    List<WindowListener> reversed = new ArrayList<>(listeners);
    Collections.reverse(reversed);
    reversed.forEach(listener -> listener.closed(id));
  }

  public boolean isOpen(UUID id) {
    return windows.containsKey(id);
  }

  public boolean isOpen(UUID id, Creation creation) {
    Window window = windows.get(id);
    if (window == null) return false;

    return creation != Creation.SYNC || !syncSource || window.synced;
  }

  public @Nullable Object key(UUID id) {
    Window window = windows.get(id);
    return window == null ? null : window.key;
  }

  public List<UUID> openIds() {
    return List.copyOf(windows.keySet());
  }

  public interface WindowListener {
    default void joined(UUID id, Object key) {
    }

    default void synced(UUID id, Object key) {
    }

    void closed(UUID id);
  }

  private static final class Window {
    final Object key;
    volatile boolean synced;

    Window(Object key) {
      this.key = key;
    }
  }
}
