package ru.cwcode.tkach.minedi.extension.paper;

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
import ru.cwcode.tkach.config.repository.yml.YmlRepositoryManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.config.ConfigCreator;
import ru.cwcode.tkach.minedi.extension.paper.config.ConfigConstructor;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.PlaceholderAdapter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class PaperPlatform extends Bootstrap {
  boolean debug = false;
  
  DiApplication diApplication;
  
  YmlConfigManager ymlConfigManager;
  YmlRepositoryManager ymlRepositoryManager;
  PaperExtension paperExtension;
  
  Logger logger;
  
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
    
    diApplication.getBeanConstructors().getConstructors().addFirst(new ConfigConstructor(ymlConfigManager));
    
    diApplication.register(diApplication);
    diApplication.register(logger, Logger.class);
    diApplication.register(ymlConfigManager, YmlConfigManager.class);
    diApplication.register(ymlRepositoryManager, YmlRepositoryManager.class);
    diApplication.register(this, JavaPlugin.class);
    diApplication.register(this);
    
    debug(() -> "PostLoad");
    
    
    debug(() -> "PreAsyncTask");
    super.onLoad();
    debug(() -> "PostAsyncTask");
  }
  
  @Override
  public void onDisable() {
    debug(() -> "PreDisable");
    
    ymlConfigManager.saveAll(configPersistOptions -> {});
    Tasks.cancelTasks(this);
    
    debug(() -> "PostDisable");
  }
  
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new ReloadCatcher(), this);
    paperExtension.onPluginEnable();
    
    super.onEnable();
  }
  
  protected @NotNull PaperPluginConfigPlatform getConfigPlatform() {
    return new PaperPluginConfigPlatform(this);
  }
  
  protected Command paperReload() {
    return ReloadCommands.get(ymlConfigManager, ymlConfig -> {
      diApplication.getContainer().updateBean(ymlConfig.getClass(), ymlConfig);
    });
  }
  
  @Override
  protected CompletableFuture<Void> asyncTask() {
    return CompletableFuture.runAsync(diApplication::start);
  }
}
