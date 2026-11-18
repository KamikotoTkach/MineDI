package isolated.conditionsource;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public final class ArgConditions {
  public enum Mode {SURVIVAL, Creative}

  private ArgConditions() {
  }

  @DefineCondition
  public static boolean numbers(int small, Long big, Double ratio, Boolean flag) {
    return small == 1 && big == 10_000_000_000L && ratio == 0.5 && flag;
  }

  @DefineCondition
  public static boolean mode(Mode mode) {
    return mode == Mode.Creative;
  }

  @DefineCondition
  public static boolean type(Class<?> type) {
    return type == String.class;
  }

  @DefineCondition
  public static boolean whole(String text) {
    return text.equals("a, b, c");
  }

  @DefineCondition
  public static boolean none() {
    return true;
  }

  @DefineCondition("renamed")
  public static Boolean nullable(String value) {
    return value.isEmpty() ? null : Boolean.TRUE;
  }

  @DefineCondition
  public static boolean throwing() {
    throw new IllegalStateException("boom");
  }
}
