package beans.reloadable;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class BeanWithReloadableBean {
  @Getter
  static ReloadableBean staticReloadableBean;
  ReloadableBean reloadableBean;
}
