package ru.cwcode.tkach.minedi.extension.paper;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.PlaceholderAdapter;
import ru.cwcode.tkach.minedi.extension.paper.placeholder.PlaceholderAnnotationProcessor;
import ru.cwcode.tkach.minedi.extension.paper.processor.BukkitListenerProcessor;
import ru.cwcode.tkach.minedi.extension.paper.processor.RepeatableAnnotationProcessor;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PaperExtension implements Extension {
  @Getter
  final JavaPlugin plugin;
  final List<EventProcessor<? extends Event>> processors;
  final List<Runnable> delayedTasks = new ArrayList<>();
  
  public PaperExtension(JavaPlugin plugin) {
    this.plugin = plugin;
    
    processors = new LinkedList<>();
    processors.add(new BukkitListenerProcessor(this));
    processors.add(new RepeatableAnnotationProcessor(this));
    
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      processors.add(new PlaceholderAnnotationProcessor(new PlaceholderAdapter(this)));
    }
  }
  
  public void addDelayedTask(Runnable runnable) {
    delayedTasks.add(runnable);
  }
  
  @Override
  public void onRegister(DiApplication application) {
    processors.forEach(processor -> application.getEventHandler().registerProcessor(processor));
  }
  
  @Override
  public void onStart(DiApplication application) {
  }
  
  public void onPluginEnable() {
    delayedTasks.forEach(Runnable::run);
    delayedTasks.clear();
  }
}
