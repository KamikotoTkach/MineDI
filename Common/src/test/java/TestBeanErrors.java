import isolated.errors.abstractbean.AbstractComponent;
import isolated.errors.missingdep.NeedsMissing;
import isolated.errors.missingdep.NotABean;
import isolated.errors.twoctors.TwoConstructors;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.exception.NoSuchBeanException;

import static org.junit.jupiter.api.Assertions.*;

public class TestBeanErrors {
  static final String CLASSES = "target/test-classes/";

  DiApplication application(String packagePrefix) {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, packagePrefix));
    application.setName("errors");
    return application;
  }

  @Test
  public void twoConstructorsFailStart() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application("isolated.errors.twoctors.")::start);

    assertTrue(error.getMessage().contains(TwoConstructors.class.getName()), error.getMessage());
    assertTrue(error.getMessage().contains("2 constructors"), error.getMessage());
  }

  @Test
  public void missingConstructorDependencyNamesBothClasses() {
    NoSuchBeanException error = assertThrows(NoSuchBeanException.class, application("isolated.errors.missingdep.")::start);

    assertTrue(error.getMessage().contains(NotABean.class.getName()), error.getMessage());
    assertTrue(error.getMessage().contains(NeedsMissing.class.getName()), error.getMessage());
  }

  @Test
  public void abstractComponentIsSkipped() {
    DiApplication application = application("isolated.errors.abstractbean.");
    application.start();

    assertFalse(application.getContainer().isBean(AbstractComponent.class));
  }
}
