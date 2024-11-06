package beans.circularOptional;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;

@Component
@Lazy
@Getter
public class Cdo1 {
  Cdo2 cdo2;
}
