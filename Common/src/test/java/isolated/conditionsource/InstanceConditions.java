package isolated.conditionsource;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public class InstanceConditions {
  @DefineCondition
  public boolean notStatic() {
    return true;
  }
}
