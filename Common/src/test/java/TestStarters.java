import isolated.starter.AlphaStarter;
import isolated.starter.BothStarter;
import isolated.starter.LazyService;
import isolated.starter.StarterLog;
import isolated.starter.StarterService;
import isolated.starterconsumer.NeedsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.scanner.ClassIndex;
import ru.cwcode.tkach.minedi.scanner.PluginJar;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestStarters {
  static final String CLASSES = "target/test-classes/";
  static final Set<String> INSTALLED = Set.of("Alpha", "beta", "Lazy", "Broken");

  @TempDir
  Path temp;

  StarterRegistry registry;
  File jar;

  @BeforeEach
  void setUp() throws IOException {
    StarterLog.events.clear();
    jar = starterJar();
    registry = new StarterRegistry();
    registry.addSource(new PluginJar(() -> "Provider", () -> jar, "isolated.starter", getClass().getClassLoader()));
  }

  DiApplication application(String packagePrefix) {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, packagePrefix));
    application.setName("Consumer");
    return application;
  }

  void apply(DiApplication application, String... dependencies) {
    StarterContext context = new StarterContext(application, "Consumer", temp.toFile(), getClass().getClassLoader(), new Object());
    registry.apply(context, List.of(dependencies), name -> INSTALLED.stream().anyMatch(name::equalsIgnoreCase));
  }

  @Test
  public void indexReadsSingleAndRepeatedStarters() {
    ClassIndex index = ClassIndex.ofJar(jar, "isolated.starter");

    assertEquals(List.of("Alpha"), starterPlugins(index, AlphaStarter.class));
    assertEquals(List.of("Alpha", "beta"), starterPlugins(index, BothStarter.class));
    assertEquals(List.of(), starterPlugins(index, StarterService.class));
    assertTrue(index.isStarterOrNested(AlphaStarter.class.getName() + "$1"));
  }

  @Test
  public void appliedByDependencyOrderThenClassName() {
    DiApplication application = application("isolated.nothing.");
    apply(application, "Beta", "Absent", "Alpha");

    assertEquals(List.of("BothStarter", "AlphaStarter for Consumer", "BothStarter"), StarterLog.events,
                 "plugin names compare ignoring case; an absent plugin's starter is never loaded");
  }

  @Test
  public void starterBeanIsInjectedIntoConsumer() {
    DiApplication application = application("isolated.starterconsumer.");
    apply(application, "Alpha");
    application.start();

    assertEquals("starter", application.get(NeedsService.class).orElseThrow().getService().origin);
  }

  @Test
  public void manualRegistrationWins() {
    DiApplication application = application("isolated.starterconsumer.");
    application.register(new StarterService("manual"), StarterService.class);
    apply(application, "Alpha");
    application.start();

    assertEquals("manual", application.get(NeedsService.class).orElseThrow().getService().origin);
  }

  @Test
  public void lazyBeanIsCreatedOnlyOnDemand() {
    DiApplication application = application("isolated.nothing.");
    LazyService.created = 0;
    apply(application, "Lazy");
    application.start();

    assertEquals(0, LazyService.created);
    LazyService service = application.get(LazyService.class).orElseThrow();
    assertSame(service, application.get(LazyService.class).orElseThrow());
    assertEquals(1, LazyService.created);
  }

  @Test
  public void failingStarterNamesStarterAndPlugin() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> apply(application("isolated.nothing."), "Broken"));

    assertTrue(error.getMessage().contains("isolated.starter.BrokenStarter"), error.getMessage());
    assertTrue(error.getMessage().contains("plugin Broken"), error.getMessage());
    assertTrue(error.getMessage().contains("no database"), error.getMessage());
  }

  private static List<String> starterPlugins(ClassIndex index, Class<?> clazz) {
    return index.entries().stream()
                .filter(entry -> entry.name().equals(clazz.getName()))
                .findFirst().orElseThrow()
                .starterPlugins();
  }

  private File starterJar() throws IOException {
    File result = temp.resolve("provider.jar").toFile();
    Path classes = Path.of(CLASSES, "isolated", "starter");

    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(result.toPath()));
         Stream<Path> files = Files.list(classes)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
        out.putNextEntry(new JarEntry("isolated/starter/" + file.getFileName()));
        out.write(Files.readAllBytes(file));
        out.closeEntry();
      }
    }
    return result;
  }
}
