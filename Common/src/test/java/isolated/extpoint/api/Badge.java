package isolated.extpoint.api;

import ru.cwcode.tkach.minedi.annotation.ExtensionPoint;

@ExtensionPoint
public interface Badge {
  String id();
}
