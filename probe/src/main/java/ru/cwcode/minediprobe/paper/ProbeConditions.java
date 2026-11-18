package ru.cwcode.minediprobe.paper;

import ru.cwcode.tkach.minedi.annotation.DefineCondition;

public final class ProbeConditions {
  private ProbeConditions() {
  }

  @DefineCondition
  public static boolean probeAnswer(Integer value) {
    return value == 42;
  }
}
