package ru.cwcode.tkach.minedi.extension.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import ru.cwcode.tkach.config.base.ConfigPlatform;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.module.SimpleModule;
import ru.cwcode.tkach.config.repository.yml.YmlRepositoryManager;
import ru.cwcode.tkach.config.velocityplatform.VelocityPluginConfigPlatform;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.config.*;
import ru.cwcode.tkach.minedi.extension.velocity.event.PluginDisableEvent;
import ru.cwcode.tkach.minedi.extension.velocity.event.PluginEnableEvent;

import java.io.File;
import java.nio.file.Path;

public abstract class VelocityPlatform {
  protected DiApplication diApplication;
  
  protected YmlConfigManager ymlConfigManager;
  protected YmlRepositoryManager ymlRepositoryManager;
  protected VelocityExtension velocityExtension;
  
  protected VelocityPlatform() {
    Main.plugins.add(this);  // т.к. нет возможности зарегать листенер до того, как плагин будет зарегистрирован,
                             // приходится делать вот такой трюк. (регаются только declared методы,
                             // а мы в абстрактном классе так-то)
  }
  
  public abstract ProxyServer getServer();
  
  public abstract Logger getLogger();
  
  public abstract Path getDataDirectory();
  
  public PluginContainer getPluginContainer() {
    return getServer().getPluginManager().ensurePluginContainer(this);
  }
  
  public File getPluginFile() {
    return getPluginContainer()
      .getDescription()
      .getSource().orElseThrow()
      .toFile();
  }
  
  void onProxyInitialization() {
    diApplication = new DiApplication(getPluginFile(), getClass().getPackageName());
    
    diApplication.registerExtension(velocityExtension = new VelocityExtension(this));
    
    ymlConfigManager = new YmlConfigManager(getConfigPlatform(), new ConfigCreator(diApplication));
    ymlRepositoryManager = new YmlRepositoryManager(ymlConfigManager);
    
    ymlConfigManager.mapper().getMapper().setInjectableValues(new DiInjectableValues(diApplication));
    
    SimpleModule injectableExclusion = new SimpleModule();
    injectableExclusion.setSerializerModifier(new InjectFieldExclusionModifier());
    ymlConfigManager.mapper().module(injectableExclusion);
    
    diApplication.getBeanConstructors().getConstructors().addFirst(new ConfigConstructor(ymlConfigManager));
    diApplication.getBeanConstructors().getConstructors().addFirst(new RepositoryConstructor(ymlRepositoryManager));
    
    diApplication.register(diApplication);
    diApplication.register(ymlConfigManager, YmlConfigManager.class);
    diApplication.register(ymlRepositoryManager, YmlRepositoryManager.class);
    diApplication.register(getPluginContainer(), PluginContainer.class);
    diApplication.register(getServer(), ProxyServer.class);
    diApplication.register(this);
    
    diApplication.getContainer().populateFields(this);
    
    onPrePluginLoad();
    
    diApplication.load();
    diApplication.start();
    
    diApplication.getEventHandler().handleEvent(new PluginEnableEvent());
    
    getServer().getEventManager().register(this, new Object() {
      @Subscribe
      void onProxyShutdown(ProxyShutdownEvent event) {
        VelocityPlatform.this.onProxyShutdown();
      }
    });
  }
  
  void onProxyShutdown() {
    ymlConfigManager.saveAll(options -> options.async(false)
                                               .silent(false));
    
    diApplication.getEventHandler().handleEvent(new PluginDisableEvent());
  }
  
  protected void onPrePluginLoad() {
  
  }
  
  private ConfigPlatform getConfigPlatform() {
    return new VelocityPluginConfigPlatform(getPluginContainer().getInstance().orElseThrow(), getServer(), getLogger(), getDataDirectory());
  }
}
