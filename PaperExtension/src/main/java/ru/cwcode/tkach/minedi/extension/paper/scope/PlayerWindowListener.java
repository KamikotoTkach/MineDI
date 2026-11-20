package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

/**
 * Drives {@link PlayerWindows#GLOBAL} for all MineDI plugins. Registered by the MineDI plugin itself, so at the same
 * priority it runs before PlayerDataStorage (which depends on MineDI): the window opens before PDS preloads at
 * {@code LOWEST} and closes, with every {@code @PreDestroy}, before PDS unloads at {@code HIGHEST}.
 */
public class PlayerWindowListener implements Listener {
  private final PlayerWindows windows;

  public PlayerWindowListener(PlayerWindows windows) {
    this.windows = windows;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  void open(PlayerJoinEvent event) {
    windows.open(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOW)
  void joined(PlayerJoinEvent event) {
    windows.joined(event.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  void close(PlayerQuitEvent event) {
    windows.close(event.getPlayer().getUniqueId());
  }
}
