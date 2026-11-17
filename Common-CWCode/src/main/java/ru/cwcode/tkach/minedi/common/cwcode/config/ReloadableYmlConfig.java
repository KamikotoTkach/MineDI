package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.annotation.Reloadable;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;

public class ReloadableYmlConfig extends YmlConfig implements Reloadable {

  /**
   * Reads the file again under the same name. The bean swap and {@link ConfigReloadEvent} come from
   * {@link DiConfigReplaceListener}, the same path the web editor and {@code config fetch} take.
   */
  @Override
  public boolean reload() {
    try {
      return manager.load(name(), getClass()) != null;
    } catch (Exception e) {
      manager.platform().warning("Cannot reload config " + name(), e);
      return false;
    }
  }
}
