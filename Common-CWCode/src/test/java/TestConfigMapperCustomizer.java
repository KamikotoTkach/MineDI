import customizer.bad.ConfigDependentCustomizer;
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

import java.nio.file.Path;
import java.time.Duration;
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
    DiApplication application = new DiApplication(new Log(), () -> Set.of(classes));
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
