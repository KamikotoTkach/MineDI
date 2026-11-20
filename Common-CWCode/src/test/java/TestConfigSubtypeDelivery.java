import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigCreator;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigMapperExtension;
import ru.cwcode.tkach.minedi.extensionpoint.ConfigSubtype;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;
import ru.cwcode.tkach.minedi.logging.Log;
import subtype.Circle;
import subtype.Shape;
import subtype.Square;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestConfigSubtypeDelivery {
  @TempDir
  Path temp;

  @Test
  public void subtypesBeforeAndAfterRegistrationReachTheMapper() throws Exception {
    ExtensionRegistry registry = new ExtensionRegistry();
    registry.addSubtype(new ConfigSubtype(Shape.class, Circle.class, "circle", "early"));

    DiApplication application = new DiApplication(new Log(), Set::of);
    application.setName("reader");
    application.setExtensionRegistry(registry);
    YmlConfigManager manager = new YmlConfigManager(new TestConfigMapperCustomizer.TestPlatform(temp), new ConfigCreator(application));
    application.registerExtension(new ConfigMapperExtension(manager));

    var mapper = manager.jacksonMapper().getMapper();
    Shape circle = mapper.readValue("{\"type\": \"circle\", \"radius\": 3}", Shape.class);
    assertEquals(3, ((Circle) circle).radius);

    registry.addSubtype(new ConfigSubtype(Shape.class, Square.class, "square", "late"));
    assertInstanceOf(Square.class, manager.jacksonMapper().getMapper().readValue("{\"type\": \"square\"}", Shape.class), "a subtype published later reaches a mapper that already read");
  }
}
