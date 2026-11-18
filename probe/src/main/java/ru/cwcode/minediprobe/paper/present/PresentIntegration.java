package ru.cwcode.minediprobe.paper.present;

import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.minediprobe.paper.GreetingService;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * Scanned despite the Integration suffix because its package has a condition.
 */
@Service
public class PresentIntegration {
  public static GreetingService greetingService;
  public int ticks;

  @Repeat(delay = 1)
  public void tick() {
    ticks++;
  }
}
