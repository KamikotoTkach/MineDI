import isolated.collections.AlphaHandler;
import isolated.collections.BetaHandler;
import isolated.collections.FilteredHolder;
import isolated.collections.HandlerCtorUser;
import isolated.collections.HandlerUser;
import isolated.collections.ZetaHandler;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestCollectionInjection {
  static final String CLASSES = "target/test-classes/";

  DiApplication application() {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, "isolated.collections."));
    application.setName("collections");
    application.setExtensionRegistry(new ExtensionRegistry());
    return application;
  }

  @Test
  public void fieldGetsCompatibleBeansInPriorityOrder() {
    DiApplication application = application();
    application.start();

    HandlerUser user = application.get(HandlerUser.class).orElseThrow();
    List<Class<?>> order = user.getHandlers().stream().<Class<?>>map(Object::getClass).toList();

    assertEquals(List.of(ZetaHandler.class, BetaHandler.class, AlphaHandler.class), order);
    assertSame(application.get(AlphaHandler.class).orElseThrow(), user.getHandlers().get(2));
    assertEquals(3, user.getHandlerSet().size());
    assertEquals(3, user.getWildcard().size());
  }

  @Test
  public void foreignAndPresetCollectionsAreLeftAlone() {
    DiApplication application = application();
    application.start();

    HandlerUser user = application.get(HandlerUser.class).orElseThrow();
    assertNull(user.getStrings(), "List<String> is not ours to fill");
    assertNull(user.getRunnables(), "a JDK interface must not collect beans");
    assertTrue(user.getPreset().isEmpty(), "a field with a value keeps it");
  }

  @Test
  public void constructorParameterGetsTheBeans() {
    DiApplication application = application();
    application.start();

    assertEquals(3, application.get(HandlerCtorUser.class).orElseThrow().getHandlers().size());
  }

  @Test
  public void filterExcludesClasses() {
    DiApplication application = application();
    application.getContainer().addCollectionInjectionFilter(type -> type != FilteredHolder.class);
    application.start();

    assertNull(application.get(FilteredHolder.class).orElseThrow().getHandlers());
    assertNotNull(application.get(HandlerUser.class).orElseThrow().getHandlers());
  }
}
