package ru.cwcode.tkach.minedi.extension.paper;

import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extension.paper.processor.BukkitListenerProcessor;
import ru.cwcode.tkach.minedi.extension.paper.processor.RepeatableAnnotationProcessor;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.util.LinkedList;
import java.util.List;

public class PaperExtension implements Extension {
  final JavaPlugin plugin;
  final List<EventProcessor<? extends Event>> processors;
  
  public PaperExtension(JavaPlugin plugin) {
    this.plugin = plugin;
    
    processors = new LinkedList<>();
    processors.add(new BukkitListenerProcessor(plugin));
    processors.add(new RepeatableAnnotationProcessor(plugin));
  }
  
  @Override
  public void onRegister(DiApplication application) {
    processors.forEach(processor -> application.getEventHandler().registerProcessor(processor));
  }
  
  @Override
  public void onStart(DiApplication application) {
  }
}
