package ru.cwcode.minediprobe.paper.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "NoSuchPlugin")
public class AbsentStarter implements StarterExtension {
  static {
    StarterGift.absentStarterLoaded = true;
  }

  @Override
  public void apply(StarterContext context) {
  }
}
