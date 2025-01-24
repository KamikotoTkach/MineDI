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
      logger.info(plugin.getPluginContainer().getDescription().getName().get() + "  initializing");
      
      plugin.onProxyInitialization();
    }
  }
}
