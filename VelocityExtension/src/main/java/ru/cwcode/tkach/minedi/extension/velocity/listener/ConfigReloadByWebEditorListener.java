package ru.cwcode.tkach.minedi.extension.velocity.listener;

import ru.cwcode.tkach.config.webeditor.VelocityStarter;
import ru.cwcode.tkach.minedi.DiApplication;

public class ConfigReloadByWebEditorListener {
  public ConfigReloadByWebEditorListener(DiApplication application) {
    VelocityStarter.INSTANCE.addReloadListener((previous, newConfig) -> {
      application.getLogger().info("%s updated by web editor".formatted(previous.getClass()));
      application.getContainer().recreate(previous.getClass(), newConfig);
    });
  }
}
