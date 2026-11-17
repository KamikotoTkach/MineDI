package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.base.manager.ConfigReplaceListener;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;
import ru.cwcode.tkach.minedi.DiApplication;

/**
 * The one subscription of a plugin to config replacement: swaps the bean (a no-op for configs that
 * are not beans, such as config groups) and publishes {@link ConfigReloadEvent} for every reload,
 * web editor save and {@code config fetch}.
 */
public class DiConfigReplaceListener implements ConfigReplaceListener<YmlConfig> {
  private final DiApplication application;

  public DiConfigReplaceListener(DiApplication application) {
    this.application = application;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void onReplace(YmlConfig previous, YmlConfig current) {
    application.getContainer().recreate(current.getClass(), current);
    application.getEventHandler().handleEvent(new ConfigReloadEvent(current));
  }
}
