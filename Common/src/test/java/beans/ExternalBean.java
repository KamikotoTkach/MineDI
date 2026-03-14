package beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExternalBean {
  SomeBean1 dependency;
}
