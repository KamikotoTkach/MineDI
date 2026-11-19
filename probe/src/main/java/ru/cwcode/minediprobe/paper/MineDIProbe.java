package ru.cwcode.minediprobe.paper;

import ru.cwcode.minediprobe.paper.starter.ManualGift;
import ru.cwcode.tkach.minedi.extension.paper.PaperPlatform;

public final class MineDIProbe extends PaperPlatform {
  @Override
  public void onEnable() {
    diApplication.register(new ManualGift("manual"), ManualGift.class);
    super.onEnable();
  }
}
