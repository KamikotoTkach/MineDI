import customizer.bad.ConfigDependentCustomizer;
import customizer.conditional.ConditionalCustomizer;
import customizer.conditional.SkippedCustomizer;
import customizer.ok.RecordingCustomizer;
import customizer.ok.SampleConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.cwcode.tkach.config.base.ConfigPlatform;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.databind.Module;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigCreator;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigMapperExtension;
import ru.cwcode.tkach.minedi.constructor.BeanConstructor;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.scanner.ScanResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestConfigMapperCustomizer {
  @TempDir
  Path temp;

  @BeforeEach
  void reset() {
    RecordingCustomizer.applied = false;
    SampleConfig.customizedWhenCreated = false;
  }

  DiApplication application(Class<?>... classes) {
    return application(() -> Set.of(classes));
  }

  DiApplication application(ClassScanner scanner) {
    DiApplication application = new DiApplication(new Log(), scanner);
    application.setName("customizers");

    YmlConfigManager manager = new YmlConfigManager(new TestPlatform(temp), new ConfigCreator(application));
    // stands in for ConfigConstructor: reading a real file needs the server libraries
    application.getBeanConstructors().getConstructors().addFirst(new BeanConstructor() {
      @Override
      public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
        return clazz == SampleConfig.class ? clazz.cast(new SampleConfig()) : null;
      }
    });
    application.registerExtension(new ConfigMapperExtension(manager));
    return application;
  }

  @Test
  public void customizerRunsBeforeTheFirstConfig() {
    DiApplication application = application(SampleConfig.class, RecordingCustomizer.class);
    application.start();

    assertTrue(application.get(SampleConfig.class).isPresent());
    assertTrue(SampleConfig.customizedWhenCreated, "the config was read before the mapper was customized");
  }

  @Test
  public void conditionalCustomizerRunsWhenItsConditionHolds() {
    ConditionalCustomizer.applied = false;
    SkippedCustomizer.applied = false;
    List<String> loaded = new ArrayList<>();
    ClassScanner scanner = new ClassScanner() {
      @Override
      public Set<Class<?>> scan() {
        return Set.of();
      }

      @Override
      public ScanResult scanResult() {
        return new ScanResult(Set.of(SampleConfig.class),
                              List.of(new ScanResult.ConditionalClass(ConditionalCustomizer.class.getName(), List.of("hasClass(java.lang.String)")),
                                      new ScanResult.ConditionalClass(SkippedCustomizer.class.getName(), List.of("!hasClass(java.lang.String)"))),
                              List.of(),
                              name -> {
                                loaded.add(name);
                                return name.equals(ConditionalCustomizer.class.getName()) ? ConditionalCustomizer.class : SkippedCustomizer.class;
                              });
      }
    };
    DiApplication application = application(scanner);
    application.start();

    assertTrue(ConditionalCustomizer.applied);
    assertTrue(SampleConfig.customizedWhenCreated, "the conditional customizer ran after the first config");
    assertFalse(SkippedCustomizer.applied);
    assertEquals(List.of(ConditionalCustomizer.class.getName()), loaded, "a class with a false condition is never loaded");
  }

  @Test
  public void customizerDependingOnConfigFailsStartup() {
    DiApplication application = application(SampleConfig.class, ConfigDependentCustomizer.class);
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application::start);

    assertTrue(error.getMessage().contains(ConfigDependentCustomizer.class.getName()), error.getMessage());
    assertTrue(error.getMessage().contains(SampleConfig.class.getName()), error.getMessage());
  }

  record TestPlatform(Path dataFolder) implements ConfigPlatform {
    @Override
    public List<Module> additionalJacksonModules() {
      return List.of();
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void warning(String message) {
    }

    @Override
    public void runAsync(Runnable runnable) {
      runnable.run();
    }

    @Override
    public void schedule(Runnable runnable, Duration frequency, boolean async) {
    }

    @Override
    public void disable() {
    }

    @Override
    public String name() {
      return "customizers";
    }
  }
}
