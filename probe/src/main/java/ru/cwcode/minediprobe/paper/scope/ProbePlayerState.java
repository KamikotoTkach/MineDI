package ru.cwcode.minediprobe.paper.scope;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.annotation.PreDestroy;
import ru.cwcode.tkach.minedi.scope.Creation;

/**
 * Created on join; counts its own player's commands and ticks, reports itself when destroyed.
 */
@PlayerScoped(create = Creation.JOIN)
public class ProbePlayerState {
  final Player player;
  final ProbeScopeLog log;
  int commands;
  int ticks;

  public ProbePlayerState(Player player, ProbeScopeLog log) {
    this.player = player;
    this.log = log;
  }

  @EventHandler
  void onCommand(PlayerCommandPreprocessEvent event) {
    if (event.getMessage().startsWith("/scopeping")) {
      commands++;
      event.setCancelled(true);
    }
  }

  @Repeat(delay = 1)
  void tick() {
    ticks++;
  }

  @PreDestroy
  void save() {
    log.destroyed(this);
  }
}
