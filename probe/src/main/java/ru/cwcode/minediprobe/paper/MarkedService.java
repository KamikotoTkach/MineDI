package ru.cwcode.minediprobe.paper;

import ru.cwcode.minediprobe.paper.starter.ManualGift;
import ru.cwcode.minediprobe.paper.starter.ProbeMark;
import ru.cwcode.minediprobe.paper.starter.StarterGift;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class MarkedService {
  final StarterGift starterGift;
  final ManualGift manualGift;

  public MarkedService(StarterGift starterGift, ManualGift manualGift) {
    this.starterGift = starterGift;
    this.manualGift = manualGift;
  }

  @ProbeMark
  public void marked() {
  }
}
