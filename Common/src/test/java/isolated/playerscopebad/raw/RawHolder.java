package isolated.playerscopebad.raw;

import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.scope.ScopedBeans;

@Service
public class RawHolder {
  @SuppressWarnings("rawtypes")
  ScopedBeans scope;
}
