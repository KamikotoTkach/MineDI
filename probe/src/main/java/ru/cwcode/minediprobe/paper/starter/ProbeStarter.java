package ru.cwcode.minediprobe.paper.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

/**
 * Applied to the probe because it lists CWConfig in softdepend; a starter of another plugin's jar works the same way.
 */
@Starter(plugin = "CWConfig")
public class ProbeStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    context.bean(new StarterGift("starter"), StarterGift.class);
    context.bean(new ManualGift("starter"), ManualGift.class);
    context.lazyBean(LazyGift.class, LazyGift::new);
    context.processor(new MarkProcessor());
  }
}
