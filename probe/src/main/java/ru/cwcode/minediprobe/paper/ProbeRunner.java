package ru.cwcode.minediprobe.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.cwutils.compat.AdventureCompats;
import ru.cwcode.minediprobe.paper.extension.ProbeBadge;
import ru.cwcode.minediprobe.paper.extension.ProbeCircleIntegration;
import ru.cwcode.minediprobe.paper.extension.ProbeShape;
import ru.cwcode.minediprobe.paper.present.PresentIntegration;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.minedi.extensionpoint.Extensions;
import ru.cwcode.minediprobe.paper.starter.LazyGift;
import ru.cwcode.minediprobe.paper.starter.MarkProcessor;
import ru.cwcode.minediprobe.paper.starter.StarterGift;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginEnableEvent;
import ru.cwcode.tkach.minedi.logging.Log;

import java.util.List;
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
  YmlConfigManager ymlConfigManager;
  Extensions<ProbeBadge> badges;
  List<ProbeBadge> liveBadges;

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
    check("static field of @Bean-made class", ProbeListenerIntegration.greetingService == greetingService,
          "field=" + ProbeListenerIntegration.greetingService);

    ProbeChain chain = application.get(ProbeChain.class).orElse(null);
    check("@Bean parameter from a later sibling", chain != null && chain.link() == application.get(ProbeLink.class).orElse(null),
          "chain=" + chain);

    // loading these would throw NoClassDefFoundError (Velocity API) and log "is not loaded and will not be scanned"
    check("@Conditional package false: classes not scanned",
          application.getContainer().findScannedClass("ru.cwcode.minediprobe.paper.missing.AbsentApiService") == null
          && application.getContainer().findScannedClass("ru.cwcode.minediprobe.paper.missing.MissingHelper") == null,
          "and no 'is not loaded' warning above");
    PresentIntegration present = application.get(PresentIntegration.class).orElse(null);
    check("@Conditional package true: Integration bean", present != null, "present=" + present);
    check("@Conditional bean static field", PresentIntegration.greetingService == greetingService, "field=" + PresentIntegration.greetingService);

    MarkedService marked = application.get(MarkedService.class).orElse(null);
    check("starter bean injected", marked != null && "starter".equals(marked.starterGift.origin), "marked=" + marked);
    check("manual registration beats starter", marked != null && "manual".equals(marked.manualGift.origin),
          "origin=" + (marked == null ? "-" : marked.manualGift.origin));
    check("starter processor saw the annotation", MarkProcessor.marks == 1, "marks=" + MarkProcessor.marks);
    check("starter of an absent plugin not loaded", !StarterGift.absentStarterLoaded, "");
    check("starter class is not a scanned component",
          application.getContainer().findScannedClass("ru.cwcode.minediprobe.paper.starter.ProbeStarter") == null, "");
    int lazyBefore = LazyGift.created;
    application.get(LazyGift.class);
    check("lazy starter bean created on demand", lazyBefore == 0 && LazyGift.created == 1, "before=" + lazyBefore + ", after=" + LazyGift.created);
    check("mapper customizer before the first config", ProbeConfig.createdAfterCustomizer, "");

    List<String> badgeIds = badges == null ? List.of() : badges.stream().map(ProbeBadge::id).toList();
    check("Extensions<T> collects own beans by @Priority", badgeIds.equals(List.of("first", "second")), "badges=" + badgeIds);
    check("List<T> on an extension point is live", liveBadges != null && liveBadges.size() == 2, "live=" + liveBadges);
    check("@ConfigSubtypes subtype read without registerSubtype", readCircle() instanceof ProbeCircleIntegration circle && circle.radius == 3,
          "shape=" + readCircle());

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
      check("@Repeat in a @Conditional class", present != null && present.ticks > 10, "ticks=" + (present == null ? "-" : present.ticks));
      logger().info("PROBE RESULT " + passed + "/" + total);
    }, 40);
  }

  private Object readCircle() {
    try {
      return ymlConfigManager.jacksonMapper().getMapper().readValue("type: circle\nradius: 3\n", ProbeShape.class);
    } catch (Exception e) {
      return e.toString();
    }
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
