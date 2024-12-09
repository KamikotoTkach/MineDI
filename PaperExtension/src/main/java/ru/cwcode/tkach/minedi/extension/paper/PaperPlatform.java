package ru.cwcode.tkach.minedi.extension.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.cwcode.commands.Command;
import ru.cwcode.cwutils.ReloadCatcher;
import ru.cwcode.cwutils.bootstrap.Bootstrap;
import ru.cwcode.cwutils.scheduler.Tasks;
import ru.cwcode.tkach.config.commands.ReloadCommands;
import ru.cwcode.tkach.config.jackson.JacksonConfigMapper;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.config.paper.PaperPluginConfigPlatform;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.module.SimpleModule;
import ru.cwcode.tkach.config.repository.yml.YmlRepositoryManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.config.ConfigCreator;
import ru.cwcode.tkach.minedi.extension.paper.config.ConfigConstructor;
import ru.cwcode.tkach.minedi.extension.paper.config.InjectFieldExclusionModifier;
import ru.cwcode.tkach.minedi.extension.paper.config.RepositoryConstructor;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginDisableEvent;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginEnableEvent;

import java.util.function.Supplier;
import java.util.logging.Logger;

public class PaperPlatform extends Bootstrap {
  protected boolean debug = false;
  
  protected DiApplication diApplication;
  
  protected YmlConfigManager ymlConfigManager;
  protected YmlRepositoryManager ymlRepositoryManager;
  protected PaperExtension paperExtension;
  
  protected Logger logger;
  
  public void debug(Supplier<String> log) {
    if (debug) logger.info(log.get());
  }
  
  @Override
  public void onLoad() {
    if (this.getDescription().getVersion().toLowerCase().contains("debug")) {
      debug = true;
    }
    
    debug(() -> "PreLoad");
    
    logger = getLogger();
    
    diApplication = new DiApplication(getFile(), getClass().getPackageName());
    diApplication.registerExtension(paperExtension = new PaperExtension(this));
    
    ymlConfigManager = new YmlConfigManager(getConfigPlatform(), new ConfigCreator(diApplication));
    ymlRepositoryManager = new YmlRepositoryManager(ymlConfigManager);
    
    ((JacksonConfigMapper) ymlConfigManager.mapper()).getMapper().setInjectableValues(new DiInjectableValues(diApplication));
    
    SimpleModule injectableExclusion = new SimpleModule();
    injectableExclusion.setSerializerModifier(new InjectFieldExclusionModifier());
    ((JacksonConfigMapper<YmlConfig>) ymlConfigManager.mapper()).module(injectableExclusion);
    
    diApplication.getBeanConstructors().getConstructors().addFirst(new ConfigConstructor(ymlConfigManager));
    diApplication.getBeanConstructors().getConstructors().addFirst(new RepositoryConstructor(ymlRepositoryManager));
    
    diApplication.register(diApplication);
    diApplication.register(logger, Logger.class);
    diApplication.register(ymlConfigManager, YmlConfigManager.class);
    diApplication.register(ymlRepositoryManager, YmlRepositoryManager.class);
    diApplication.register(this, JavaPlugin.class);
    diApplication.register(this);
    
    debug(() -> "PostLoad");
    
    initialize();
    
    debug(() -> "PreAsyncTask");
    super.onLoad();
    debug(() -> "PostAsyncTask");
  }
  
  protected void initialize() {
  
  }
  
  @Override
  public void onDisable() {
    debug(() -> "PreDisable");
    
    ymlConfigManager.saveAll(options -> options.async(false)
                                               .silent(false));
    Tasks.cancelTasks(this);
    
    diApplication.getEventHandler().handleEvent(new PluginDisableEvent());
    
    debug(() -> "PostDisable");
  }
  
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new ReloadCatcher(), this);
    
    diApplication.start();
    paperExtension.onPluginEnable();
    
    super.onEnable();
    
    diApplication.getEventHandler().handleEvent(new PluginEnableEvent());
  }
  
  protected @NotNull PaperPluginConfigPlatform getConfigPlatform() {
    return new PaperPluginConfigPlatform(this);
  }
  
  protected Command paperReload() {
    return ReloadCommands.get(ymlConfigManager, ymlConfig -> {
      diApplication.getContainer().updateBean(ymlConfig.getClass(), ymlConfig);
    });
  }
}
