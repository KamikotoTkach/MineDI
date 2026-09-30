package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class ProbeBeans {
  @Bean(condition = "hasPlugin(CWConfig)", as = ProbeListenerIntegration.class)
  public ProbeListenerIntegration present() {
    return new ProbeListenerIntegration();
  }

  @Bean(condition = "hasPlugin(NoSuchPlugin)", as = MissingIntegration.class)
  public MissingIntegration missing() {
    return new MissingIntegration();
  }
}
