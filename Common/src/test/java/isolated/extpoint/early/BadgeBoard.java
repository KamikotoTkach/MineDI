package isolated.extpoint.early;

import isolated.extpoint.api.Badge;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extensionpoint.Extensions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Service
public class BadgeBoard {
  private final Extensions<Badge> badges;
  private final Map<String, Badge> byId = new LinkedHashMap<>();
  List<Badge> live;
  int changes;

  public BadgeBoard(Extensions<Badge> badges) {
    this.badges = badges;
    badges.onAdd(badge -> byId.put(badge.id(), badge));
    badges.onChange(() -> changes++);
  }
}
