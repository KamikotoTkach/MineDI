package beans;

import lombok.Getter;

public class ExternalObjectWithBeans {
  @Getter
  static SimpleBean simpleBeanStatic;
  @Getter
  SimpleBean simpleBean;
}
