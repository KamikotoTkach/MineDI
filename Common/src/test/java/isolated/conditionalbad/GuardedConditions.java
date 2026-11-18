package isolated.conditionalbad;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public final class GuardedConditions {
  private GuardedConditions() {
  }

  @DefineCondition
  public static boolean guarded() {
    return true;
  }
}
