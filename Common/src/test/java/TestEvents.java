import beans.events.ApplicationEventListener;
import beans.events.CustomEvent;
import beans.events.Ebean1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;

public class TestEvents {
  static DiApplication application;
  
  @BeforeAll
  static void setUpBeforeClass() {
    application = new DiApplication(new TestClassScanner("target/test-classes/"));
    application.start();
  }
  
  @Test
  void testBeanConstructedEventProperlyCalled() {
    Ebean1 ebean = application.get(Ebean1.class).orElseThrow(RuntimeException::new);
    
    Assertions.assertTrue(ebean.isBeanProperlyConstructed());
  }
  
  @Test
  void testApplicationEvents() {
    ApplicationEventListener applicationEventListener = application.get(ApplicationEventListener.class).get();
    application.getEventHandler().handleEvent(new CustomEvent(""));
    Assertions.assertTrue(applicationEventListener.isEventInvoked());
  }
}
