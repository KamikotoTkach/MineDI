import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.condition.Condition;
import ru.cwcode.tkach.minedi.condition.ConditionRegistry;
import ru.cwcode.tkach.minedi.extensionpoint.ConfigSubtype;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Subtypes are found in a real jar through its bytecode and loaded by a child-first loader, as on a server.
 */
public class TestConfigSubtypes {
  static final Path CLASSES = Path.of("target/test-classes");
  static final String PACKAGE = "isolated.subtypes";

  @TempDir
  Path temp;

  @Test
  public void subtypesOfTheJarArePublished() throws Exception {
    ExtensionRegistry registry = new ExtensionRegistry();
    try (TestConditionalScan.ChildFirstLoader loader = start(registry)) {
      Map<String, String> byName = registry.subtypes().stream()
                                           .collect(Collectors.toMap(ConfigSubtype::name, subtype -> subtype.type().getName()));

      assertEquals(Map.of("gem", PACKAGE + ".GemReward",
                          "coin", PACKAGE + ".CoinIntegration",
                          "ext", PACKAGE + ".ExternalSubtype"), byName,
                   "an Integration suffix, an abstract middle class and a base from another jar all count; no name, no subtype");

      ConfigSubtype coin = registry.subtypes().stream().filter(subtype -> subtype.name().equals("coin")).findFirst().orElseThrow();
      assertEquals(PACKAGE + ".Reward", coin.base().getName());
      assertEquals("subtypes", coin.plugin());
    }
  }

  @Test
  public void falseConditionKeepsSubtypeUnloaded() throws Exception {
    ExtensionRegistry registry = new ExtensionRegistry();
    try (TestConditionalScan.ChildFirstLoader loader = start(registry)) {
      assertFalse(loader.isLoaded(PACKAGE + ".off.HiddenReward"));
      assertTrue(registry.subtypes().stream().noneMatch(subtype -> subtype.name().equals("hidden")));
    }
  }

  @Test
  public void laterSubscriberGetsEarlierSubtypes() throws Exception {
    ExtensionRegistry registry = new ExtensionRegistry();
    try (TestConditionalScan.ChildFirstLoader loader = start(registry)) {
      List<String> seen = new ArrayList<>();
      registry.onSubtype(subtype -> seen.add(subtype.name()));

      assertEquals(3, seen.size());
    }
  }

  TestConditionalScan.ChildFirstLoader start(ExtensionRegistry registry) throws Exception {
    File jar = temp.resolve(PACKAGE + ".jar").toFile();
    Path root = CLASSES.resolve(PACKAGE.replace('.', '/'));

    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()));
         Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        out.putNextEntry(new JarEntry(CLASSES.relativize(file).toString().replace('\\', '/')));
        Files.copy(file, out);
        out.closeEntry();
      }
    }

    TestConditionalScan.ChildFirstLoader loader = new TestConditionalScan.ChildFirstLoader(jar, PACKAGE + ".");
    DiApplication application = new DiApplication(new TestLogger(), jar, PACKAGE, loader);
    application.setName("subtypes");
    application.setExtensionRegistry(registry);
    application.setConditionRegistry(new ConditionRegistry());
    application.getConditionParser().register(new Condition() {
      @Override
      public String name() {
        return "flag";
      }

      @Override
      public boolean process(String value) {
        return value.equals("on");
      }
    });
    application.start();
    return loader;
  }
}
