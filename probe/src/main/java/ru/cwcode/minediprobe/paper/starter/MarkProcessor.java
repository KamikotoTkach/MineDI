package ru.cwcode.minediprobe.paper.starter;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

public class MarkProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  public static int marks;

  public MarkProcessor() {
    super(CustomMethodAnnotationEvent.class);
  }

  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (event.getAnnotation() instanceof ProbeMark) marks++;
  }
}
