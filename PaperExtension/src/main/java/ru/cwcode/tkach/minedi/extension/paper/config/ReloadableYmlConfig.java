package ru.cwcode.tkach.minedi.extension.paper.config;

import ru.cwcode.tkach.config.annotation.Reloadable;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;
import ru.cwcode.tkach.minedi.DiApplication;

public class ReloadableYmlConfig extends YmlConfig implements Reloadable {
  
  transient DiApplication diApplication;
  
  @Override
  public boolean reload() {
    try {
      ReloadableYmlConfig config = diApplication.getContainer().create(this.getClass());
      diApplication.getContainer().updateBean(this.getClass(), config);
      return true;
      
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
