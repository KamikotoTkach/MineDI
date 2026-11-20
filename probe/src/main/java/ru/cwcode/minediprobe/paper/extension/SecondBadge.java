package ru.cwcode.minediprobe.paper.extension;

import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class SecondBadge implements ProbeBadge {
  @Override
  public String id() {
    return "second";
  }
}
