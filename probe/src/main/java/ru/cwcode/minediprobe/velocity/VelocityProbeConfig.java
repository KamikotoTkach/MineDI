package ru.cwcode.minediprobe.velocity;

import ru.cwcode.tkach.minedi.common.cwcode.config.Config;
import ru.cwcode.tkach.minedi.common.cwcode.config.ReloadableYmlConfig;

@Config
public class VelocityProbeConfig extends ReloadableYmlConfig {
  public int answer = 42;
}
