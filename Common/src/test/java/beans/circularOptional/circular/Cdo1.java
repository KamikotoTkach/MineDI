package beans.circularOptional.circular;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.annotation.Required;

@Component
@Lazy
@Getter
public class Cdo1 {
  Cdo2 cdo2;
}
