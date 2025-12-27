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
    String name;
    if (event.getAnnotation().annotationType().equals(Placeholder.class)) {
      Placeholder annotation = (Placeholder) event.getAnnotation();
      name = annotation.name();
    } else if (event.getAnnotation().annotationType().equals(PapiPlaceholder.class)) {
      PapiPlaceholder annotation = (PapiPlaceholder) event.getAnnotation();
      name = annotation.name();
    } else return;
    
    placeholderAdapter.register(event.getBean(), event.getMethod(), name);
  }
}
