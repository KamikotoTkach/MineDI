package beans.events;

import beans.SomeBean1;
import beans.SomeBean2;
import beans.SomeBean3;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;

@Component
public class Ebean1 {
  SomeBean1 someBean1;
  final SomeBean2 someBean2;
  SomeBean3 someBean3;
  
  @Getter
  boolean beanProperlyConstructed = false;
  
  public Ebean1(SomeBean1 someBean1, SomeBean2 someBean2) {
    this.someBean1 = someBean1;
    this.someBean2 = someBean2;
  }
  
  @EventListener
  private void onBeanConstructed(BeanConstructedEvent event) {
    if (someBean1 != null && someBean2 != null && someBean3 != null) {
      beanProperlyConstructed = true;
    }
  }
}
