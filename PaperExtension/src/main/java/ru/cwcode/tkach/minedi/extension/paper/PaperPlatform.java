package ru.cwcode.tkach.minedi.extension.paper;

import lombok.Getter;
import org.bukkit.Bukkit;
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
import ru.cwcode.tkach.minedi.extension.paper.event.PluginDisableEvent;
import ru.cwcode.tkach.minedi.extension.paper.event.PluginEnableEvent;
import ru.cwcode.tkach.minedi.extension.paper.logger.PaperLogger;
import ru.cwcode.tkach.minedi.logging.Log;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class PaperPlatform extends Bootstrap {
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
    
    diApplication = new DiApplication(logger, getFile(), getClass().getPackageName());
    diApplication.load();
    
    diApplication.registerExtension(paperExtension = new PaperExtension(this));
    
    ymlConfigManager = new YmlConfigManager(getConfigPlatform(), new ConfigCreator(diApplication));
    ymlRepositoryManager = new YmlRepositoryManager(ymlConfigManager);
    
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
  
  @Override
  public void onDisable() {
    logger.debug("PreDisable");
    
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
  
  protected @NotNull PaperPluginConfigPlatform getConfigPlatform() {
    return new PaperPluginConfigPlatform(this);
  }
  
  protected Command paperReload() {
    return ReloadCommands.get(ymlConfigManager, ymlConfig -> {
      diApplication.getContainer().updateBean(ymlConfig.getClass(), ymlConfig);
      diApplication.getEventHandler().handleEvent(new ConfigReloadEvent(ymlConfig));
    });
  }
}
