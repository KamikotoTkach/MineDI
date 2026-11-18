package isolated.conditional;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

@Conditional("flag(off)")
@Service
public class OffFlag {
  public static class Helper {
  }
}
