import beans.reloadable.BeanWithReloadableBean;
import beans.reloadable.ReloadableBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.BoundEventListener;
import ru.cwcode.tkach.minedi.processing.EventHandlerImpl;
import ru.cwcode.tkach.minedi.processing.event.BeanDestroyEvent;

import java.util.List;
import java.util.Optional;

public class TestBeanReloading {
  static DiApplication application;
  static Optional<ReloadableBean> destroyed; //to avoid injection
  static Optional<ReloadableBean> recreated;
  
  @BeforeAll
  public static void setUpBeforeClass() {
    ReloadableBean.i = 0;
    ReloadableBean.events.clear();
    
    application = new DiApplication(new TestLogger(), new TestClassScanner("target/test-classes/"));
    application.start();
    
    destroyed = Optional.of(application.get(ReloadableBean.class).orElseThrow());
    recreated = Optional.of(application.getContainer().recreate(ReloadableBean.class, null));
  }
  
  @Test
  public void testReloadingSuccess() {
    Assertions.assertNotNull(recreated.get());
    Assertions.assertNotEquals(destroyed.get(), recreated.get());
  }
  
  @Test
  public void testReloadableBeanInjected() {
    BeanWithReloadableBean beanWithReloadableBean = application.get(BeanWithReloadableBean.class).orElseThrow();
    Assertions.assertEquals(beanWithReloadableBean.getReloadableBean(), recreated.get());
  }
  
  @Test
  public void testReloadableBeanStaticInjected() {
    Assertions.assertEquals(BeanWithReloadableBean.getStaticReloadableBean(), recreated.get());
  }
  
  @Test
  public void testReloadableBeanEvents() {
    Assertions.assertEquals(2, ReloadableBean.i);
    Assertions.assertIterableEquals(List.of("init 0", "destroy 0", "init 1"), ReloadableBean.events);
  }
  
  @Test
  public void testReloadableBeanPopulated() {
    Assertions.assertNotNull(recreated.get().getSomeBean1());
  }
  
  @Test
  public void testBeanApplicationEventHandlersCleared() {
    EventHandlerImpl eventHandler = (EventHandlerImpl) application.getEventHandler();
    for (BoundEventListener eventListener : eventHandler.getEventListeners(BeanDestroyEvent.class)) {
      Assertions.assertNotEquals(destroyed.get(), eventListener.bean());
    }
  }
}
