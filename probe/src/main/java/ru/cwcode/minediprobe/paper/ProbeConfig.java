package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.minedi.common.cwcode.config.Config;
import ru.cwcode.tkach.minedi.common.cwcode.config.ReloadableYmlConfig;

@Config
public class ProbeConfig extends ReloadableYmlConfig {
  public int answer = 42;
}
