package ru.cwcode.tkach.minedi.extension.velocity;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extension.velocity.processor.ListenerRegisterProcessor;
import ru.cwcode.tkach.minedi.extension.velocity.processor.PacketListenerAnnotationProcessor;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.util.LinkedList;
import java.util.List;

public class VelocityExtension implements Extension {
  final List<EventProcessor<? extends Event>> processors = new LinkedList<>();
  @Getter
  private final VelocityPlatform plugin;
  
  public VelocityExtension(VelocityPlatform velocityPlugin) {
    this.plugin = velocityPlugin;
    
    processors.add(new ListenerRegisterProcessor(this));
    
    if (velocityPlugin.getServer().getPluginManager().getPlugin("ipmc").isPresent()) {
      processors.add(new PacketListenerAnnotationProcessor(this));
    }
  }
  
  @Override
  public void onRegister(DiApplication application) {
    processors.forEach(processor -> application.getEventHandler().registerProcessor(processor));
  }
  
  @Override
  public void onStart(DiApplication application) {
  }
}
