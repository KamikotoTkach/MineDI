package ru.cwcode.tkach.minedi.extension.paper.listener;

import ru.cwcode.tkach.config.webeditor.PaperStarter;
import ru.cwcode.tkach.minedi.DiApplication;

public class ConfigReloadByWebEditorListener {
  public ConfigReloadByWebEditorListener(DiApplication application) {
    PaperStarter.INSTANCE.addReloadListener((previous, newConfig) -> {
      application.getLogger().info("%s updated by web editor".formatted(previous.getClass()));
      application.getContainer().recreate(previous.getClass(), newConfig);
    });
  }
}
