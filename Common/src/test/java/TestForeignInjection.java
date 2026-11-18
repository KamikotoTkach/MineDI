import isolated.foreign.consumer.ConsumerCtorService;
import isolated.foreign.consumer.ConsumerRegisteredObject;
import isolated.foreign.consumer.ConsumerService;
import isolated.foreign.consumer.ConsumerStaticHolder;
import isolated.foreign.owner.*;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.data.ForeignOwner;
import ru.cwcode.tkach.minedi.exception.ForeignBeanUnavailableException;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.logging.LogConsumer;
import ru.cwcode.tkach.minedi.logging.LogLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestForeignInjection {
  static final String CLASSES = "target/test-classes/";

  DiApplication owner() {
    DiApplication owner = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, "isolated.foreign.owner."));
    owner.setName("owner");
    owner.excludeClassFromAutoScan(OwnerExcludedService.class.getName());
    return owner;
  }

  DiApplication consumer(DiApplication owner) {
    DiApplication consumer = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, "isolated.foreign.consumer."));
    consumer.setName("consumer");

    consumer.setForeignResolver(type -> type.getName().startsWith("isolated.foreign.owner.")
                                        ? Optional.of(new ForeignOwner("owner", owner))
                                        : Optional.empty());
    owner.setForeignResolver(consumer.getForeignResolver());
    return consumer;
  }

  @Test
  public void injectsForeignBeansEverywhere() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.start();

    OwnerService ownerService = owner.get(OwnerService.class).orElseThrow();
    OwnerSettings settings = owner.get(OwnerSettings.class).orElseThrow();
    ConsumerService service = consumer.get(ConsumerService.class).orElseThrow();

    assertSame(ownerService, service.getOwnerService());
    assertSame(settings, service.getSettings());
    assertSame(ownerService, consumer.get(ConsumerCtorService.class).orElseThrow().getOwnerService());
    assertSame(settings, ConsumerStaticHolder.getSettings());
    assertSame(ownerService, consumer.get(OwnerService.class).orElseThrow());
  }

  @Test
  public void foreignInterfaceResolvesToOwnersOnlyImplementation() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.start();

    assertSame(owner.get(OwnerApiImpl.class).orElseThrow(), consumer.get(ConsumerService.class).orElseThrow().getApi());
  }

  @Test
  public void foreignNonComponentStaysNull() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.start();

    assertNull(consumer.get(ConsumerService.class).orElseThrow().getDto());
    assertTrue(consumer.get(OwnerDto.class).isEmpty());
  }

  @Test
  public void missingForeignComponentIsReported() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.start();

    assertNull(consumer.get(ConsumerService.class).orElseThrow().getExcluded());

    ForeignBeanUnavailableException error = assertThrows(ForeignBeanUnavailableException.class,
                                                         () -> consumer.get(OwnerExcludedService.class));
    assertTrue(error.getMessage().contains("plugin owner has no bean"), error.getMessage());
  }

  @Test
  public void ownerThatHasNotStartedFailsConstructorInjection() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);

    ForeignBeanUnavailableException error = assertThrows(ForeignBeanUnavailableException.class, consumer::start);
    assertTrue(error.getMessage().contains("has not started yet"), error.getMessage());
    assertTrue(error.getMessage().contains(ConsumerCtorService.class.getName()), error.getMessage());
  }

  @Test
  public void ownerThatHasNotStartedOnlyWarnsForFields() {
    DiApplication owner = owner();
    List<LogLevel> levels = new ArrayList<>();
    Log log = new Log() {{
      consumers.add(new LogConsumer() {
        @Override
        public boolean isEnabled(LogLevel level) {
          return true;
        }

        @Override
        public void consume(String message, LogLevel level) {
          if (message.contains("has not started yet")) levels.add(level);
        }
      });
    }};
    DiApplication consumer = new DiApplication(log, new TestClassScanner(CLASSES, "isolated.foreign.consumer."));
    consumer.setName("consumer");
    consumer.setForeignResolver(type -> type.getName().startsWith("isolated.foreign.owner.")
                                        ? Optional.of(new ForeignOwner("owner", owner))
                                        : Optional.empty());
    consumer.excludeClassFromAutoScan(ConsumerCtorService.class.getName());

    assertDoesNotThrow(consumer::start);
    assertNull(consumer.get(ConsumerService.class).orElseThrow().getOwnerService());
    assertFalse(levels.isEmpty());
    assertTrue(levels.stream().allMatch(LogLevel.WARNING::equals), levels.toString());
  }

  @Test
  public void ownerReloadReachesImporter() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.start();

    OwnerSettings reloaded = new OwnerSettings();
    owner.getContainer().recreate(OwnerSettings.class, reloaded);

    assertSame(reloaded, consumer.get(ConsumerService.class).orElseThrow().getSettings());
    assertSame(reloaded, ConsumerStaticHolder.getSettings());
    assertSame(reloaded, consumer.get(OwnerSettings.class).orElseThrow());
  }

  @Test
  public void manualReferenceIsLinkedForReload() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();
    consumer.registerReference(owner.get(OwnerSettings.class).orElseThrow(), OwnerSettings.class);
    consumer.start();

    OwnerSettings reloaded = new OwnerSettings();
    owner.getContainer().recreate(OwnerSettings.class, reloaded);

    assertSame(reloaded, consumer.get(ConsumerService.class).orElseThrow().getSettings());
  }

  @Test
  public void registeredObjectImportsWhileSingletonsArePopulated() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();

    ConsumerRegisteredObject registered = new ConsumerRegisteredObject();
    consumer.register(registered);
    consumer.start();

    assertSame(owner.get(OwnerAuditService.class).orElseThrow(), registered.getAuditService());
  }

  @Test
  public void manualReferenceWinsOverImport() {
    DiApplication owner = owner();
    DiApplication consumer = consumer(owner);
    owner.start();

    OwnerService manual = new OwnerService();
    consumer.registerReference(manual, OwnerService.class);
    consumer.start();

    assertSame(manual, consumer.get(ConsumerService.class).orElseThrow().getOwnerService());
    assertSame(manual, consumer.get(ConsumerCtorService.class).orElseThrow().getOwnerService());
  }
}
