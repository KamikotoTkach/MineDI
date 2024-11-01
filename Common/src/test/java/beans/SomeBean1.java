package beans;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Required;

@Component
@Getter
public class SomeBean1 {
  @Required
  SomeBean2 dep;
  
  SomeBean3 optionalDep;
  
  public SomeBean1(SomeBean2 dep) {
    this.dep = dep;
  }
}
