package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigMapperCustomizer;

@Service
public class ProbeMapperCustomizer implements ConfigMapperCustomizer {
  static volatile boolean applied;

  @Override
  public void customize(YmlConfigManager manager) {
    applied = true;
  }
}
