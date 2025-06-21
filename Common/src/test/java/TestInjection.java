import beans.*;
import beans.circular.Cd1;
import beans.circularOptional.Cdo1;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.CircularDependencyException;

import static org.junit.jupiter.api.Assertions.*;

public class TestInjection {
  static DiApplication application;
  
  @BeforeAll
  public static void init() {
    application = new DiApplication(new TestLogger(), new TestClassScanner("target/test-classes/"));
    application.start();
  }
  
  @Test
  public void testPopulateExternalObject() {
    SimpleBean bean = application.get(SimpleBean.class).orElseThrow();
    ExternalObjectWithBeans external = new ExternalObjectWithBeans();
    
    application.getContainer().populateExternalObject(external);
    
    assertEquals(ExternalObjectWithBeans.getSimpleBeanStatic(), bean);
    assertEquals(external.getSimpleBean(), bean);
  }
  
  @Test
  public void testStaticBeanInject() {
    SimpleBean bean = application.get(SimpleBean.class).orElseThrow();
    
    assertEquals(StaticInject.getBean(), bean);
  }
  
  @Test
  public void testStaticBeanInjectUpdate() {
    SimpleBean newBeanInstance = new SimpleBean();
    
    application.getContainer().updateBean(SimpleBean.class, newBeanInstance);
    
    assertEquals(StaticInject.getBean(), newBeanInstance);
  }
  
  @Test
  public void testUpdateBean() {
    SomeBean1 val = application.get(SomeBean1.class).orElse(null);
    assertNotNull(val);
    
    SomeBean2 bean2 = val.getDep();
    assertNotNull(bean2);
    
    application.getContainer().updateBean(SomeBean2.class, new SomeBean2());
    SomeBean1 someBean1 = application.get(SomeBean1.class).orElse(null);
    
    assertNotSame(bean2, someBean1.getDep(), "invalid update bean");
  }
  
  @Test
  public void testBeanWithDependenciesIsProperlyInjected() {
    SomeBean1 someBean1 = application.get(SomeBean1.class).orElse(null);
    
    assertNotNull(someBean1);
    
    assertNotNull(someBean1.getDep());
    assertNotNull(someBean1.getOptionalDep());
  }
  
  @Test
  public void testLazyCircularDependenciesThrowsException() {
    assertThrows(CircularDependencyException.class, () -> application.get(Cd1.class).orElse(null));
  }
  
  @Test
  public void testLazyOptionalCircularDependenciesIsOk() {
    Cdo1 cdo1 = application.get(Cdo1.class).orElse(null);
    
    assertNotNull(cdo1);
    assertNotNull(cdo1.getCdo2());
  }
}
