package beans;

import beans.integration.IntegratedBean;
import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class TestConfiguration {
  @Bean
  public IntegratedBean integratedBean() {
    return new IntegratedBean();
  }
  
  @Bean
  public ExternalBean externalBean() {
    return new ExternalBean();
  }
}
