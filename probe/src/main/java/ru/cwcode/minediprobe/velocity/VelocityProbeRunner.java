package ru.cwcode.minediprobe.velocity;

import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.velocity.event.PluginEnableEvent;
import ru.cwcode.tkach.minedi.logging.Log;

@Service
public class VelocityProbeRunner {
  MineDIProbeVelocity plugin;
  Log log;
  VelocityProbeConfig config;
  VelocityProbeListener listener;

  int passed;
  int total;

  @EventListener
  void onEnable(PluginEnableEvent event) {
    check("config", config.answer == 42, "answer=" + config.answer);
    check("static injection", VelocityStaticHolder.config() == config, "holder=" + VelocityStaticHolder.config());

    plugin.getServer().getEventManager().fire(new VelocityProbeEvent()).join();
    check("@Subscribe service registered once", listener.received == 1, "received=" + listener.received);

    log.warn("PROBE log throwable {}", "(stack trace must follow)", new IllegalStateException("probe-trace"));
    plugin.getLogger().info("PROBE RESULT " + passed + "/" + total + ", java " + System.getProperty("java.version"));
  }

  private void check(String name, boolean ok, String detail) {
    total++;
    if (ok) passed++;
    plugin.getLogger().info("PROBE " + (ok ? "OK   " : "FAIL ") + name + " - " + detail);
  }
}
