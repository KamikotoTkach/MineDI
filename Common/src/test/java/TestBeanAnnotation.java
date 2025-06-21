import beans.integration.IntegratedBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;

public class TestBeanAnnotation {
  static DiApplication application;
  
  @BeforeAll
  static void setUpBeforeClass() {
    application = new DiApplication(new TestLogger(), new TestClassScanner("target/test-classes/"));
    application.start();
  }
  
  @Test
  public void testIntegratedBeanPopulated() {
    IntegratedBean integratedBean = application.get(IntegratedBean.class).orElseThrow();
    Assertions.assertNotNull(integratedBean.getSimpleBean());
  }
}
