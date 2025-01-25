package ru.cwcode.tkach.minedi.extension.velocity.processor;

import com.velocitypowered.api.event.Subscribe;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.velocity.VelocityExtension;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Method;

public class ListenerRegisterProcessor extends EventProcessor<BeanConstructedEvent> {
  VelocityExtension extension;
  
  public ListenerRegisterProcessor(VelocityExtension extension) {
    super(BeanConstructedEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    if (event.getBean().getClass().isAnnotationPresent(Service.class)) {
      if (event.getBean() == extension.getPlugin()) {
        return;
      }
      
      for (Method declaredMethod : event.getBean().getClass().getDeclaredMethods()) {
        if (declaredMethod.isAnnotationPresent(Subscribe.class)) {
          extension.getPlugin().getServer().getEventManager().register(extension.getPlugin(), event.getBean());
          return;
        }
      }
    }
  }
}
