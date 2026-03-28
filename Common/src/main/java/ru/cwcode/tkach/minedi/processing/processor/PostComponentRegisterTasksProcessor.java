package ru.cwcode.tkach.minedi.processing.processor;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.ComponentsRegisteredEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostComponentRegisterTasksProcessor extends EventProcessor<ComponentsRegisteredEvent> {
  @Getter
  private List<Runnable> tasks = Collections.synchronizedList(new ArrayList<>());
  
  public PostComponentRegisterTasksProcessor() {
    super(ComponentsRegisteredEvent.class);
  }
  
  @Override
  public void process(ComponentsRegisteredEvent event, DiApplication application) {
    if (tasks.isEmpty()) return;
    tasks.forEach(Runnable::run);
    tasks = List.of();
  }
}
