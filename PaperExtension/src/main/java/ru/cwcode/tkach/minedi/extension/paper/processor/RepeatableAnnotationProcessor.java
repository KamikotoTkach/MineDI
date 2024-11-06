package ru.cwcode.tkach.minedi.extension.paper.processor;

import ru.cwcode.cwutils.scheduler.Scheduler;
import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RepeatableAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final PaperExtension extension;
  
  public RepeatableAnnotationProcessor(PaperExtension extension) {
    super(CustomMethodAnnotationEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(Repeat.class)) return;
    Repeat annotation = event.getMethod().getAnnotation(Repeat.class);
    
    Method method = event.getMethod();
    method.setAccessible(true);
    extension.addDelayedTask(() -> {
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
               .register(extension.getPlugin(), annotation.delay());
    });
  }
}
