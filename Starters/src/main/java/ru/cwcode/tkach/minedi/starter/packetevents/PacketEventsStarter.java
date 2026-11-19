package ru.cwcode.tkach.minedi.starter.packetevents;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "packetevents")
public class PacketEventsStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    context.processor(new PacketAnnotationProcessor());
  }
}
