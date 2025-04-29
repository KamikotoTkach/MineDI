package beans.integration;

import beans.SimpleBean;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Component
@Getter
public class IntegratedBean {
  SimpleBean simpleBean;
}
