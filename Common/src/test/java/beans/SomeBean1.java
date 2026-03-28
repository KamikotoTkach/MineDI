package beans;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Component
@Getter
public class SomeBean1 {
  final SomeBean2 dep;
  
  SomeBean3 optionalDep;
  
  public SomeBean1(SomeBean2 dep) {
    this.dep = dep;
  }
}
