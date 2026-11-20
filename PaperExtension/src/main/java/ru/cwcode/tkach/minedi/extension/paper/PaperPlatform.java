package ru.cwcode.tkach.minedi.extension.paper;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.cwcode.commands.Command;
import ru.cwcode.cwutils.ReloadCatcher;
import ru.cwcode.cwutils.bootstrap.Bootstrap;
import ru.cwcode.cwutils.scheduler.Tasks;
import ru.cwcode.tkach.config.commands.ReloadCommands;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.config.paper.PaperPluginConfigPlatform;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.module.SimpleModule;
import ru.cwcode.tkach.config.repository.yml.YmlRepositoryManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.config.*;
import ru.cwcode.tkach.minedi.condition.ConditionRegistry;
import ru.cwcode.tkach.minedi.condition.ConditionSource;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginDisableEvent;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginEnableEvent;
import ru.cwcode.tkach.minedi.extension.paper.logger.PaperLogger;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.scanner.PluginJar;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterRegistry;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PaperPlatform extends Bootstrap {
  private static final AtomicBoolean BUILTIN_STARTERS = new AtomicBoolean();

  @Getter
  protected DiApplication diApplication;
  @Getter
  protected YmlConfigManager ymlConfigManager;
  @Getter
  protected YmlRepositoryManager ymlRepositoryManager;
  protected PaperExtension paperExtension;
  
  protected Log logger;
  
  protected List<Runnable> preEnableHooks = new LinkedList<>();
  
  public void addPreEnableHook(Runnable runnable) {
    preEnableHooks.add(runnable);
  }
  
  @Override
  public @NotNull File getFile() {
    return super.getFile();
  }
  
  @Override
  public void onLoad() {
    logger = new PaperLogger(this);
    
    logger.debug("PreLoad");
    
    diApplication = new DiApplication(logger, getFile(), getClass().getPackageName(), getClassLoader());
    diApplication.setName(getName());
    diApplication.setForeignResolver(new PaperForeignBeanResolver());
    PluginJar pluginJar = new PluginJar(this::getName, this::getFile, getClass().getPackageName(), getClassLoader());
    ConditionRegistry.GLOBAL.addSource(ConditionSource.of(pluginJar));
    StarterRegistry.GLOBAL.addSource(pluginJar);
    registerBuiltinStarters();
    configureDiApplication(diApplication);
    diApplication.registerExtension(paperExtension = new PaperExtension(this));
    diApplication.load();
    
    ymlConfigManager = new YmlConfigManager(getConfigPlatform(), new ConfigCreator(diApplication));
    ymlRepositoryManager = new YmlRepositoryManager(ymlConfigManager);
    ymlConfigManager.addReplaceListener(new DiConfigReplaceListener(diApplication));
    diApplication.registerExtension(new ConfigMapperExtension(ymlConfigManager));

    ymlConfigManager.mapper().getMapper().setInjectableValues(new DiInjectableValues(diApplication));
    
    SimpleModule injectableExclusion = new SimpleModule();
    injectableExclusion.setSerializerModifier(new InjectFieldExclusionModifier());
    ymlConfigManager.mapper().module(injectableExclusion);
    
    diApplication.getBeanConstructors().getConstructors().addFirst(new ConfigConstructor(ymlConfigManager));
    diApplication.getBeanConstructors().getConstructors().addFirst(new RepositoryConstructor(ymlRepositoryManager));
    
    diApplication.register(diApplication);
    diApplication.register(logger, Log.class);
    diApplication.register(ymlConfigManager, YmlConfigManager.class);
    diApplication.register(ymlRepositoryManager, YmlRepositoryManager.class);
    diApplication.register(this, JavaPlugin.class);
    diApplication.register(this);
    
    
    logger.debug("PostLoad");
    initialize();
    
    logger.debug("PreAsyncTask");
    super.onLoad();
    logger.debug("PostAsyncTask");
  }
  
  protected void initialize() {
   
  }

  protected void configureDiApplication(DiApplication diApplication) {

  }
  
  @Override
  public void onDisable() {
    logger.debug("PreDisable");

    // Paper disables plugins before removing players, so no quit event will destroy their state
    diApplication.getContainer().getPlayerScope().closeAll();
    diApplication.getContainer().getPlayerScope().detach();

    ymlConfigManager.saveAll(options -> options.async(false)
                                               .silent(false));
    Tasks.cancelTasks(this);
    
    diApplication.getEventHandler().handleEvent(new PluginDisableEvent());
    
    logger.debug("PostDisable");
  }
  
  @Override
  public void onEnable() {
    preEnableHooks.removeIf(runnable -> {
      runnable.run();
      return true;
    });
    
    Bukkit.getPluginManager().registerEvents(new ReloadCatcher(), this);

    applyStarters();

    logger.debug("Pre start");
    diApplication.start();
    
    diApplication.getContainer().populateBeanFields(this);
    
    logger.debug("Pre extension enable");
    paperExtension.onPluginEnable();
    
    logger.debug("Pre async task waiting");
    super.onEnable();
    
    logger.debug("Pre plugin enabled event");
    diApplication.getEventHandler().handleEvent(new PluginEnableEvent());
  }
  
  private void applyStarters() {
    List<String> dependencies = new ArrayList<>(getDescription().getDepend());
    dependencies.addAll(getDescription().getSoftDepend());

    for (String dependency : dependencies) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin(dependency);
      if (plugin == null || plugin instanceof PaperPlatform || plugin.getClass().getClassLoader() == PaperPlatform.class.getClassLoader()) {
        continue; // MineDI plugins and MineDI itself are registered already
      }
      Class<?> main = plugin.getClass();
      StarterRegistry.GLOBAL.addDependencySource(plugin.getName(), () -> new PluginJar(plugin::getName, () -> jarOf(main),
                                                                                        main.getPackageName(), main.getClassLoader()));
    }

    StarterContext context = new StarterContext(diApplication, getName(), getDataFolder(), getClassLoader(), this);
    StarterRegistry.GLOBAL.apply(context, dependencies, name -> Bukkit.getPluginManager().getPlugin(name) != null);
  }

  /**
   * Starters shipped inside the MineDI jar (RefreshMenu, PacketEvents, LuckPerms); MineDI itself is not a PaperPlatform.
   */
  private static void registerBuiltinStarters() {
    if (!BUILTIN_STARTERS.compareAndSet(false, true)) return;

    StarterRegistry.GLOBAL.addSource(new PluginJar(() -> "MineDI", () -> jarOf(PaperPlatform.class), "ru.cwcode.tkach.minedi.starter",
                                                   PaperPlatform.class.getClassLoader()));
  }

  private static File jarOf(Class<?> clazz) {
    try {
      return new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot locate the jar of " + clazz.getName(), e);
    }
  }

  protected @NotNull PaperPluginConfigPlatform getConfigPlatform() {
    return new PaperPluginConfigPlatform(this);
  }
  
  protected Command paperReload() {
    return ReloadCommands.get(ymlConfigManager);
  }
}
