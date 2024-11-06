package ru.cwcode.tkach.minedi.extension.paper.processor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

public class BukkitListenerProcessor extends EventProcessor<BeanConstructedEvent> {
  PaperExtension extension;
  
  public BukkitListenerProcessor(PaperExtension extension) {
    super(BeanConstructedEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    extension.addDelayedTask(() -> {
      if (event.getBean() instanceof Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, extension.getPlugin());
      }
    });
  }
}
