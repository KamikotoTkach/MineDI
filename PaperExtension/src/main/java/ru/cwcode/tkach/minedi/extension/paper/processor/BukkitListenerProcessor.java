package ru.cwcode.tkach.minedi.extension.paper.processor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;

public class BukkitListenerProcessor extends EventProcessor<BeanConstructedEvent> {
  JavaPlugin plugin;
  
  public BukkitListenerProcessor(JavaPlugin plugin) {
    super(BeanConstructedEvent.class);
    this.plugin = plugin;
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    if (event.getBean() instanceof Listener listener) {
      Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
  }
}
