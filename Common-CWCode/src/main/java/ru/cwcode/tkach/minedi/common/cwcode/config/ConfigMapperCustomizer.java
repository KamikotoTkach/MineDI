package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;

/**
 * Changes the config mapper (Jackson subtypes, modules) before the first {@code @Config} or repository of the plugin is
 * read. Any bean implementing it takes part: a component of the plugin or one a starter adds with
 * {@code context.bean(customizer)}.
 * <p>
 * A customizer must not depend on a config or a repository: it runs before them, so such a dependency fails startup.
 */
public interface ConfigMapperCustomizer {
  void customize(YmlConfigManager manager);
}
