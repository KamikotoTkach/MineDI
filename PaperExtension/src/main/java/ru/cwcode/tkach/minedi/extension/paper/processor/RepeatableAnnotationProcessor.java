package ru.cwcode.tkach.minedi.extension.paper.processor;

import revxrsal.asm.BoundMethodCaller;
import revxrsal.asm.MethodCaller;
import ru.cwcode.cwutils.scheduler.Scheduler;
import ru.cwcode.cwutils.scheduler.annotationRepeatable.Repeat;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

public class RepeatableAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final PaperExtension extension;
  
  public RepeatableAnnotationProcessor(PaperExtension extension) {
    super(CustomMethodAnnotationEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(Repeat.class)) return;
    Repeat annotation = (Repeat) event.getAnnotation();
    
    BoundMethodCaller boundMethodCaller = MethodCaller.wrap(event.getMethod())
                                                      .bindTo(event.getBean());
    
    extension.addDelayedTask(() -> Scheduler.create()
                                          .async(annotation.async())
                                          .infinite()
                                          .perform(boundMethodCaller::call)
                                          .register(extension.getPlugin(), annotation.delay()));
  }
}
