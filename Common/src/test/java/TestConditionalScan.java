import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.condition.Condition;
import ru.cwcode.tkach.minedi.condition.ConditionRegistry;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A real jar and a child-first loader: a class whose condition is false must never reach the loader.
 */
public class TestConditionalScan {
  static final Path CLASSES = Path.of("target/test-classes");

  @TempDir
  Path temp;

  @Test
  public void falseConditionsKeepClassesUnloaded() throws Exception {
    try (Plugin plugin = plugin("isolated.conditional")) {
      plugin.application.start();

      assertFalse(plugin.loader.isLoaded("isolated.conditional.OffFlag"));
      assertFalse(plugin.loader.isLoaded("isolated.conditional.OffFlag$Helper"));
      assertFalse(plugin.loader.isLoaded("isolated.conditional.off.sub.Hidden"));
      assertFalse(plugin.loader.isLoaded("isolated.conditional.PlainIntegration"));

      assertTrue(plugin.isBean("isolated.conditional.Always"));
      assertTrue(plugin.isBean("isolated.conditional.OnFlag"));
      assertTrue(plugin.isBean("isolated.conditional.UsesOwn"));
      assertTrue(plugin.isBean("isolated.conditional.ByProperty"));
    }
  }

  @Test
  public void conditionOverridesIntegrationSuffixAndStaticsAreInjected() throws Exception {
    try (Plugin plugin = plugin("isolated.conditional")) {
      plugin.application.start();

      assertTrue(plugin.isBean("isolated.conditional.on.ShownIntegration"));

      Class<?> always = plugin.loader.loadClass("isolated.conditional.Always");
      Object injected = plugin.loader.loadClass("isolated.conditional.on.ShownIntegration").getField("always").get(null);
      assertSame(plugin.application.get(always).orElseThrow(), injected);
    }
  }

  @Test
  public void conditionDefinitionUnderConditionFailsTheScan() throws Exception {
    try (Plugin plugin = plugin("isolated.conditionalbad")) {
      BeanDefinitionException error = assertThrows(BeanDefinitionException.class, plugin.application::start);
      assertTrue(error.getMessage().contains("GuardedConditions"), error.getMessage());
      assertTrue(error.getMessage().contains("cannot depend on a condition"), error.getMessage());
    }
  }

  @Test
  public void unknownConditionNamesTheClass() throws Exception {
    try (Plugin plugin = plugin("isolated.conditionalunknown")) {
      BeanDefinitionException error = assertThrows(BeanDefinitionException.class, plugin.application::start);
      assertTrue(error.getMessage().contains("unknown condition nope"), error.getMessage());
      assertTrue(error.getMessage().contains("isolated.conditionalunknown.UnknownCondition"), error.getMessage());
      assertTrue(error.getMessage().contains("flag (built-in)"), error.getMessage());
    }
  }

  Plugin plugin(String packageName) throws IOException {
    File jar = temp.resolve(packageName + ".jar").toFile();
    Path root = CLASSES.resolve(packageName.replace('.', '/'));

    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()));
         Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        out.putNextEntry(new JarEntry(CLASSES.relativize(file).toString().replace('\\', '/')));
        Files.copy(file, out);
        out.closeEntry();
      }
    }

    ChildFirstLoader loader = new ChildFirstLoader(jar, packageName + ".");
    DiApplication application = new DiApplication(new TestLogger(), jar, packageName, loader);
    application.setName("conditional");
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
    return new Plugin(application, loader);
  }

  record Plugin(DiApplication application, ChildFirstLoader loader) implements AutoCloseable {
    boolean isBean(String name) throws ClassNotFoundException {
      return application.getContainer().isBean(loader.loadClass(name));
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }

  static final class ChildFirstLoader extends URLClassLoader {
    final String prefix;

    ChildFirstLoader(File jar, String prefix) throws IOException {
      super(new URL[]{jar.toURI().toURL()}, TestConditionalScan.class.getClassLoader());
      this.prefix = prefix;
    }

    boolean isLoaded(String name) {
      return findLoadedClass(name) != null;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!name.startsWith(prefix)) return super.loadClass(name, resolve);

      synchronized (getClassLoadingLock(name)) {
        Class<?> loaded = findLoadedClass(name);
        return loaded != null ? loaded : findClass(name);
      }
    }
  }
}
