package ru.cwcode.tkach.minedi.extension.paper.placeholder;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

public class PlaceholderAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final PlaceholderAdapter placeholderAdapter;
  
  public PlaceholderAnnotationProcessor(PlaceholderAdapter placeholderAdapter) {
    super(CustomMethodAnnotationEvent.class);
    this.placeholderAdapter = placeholderAdapter;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(Placeholder.class)) return;
    Placeholder annotation = (Placeholder) event.getAnnotation();
    
    String name = annotation.name();
    placeholderAdapter.register(event.getBean(), event.getMethod(), name);
  }
}
