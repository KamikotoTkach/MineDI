package beans.events;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.EventListener;

@Getter
@Component
public class ApplicationEventListener {
  boolean eventInvoked = false;
  
  @EventListener
  public void onApplicationEvent(CustomEvent event) {
    eventInvoked = true;
  }
}
