package ru.cwcode.tkach.minedi.extension.velocity;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VelocityExtension implements Extension {
  final List<EventProcessor<? extends Event>> processors = new LinkedList<>();
  final List<Runnable> delayedTasks = new ArrayList<>();
  @Getter
  private final Object plugin;
  
  public VelocityExtension(Object velocityPlugin) {
    this.plugin = velocityPlugin;
  }
  
  @Override
  public void onRegister(DiApplication application) {
    processors.forEach(processor -> application.getEventHandler().registerProcessor(processor));
  }
  
  @Override
  public void onStart(DiApplication application) {
  }
  
  public void addDelayedTask(Runnable runnable) {
    delayedTasks.add(runnable);
  }
  
  public void onPluginEnable() {
    delayedTasks.forEach(Runnable::run);
    delayedTasks.clear();
  }
}
