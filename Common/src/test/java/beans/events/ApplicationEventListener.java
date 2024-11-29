package beans.events;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.EventHandler;

@Getter
@Component
public class ApplicationEventListener {
  boolean eventInvoked = false;
  
  @EventHandler
  public void onApplicationEvent(CustomEvent event) {
    eventInvoked = true;
  }
}
