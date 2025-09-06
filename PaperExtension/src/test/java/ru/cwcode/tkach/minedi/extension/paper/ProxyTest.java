package ru.cwcode.tkach.minedi.extension.paper;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.NoOp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extension.paper.beans.ProxiedBean;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;


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
            if(event.getBean() instanceof ProxiedBean) {
              Enhancer enhancer = new Enhancer();
              enhancer.setSuperclass(ProxiedBean.class);
              
              enhancer.setCallbackFilter(method -> method.getName().equals("isProxied") ? 0 : 1);
              
              enhancer.setCallbacks(new Callback[]{
                (MethodInterceptor) (o, method, args, methodProxy) -> true,
                NoOp.INSTANCE
              });
              
              event.setReplacement(enhancer.create());
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
  
  @Test
  void testBeanConstructedEventProperlyCalled() {
    ProxiedBean proxiedBean = application.get(ProxiedBean.class).orElseThrow(RuntimeException::new);
    
    Assertions.assertTrue(proxiedBean.isProxied());
  }
}