package ru.cwcode.tkach.minedi.extension.paper.processor;

import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.cwutils.scheduler.Scheduler;
import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepeatableAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final JavaPlugin plugin;
  
  public RepeatableAnnotationProcessor(JavaPlugin plugin) {
    super(CustomMethodAnnotationEvent.class);
    this.plugin = plugin;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(Repeat.class)) return;
    Repeat annotation = event.getMethod().getAnnotation(Repeat.class);
    
    Method method = event.getMethod();
    method.setAccessible(true);
    
    Scheduler.create()
             .async(annotation.async())
             .infinite()
             .perform(() -> {
               try {
                 method.invoke(event.getBean());
               } catch (IllegalAccessException | InvocationTargetException e) {
                 throw new RuntimeException(e);
               }
             })
             .register(plugin, annotation.delay());
  }
}
