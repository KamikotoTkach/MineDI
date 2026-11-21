import isolated.beanorder.ok.Consumer;
import isolated.beanorder.ok.MadeByBean;
import isolated.beanorder.ok.Marked;
import isolated.beanorder.ok.OrderConfiguration;
import isolated.beanorder.ok.Producer;
import isolated.beanorder.ok.Skipped;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

public class TestBeanMethods {
  static final String CLASSES = "target/test-classes/";

  DiApplication application(String packagePrefix) {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, packagePrefix));
    application.setName("beans");
    return application;
  }

  @Test
  public void beanOfJdkModuleClassStarts() {
    DiApplication application = application("isolated.jdkbean.");
    application.setPlayerWindows(new PlayerWindows());
    application.start();

    assertNotNull(application.get(Clock.class).orElseThrow().instant());
  }

  @Test
  public void parameterProducedBySiblingRunsItFirst() {
    DiApplication application = application("isolated.beanorder.ok.");
    application.excludeClassFromAutoScan(MadeByBean.class.getName());
    OrderConfiguration.markedCalls = 0;
    application.start();

    Producer producer = application.get(Producer.class).orElseThrow();
    assertSame(producer, application.get(Consumer.class).orElseThrow().getProducer());
    assertTrue(application.get(Marked.class).isPresent());
    assertTrue(application.get(Skipped.class).isEmpty());
    assertEquals(1, OrderConfiguration.markedCalls, "a @Bean method with a second annotation runs once");
  }

  @Test
  public void staticFieldsOfBeanMadeClassAreInjected() {
    DiApplication application = application("isolated.beanorder.ok.");
    application.excludeClassFromAutoScan(MadeByBean.class.getName());
    application.start();

    assertSame(application.get(Producer.class).orElseThrow(), MadeByBean.producer);
    assertSame(application.get(Consumer.class).orElseThrow(), MadeByBean.consumer);

    Producer reloaded = new Producer();
    application.getContainer().recreate(Producer.class, reloaded);
    assertSame(reloaded, MadeByBean.producer);
  }

  @Test
  public void cycleBetweenBeanMethodsIsReported() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application("isolated.beanorder.cycle.")::start);

    assertTrue(error.getMessage().contains("cycle"), error.getMessage());
    assertTrue(error.getMessage().contains("CycleConfiguration.left()"), error.getMessage());
    assertTrue(error.getMessage().contains("CycleConfiguration.right()"), error.getMessage());
  }

  @Test
  public void parameterFromDisabledMethodIsReported() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application("isolated.beanorder.disabled.")::start);

    assertTrue(error.getMessage().contains("DisabledConfiguration.source()"), error.getMessage());
    assertTrue(error.getMessage().contains("condition is false"), error.getMessage());
  }
}
