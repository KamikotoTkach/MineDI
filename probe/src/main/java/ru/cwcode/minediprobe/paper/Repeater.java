package ru.cwcode.minediprobe.paper;

import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * {@code @Repeat} goes through the ASM-generated method caller.
 */
@Service
public class Repeater {
  int ticks;

  @Repeat(delay = 1)
  void tick() {
    ticks++;
  }
}
