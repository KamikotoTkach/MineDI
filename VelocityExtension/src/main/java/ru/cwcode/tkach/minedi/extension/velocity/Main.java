package ru.cwcode.tkach.minedi.extension.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.Player;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(
  id = "minedi",
  name = "MineDI",
  version = "1.10.0",
  dependencies = {
    @Dependency(id = "cwcommands"),
    @Dependency(id = "cwconfig"),
    @Dependency(id = "ipmc", optional = true),
    @Dependency(id = "cwconfig_webeditor", optional = true),
  }
)
public class Main {
  static final List<VelocityPlatform> plugins = new ArrayList<>();
  
  @Inject
  private Logger logger;
  
  @Subscribe(order = PostOrder.FIRST)
  void openPlayerWindow(PostLoginEvent event) {
    PlayerWindows.GLOBAL.open(event.getPlayer());
    PlayerWindows.GLOBAL.joined(event.getPlayer().getUniqueId());
  }

  /**
   * Last: the disconnect handlers of plugins still see the player's state.
   */
  @Subscribe(order = PostOrder.LAST)
  void closePlayerWindow(DisconnectEvent event) {
    PlayerWindows.GLOBAL.close(event.getPlayer().getUniqueId());
  }

  @Subscribe(order = PostOrder.LATE)
  void onProxyInitialization(ProxyInitializeEvent event) {
    logger.info("MineDI initializing");
    PlayerWindows.GLOBAL.setKeyType(Player.class, Player::getUniqueId);

    for (VelocityPlatform plugin : plugins) {
      String pluginName = plugin.getClass().getSimpleName();
      try {
        pluginName = plugin.getPluginContainer().getDescription().getName().get();
        logger.info(pluginName + "  initializing");
        
        plugin.onProxyInitialization();
        
      } catch (Throwable e) {
        logger.log(Level.WARNING, "Cannot pass ProxyInitializeEvent to " + pluginName, e);
      }
    }
  }
}
