import isolated.extpoint.api.Badge;
import isolated.extpoint.early.BadgeBoard;
import isolated.extpoint.early.EarlyBadge;
import isolated.extpoint.late.LateBadge;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.extensionpoint.ConfigSubtype;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestExtensionPoints {
  static final String CLASSES = "target/test-classes/";

  final ExtensionRegistry registry = new ExtensionRegistry();

  DiApplication application(String name, String packagePrefix) {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, packagePrefix));
    application.setName(name);
    application.setExtensionRegistry(registry);
    return application;
  }

  @Test
  public void laterPluginExtendsTheView() {
    DiApplication early = application("early", "isolated.extpoint.early.");
    DiApplication late = application("late", "isolated.extpoint.late.");

    early.start();
    BadgeBoard board = early.get(BadgeBoard.class).orElseThrow();
    assertEquals(List.of("early"), List.copyOf(board.getById().keySet()), "own implementation arrives during start");

    int changesBefore = board.getChanges();
    late.start();

    assertEquals(List.of("early", "late"), List.copyOf(board.getById().keySet()));
    assertEquals(List.of("late", "early"), board.getBadges().stream().map(Badge::id).toList(), "@Priority(-1) goes first");
    assertTrue(board.getChanges() > changesBefore);
  }

  @Test
  public void plainListOnExtensionPointIsLive() {
    DiApplication early = application("early", "isolated.extpoint.early.");
    early.start();
    BadgeBoard board = early.get(BadgeBoard.class).orElseThrow();
    assertEquals(1, board.getLive().size());

    application("late", "isolated.extpoint.late.").start();

    assertEquals(2, board.getLive().size());
    assertThrows(UnsupportedOperationException.class, () -> board.getLive().add(new LateBadge()));
  }

  @Test
  public void onAddReplaysExistingImplementations() {
    application("early", "isolated.extpoint.early.").start();
    application("late", "isolated.extpoint.late.").start();

    List<String> seen = new ArrayList<>();
    registry.view(Badge.class, new TestLogger()).onAdd(badge -> seen.add(badge.id()));

    assertEquals(List.of("late", "early"), seen);
  }

  @Test
  public void recreatedBeanReplacesTheOldOne() {
    DiApplication early = application("early", "isolated.extpoint.early.");
    early.start();
    BadgeBoard board = early.get(BadgeBoard.class).orElseThrow();
    EarlyBadge old = early.get(EarlyBadge.class).orElseThrow();

    early.getContainer().recreate(EarlyBadge.class, null);
    EarlyBadge current = early.get(EarlyBadge.class).orElseThrow();

    assertNotSame(old, current);
    assertEquals(List.of(current), board.getBadges().list());
    assertSame(current, board.getById().get("early"));
  }

  @Test
  public void failingListenerDoesNotStopOthers() {
    application("early", "isolated.extpoint.early.").start();

    List<String> seen = new ArrayList<>();
    var view = registry.view(Badge.class, new TestLogger());
    view.onAdd(badge -> {
      throw new IllegalStateException("listener bug");
    });
    view.onAdd(badge -> seen.add(badge.id()));
    application("late", "isolated.extpoint.late.").start();

    assertEquals(List.of("early", "late"), seen);
  }

  @Test
  public void extensionsOfAPlainInterfaceFailStartup() {
    DiApplication application = application("bad", "isolated.extpointbad.");
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application::start);

    assertTrue(error.getMessage().contains("@ExtensionPoint"), error.getMessage());
    assertTrue(error.getMessage().contains("NotAPoint"), error.getMessage());
  }

  @Test
  public void duplicateSubtypeNameIsRejected() {
    assertEquals(Optional.empty(), registry.addSubtype(new ConfigSubtype(Object.class, String.class, "x", "a")));
    assertEquals(Optional.empty(), registry.addSubtype(new ConfigSubtype(Object.class, String.class, "x", "a")), "same class again is fine");

    Optional<ConfigSubtype> clash = registry.addSubtype(new ConfigSubtype(Object.class, Integer.class, "x", "b"));
    assertEquals(String.class, clash.orElseThrow().type());
    assertEquals(1, registry.subtypes().size());
  }
}
