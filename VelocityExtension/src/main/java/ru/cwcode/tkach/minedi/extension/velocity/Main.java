package ru.cwcode.tkach.minedi.extension.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Plugin(
  id = "minedi",
  name = "MineDI",
  version = "1.0.0",
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
  
  @Subscribe
  void onProxyInitialization(ProxyInitializeEvent event) {
    logger.info("MineDI initializing");
    
    for (VelocityPlatform plugin : plugins) {
      String pluginName = plugin.getClass().getSimpleName();
      try {
        pluginName = plugin.getPluginContainer().getDescription().getName().get();
        logger.info(pluginName + "  initializing");
        
        plugin.onProxyInitialization();
        
      } catch (Throwable e) {
        logger.warning("Cannot pass ProxyInitializeEvent to " + pluginName);
      }
    }
  }
}
