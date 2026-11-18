package isolated.conditionsource;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public final class ForeignConditions {
  private ForeignConditions() {
  }

  @DefineCondition
  public static boolean serverType(String prefix) {
    return "hub-01".startsWith(prefix);
  }
}
