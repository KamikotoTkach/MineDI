package ru.cwcode.minediprobe.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Created only by a conditional {@code @Bean}; MineDI must register it with Bukkit exactly once.
 */
public class ProbeListenerIntegration implements Listener {
  int received;

  @EventHandler
  void onProbe(ProbeEvent event) {
    received++;
  }
}
