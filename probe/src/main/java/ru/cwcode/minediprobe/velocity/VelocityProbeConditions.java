package ru.cwcode.minediprobe.velocity;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public final class VelocityProbeConditions {
  private VelocityProbeConditions() {
  }

  @DefineCondition
  public static boolean velocityAnswer(Integer value) {
    return value == 42;
  }
}
