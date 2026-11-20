package isolated.extpoint.early;

import isolated.extpoint.api.Badge;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class EarlyBadge implements Badge {
  BadgeBoard board;

  @Override
  public String id() {
    return "early";
  }
}
