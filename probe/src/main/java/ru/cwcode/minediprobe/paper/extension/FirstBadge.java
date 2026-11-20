package ru.cwcode.minediprobe.paper.extension;

import ru.cwcode.tkach.minedi.annotation.Priority;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
@Priority(-1)
public class FirstBadge implements ProbeBadge {
  @Override
  public String id() {
    return "first";
  }
}
