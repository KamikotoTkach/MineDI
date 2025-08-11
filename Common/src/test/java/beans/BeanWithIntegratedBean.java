package beans;

import beans.integration.IntegratedBean;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class BeanWithIntegratedBean {
  IntegratedBean integratedBean;
  
  @Getter
  static IntegratedBean staticIntegratedBean;
}
