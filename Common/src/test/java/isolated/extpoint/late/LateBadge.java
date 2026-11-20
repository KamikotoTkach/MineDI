package isolated.extpoint.late;

import isolated.extpoint.api.Badge;
import ru.cwcode.tkach.minedi.annotation.Priority;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
@Priority(-1)
public class LateBadge implements Badge {
  @Override
  public String id() {
    return "late";
  }
}
