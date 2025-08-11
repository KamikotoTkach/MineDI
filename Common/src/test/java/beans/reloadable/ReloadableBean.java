package beans.reloadable;

import beans.SomeBean1;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.EventListener;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanDestroyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ReloadableBean {
  public static List<String> events = new ArrayList<>();
  public static int i = 0;
  
  @Getter
  SomeBean1 someBean1;
  int index;
  int random = ThreadLocalRandom.current().nextInt();
  
  @EventListener
  private void onInit(BeanConstructedEvent event) {
    events.add("init " + (index = i++));
  }
  
  @EventListener
  private void onDestroy(BeanDestroyEvent event) {
    if (event.getBean().equals(this)) {
      events.add("destroy " + index);
    }
  }
}
