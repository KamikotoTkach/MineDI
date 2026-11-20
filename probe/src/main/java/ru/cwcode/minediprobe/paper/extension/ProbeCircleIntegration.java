package ru.cwcode.minediprobe.paper.extension;

import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("circle")
public class ProbeCircleIntegration extends ProbeShape {
  public int radius;
}
