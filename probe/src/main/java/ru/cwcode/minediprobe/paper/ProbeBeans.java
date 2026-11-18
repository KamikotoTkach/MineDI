package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class ProbeBeans {
  /**
   * Sorted first by name, but needs the link: MineDI must call bLink() before it.
   */
  @Bean
  public ProbeChain aChain(ProbeLink link) {
    return new ProbeChain(link);
  }

  @Bean
  public ProbeLink bLink() {
    return new ProbeLink();
  }

  @Bean(condition = "hasPlugin(CWConfig)", as = ProbeListenerIntegration.class)
  public ProbeListenerIntegration present() {
    return new ProbeListenerIntegration();
  }

  @Bean(condition = "hasPlugin(NoSuchPlugin)", as = MissingIntegration.class)
  public MissingIntegration missing() {
    return new MissingIntegration();
  }
}
