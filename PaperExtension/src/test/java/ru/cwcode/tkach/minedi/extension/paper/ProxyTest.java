package ru.cwcode.tkach.minedi.extension.paper;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extension.paper.beans.ProxiedBean;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.invoke.MethodHandles;

import static net.bytebuddy.matcher.ElementMatchers.named;


class ProxyTest {
  static DiApplication application;

  @BeforeAll
  static void setUpBeforeClass() {
    application = new DiApplication(new TestLogger(), new TestClassScanner("target/test-classes/"));
    application.registerExtension(new Extension() {
      @Override
      public void onRegister(DiApplication application) {
        application.getEventHandler().registerProcessor(new EventProcessor<>(BeanCreatedEvent.class) {
          @Override
          public void process(BeanCreatedEvent event, DiApplication application) {
            if (event.getBean() instanceof ProxiedBean) {
              event.setReplacement(proxy(event.getBean().getClass()));
            }
          }
        });
      }

      @Override
      public void onStart(DiApplication application) {

      }
    });
    application.start();
  }

  private static Object proxy(Class<?> beanClass) {
    try {
      return new ByteBuddy().subclass(beanClass)
                            .method(named("isProxied")).intercept(FixedValue.value(true))
                            .make()
                            .load(beanClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(beanClass, MethodHandles.lookup())))
                            .getLoaded()
                            .getDeclaredConstructor()
                            .newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void testBeanConstructedEventProperlyCalled() {
    ProxiedBean proxiedBean = application.get(ProxiedBean.class).orElseThrow(RuntimeException::new);

    Assertions.assertTrue(proxiedBean.isProxied());
  }
}
