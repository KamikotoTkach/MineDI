package ru.cwcode.minediprobe.paper.scope;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.scope.PlayerMap;
import ru.cwcode.tkach.minedi.extension.paper.scope.PlayerScope;
import ru.cwcode.tkach.minedi.logging.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks the player scope from a singleton with the bot: join, a command, quit, rejoin; "SCOPE CHECK" lines in the log.
 */
@Service
public class ProbeScopeLog implements Listener {
  final Log logger;
  PlayerScope<ProbePlayerState> states;
  PlayerMap<Integer> joins;
  final Map<String, Integer> previousIdentity = new HashMap<>();
  int destroyed;

  public ProbeScopeLog(Log logger) {
    this.logger = logger;
  }

  @EventHandler
  void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    ProbePlayerState state = states.find(player).orElse(null);
    check("JOIN bean created before NORMAL", state != null, "");
    check("player injected", state != null && state.player == player, "");
    check("player map empty on (re)join", joins.get(player) == null, "value=" + joins.get(player));
    joins.put(player, 1);

    Integer previous = previousIdentity.get(player.getName());
    if (state != null && previous != null) {
      check("rejoin gives a new instance", previous != System.identityHashCode(state), "");
    }
    if (state != null) previousIdentity.put(player.getName(), System.identityHashCode(state));
  }

  @EventHandler
  void onQuit(PlayerQuitEvent event) {
    ProbePlayerState state = states.find(event.getPlayer()).orElse(null);
    check("state alive in a NORMAL quit handler", state != null, "");
    boolean firstSession = destroyed == 0; // the bot sends its two commands before the first quit only
    if (firstSession) {
      check("event reached the player's instance", state != null && state.commands == 2, "commands=" + (state == null ? "-" : state.commands));
    }
    check("@Repeat visits the instance", state != null && state.ticks > 5, "ticks=" + (state == null ? "-" : state.ticks));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  void afterQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    check("state gone after HIGHEST", states.find(player).isEmpty(), "");
    check("@PreDestroy ran", destroyed > 0, "destroyed=" + destroyed);
    check("player map entry dropped", joins.get(player) == null, "");
    joins.put(player, 5);
    check("late player map write ignored", joins.get(player) == null, "");
    check("get after quit throws", throwsOnGet(player), "");
  }

  void destroyed(ProbePlayerState state) {
    destroyed++;
    logger.info("SCOPE destroyed {} commands={} ticks={}", state.player.getName(), state.commands, state.ticks);
  }

  private boolean throwsOnGet(Player player) {
    try {
      states.get(player);
      return false;
    } catch (IllegalStateException e) {
      return true;
    }
  }

  private void check(String name, boolean ok, String detail) {
    logger.info("SCOPE CHECK {} {} {}", ok ? "OK" : "FAIL", name, detail);
  }
}
