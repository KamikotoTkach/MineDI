package ru.cwcode.tkach.minedi.extension.velocity.condition;

import com.velocitypowered.api.proxy.ProxyServer;
import ru.cwcode.tkach.minedi.condition.Condition;

/**
 * {@code hasPlugin(id)} — the plugin is installed on the proxy.
 */
public class HasPluginCondition implements Condition {
  private final ProxyServer server;

  public HasPluginCondition(ProxyServer server) {
    this.server = server;
  }

  @Override
  public String name() {
    return "hasPlugin";
  }

  @Override
  public boolean process(String value) {
    return server.getPluginManager().getPlugin(value.strip()).isPresent();
  }
}
