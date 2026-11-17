import isolated.compat.ambiguousctor.ShapeCtorUser;
import isolated.compat.core.*;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.AmbiguousBeanException;

import static org.junit.jupiter.api.Assertions.*;

public class TestCompatibleTypes {
  static final String CLASSES = "target/test-classes/";

  DiApplication startCore() {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, "isolated.compat.core."));
    application.start();
    return application;
  }

  @Test
  public void singleImplementationIsInjectedByInterface() {
    DiApplication application = startCore();
    GreeterImpl greeter = application.get(GreeterImpl.class).orElseThrow();

    assertSame(greeter, application.get(Greeter.class).orElseThrow());
    assertSame(greeter, application.get(GreeterUser.class).orElseThrow().getGreeter());
    assertSame(greeter, application.get(GreeterCtorUser.class).orElseThrow().getGreeter());
  }

  @Test
  public void recreatedImplementationReachesInterfaceFields() {
    DiApplication application = startCore();

    GreeterImpl recreated = new GreeterImpl();
    application.getContainer().recreate(GreeterImpl.class, recreated);

    assertSame(recreated, application.get(GreeterUser.class).orElseThrow().getGreeter());
  }

  @Test
  public void ambiguousFieldStaysNull() {
    DiApplication application = startCore();

    assertNull(application.get(ShapeHolder.class).orElseThrow().getShape());
  }

  @Test
  public void ambiguousGetThrows() {
    DiApplication application = startCore();

    AmbiguousBeanException error = assertThrows(AmbiguousBeanException.class, () -> application.get(Shape.class));
    assertTrue(error.getMessage().contains(Circle.class.getName()), error.getMessage());
    assertTrue(error.getMessage().contains(Square.class.getName()), error.getMessage());
  }

  @Test
  public void ambiguousConstructorParameterThrows() {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, "isolated.compat."));

    AmbiguousBeanException error = assertThrows(AmbiguousBeanException.class, application::start);
    assertTrue(error.getMessage().contains(ShapeCtorUser.class.getName()), error.getMessage());
  }
}
