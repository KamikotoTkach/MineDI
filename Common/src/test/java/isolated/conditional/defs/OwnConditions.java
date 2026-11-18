package isolated.conditional.defs;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

import java.util.Arrays;

public final class OwnConditions {
  private OwnConditions() {
  }

  @DefineCondition
  public static boolean answer(Integer value) {
    return value == 42;
  }

  @DefineCondition("allOf")
  public static boolean every(String first, String... rest) {
    return Arrays.stream(rest).allMatch(first::equals);
  }
}
