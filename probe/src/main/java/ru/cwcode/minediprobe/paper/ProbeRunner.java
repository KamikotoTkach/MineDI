package ru.cwcode.minediprobe.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.cwutils.compat.AdventureCompats;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginEnableEvent;
import ru.cwcode.tkach.minedi.logging.Log;

import java.util.logging.Logger;

@Service
public class ProbeRunner {
  private static final String V26_CLASS = "ru.cwcode.minediprobe.paper.libs.compat.v26.V26AdventureCompat";

  JavaPlugin plugin;
  DiApplication application;
  Log log;
  GreetingService greetingService;
  Repeater repeater;
  AsyncWorker asyncWorker;

  int passed;
  int total;

  @EventListener
  void onEnable(PluginEnableEvent event) {
    check("config", greetingService.answer() == 42, "answer=" + greetingService.answer());
    check("static injection", StaticHolder.greetingService() == greetingService, "holder=" + StaticHolder.greetingService());

    ProbeListenerIntegration listener = application.get(ProbeListenerIntegration.class).orElse(null);
    check("bean hasPlugin(CWConfig)", listener != null, "listener=" + listener);
    check("bean hasPlugin(NoSuchPlugin) skipped", application.get(MissingIntegration.class).isEmpty(), "");
    Bukkit.getPluginManager().callEvent(new ProbeEvent());
    check("bean listener registered once", listener != null && listener.received == 1, "received=" + (listener == null ? "-" : listener.received));

    check("@Async/@Sync proxied", asyncWorker.getClass() != AsyncWorker.class, asyncWorker.getClass().getName());
    asyncWorker.work();

    check("compat selection", true, AdventureCompats.get().getClass().getName() + ", v26 class " + describeV26Class()
                                   + ", java " + System.getProperty("java.version"));

    log.warn("PROBE log throwable {}", "(stack trace must follow)", new IllegalStateException("probe-trace"));

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      check("@Async ran off the main thread", Boolean.FALSE.equals(asyncWorker.ranOnMainThread), "ranOnMainThread=" + asyncWorker.ranOnMainThread);
      check("@Sync from async returned the main thread's result", Boolean.TRUE.equals(asyncWorker.syncResultFromMainThread),
            "result=" + asyncWorker.syncResultFromMainThread);
      check("@Repeat ticks", repeater.ticks > 10, "ticks=" + repeater.ticks);
      logger().info("PROBE RESULT " + passed + "/" + total);
    }, 40);
  }

  private String describeV26Class() {
    try {
      Class.forName(V26_CLASS, false, getClass().getClassLoader());
      return "loadable";
    } catch (ClassNotFoundException | LinkageError e) {
      return "not loadable (" + e.getClass().getSimpleName() + ")";
    }
  }

  private void check(String name, boolean ok, String detail) {
    total++;
    if (ok) passed++;
    logger().info("PROBE " + (ok ? "OK   " : "FAIL ") + name + (detail.isEmpty() ? "" : " - " + detail));
  }

  private Logger logger() {
    return plugin.getLogger();
  }
}
