import isolated.playerscope.Inventory;
import isolated.playerscope.Journal;
import isolated.playerscope.Profile;
import isolated.playerscope.TestPlayer;
import isolated.playerscope.Wallet;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;
import ru.cwcode.tkach.minedi.scope.PlayerScopeProvider;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestPlayerScope {
  static final String CLASSES = "target/test-classes/";

  final PlayerWindows windows = new PlayerWindows();
  final TestPlayer alex = TestPlayer.of("alex");
  final TestPlayer bob = TestPlayer.of("bob");

  {
    windows.setKeyType(TestPlayer.class, TestPlayer::id);
  }

  DiApplication application(String prefix) {
    DiApplication application = new DiApplication(new TestLogger(), new TestClassScanner(CLASSES, prefix));
    application.setName("scope");
    application.setExtensionRegistry(new ExtensionRegistry());
    application.setPlayerWindows(windows);
    return application;
  }

  DiApplication started() {
    DiApplication application = application("isolated.playerscope.");
    application.start();
    return application;
  }

  void join(TestPlayer player) {
    windows.open(player);
    windows.joined(player.id());
  }

  @Test
  public void lazyBeanIsCreatedOnFirstGetWithItsPlayerAndSingletons() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    windows.open(alex); // no joined(): nothing eager is created

    assertTrue(journal.getWallets().find(alex).isEmpty());

    Wallet wallet = journal.getWallets().get(alex);
    assertSame(alex, wallet.getPlayer());
    assertSame(journal, wallet.getJournal());
    assertSame(wallet, journal.getWallets().get(alex), "one instance per player");
  }

  @Test
  public void joinBeanIsCreatedOnJoinAndSharesBeansOfItsPlayer() {
    DiApplication application = started();
    PlayerScopeProvider scope = application.getContainer().getPlayerScope();
    join(alex);
    join(bob);

    Profile profile = scope.find(Profile.class, alex.id()).orElseThrow();
    assertSame(scope.get(Wallet.class, alex.id()), profile.getWallet(), "the same player's wallet");
    assertNotSame(profile.getWallet(), scope.get(Profile.class, bob.id()).getWallet());
    assertEquals(2, scope.all(Profile.class).size());
  }

  @Test
  public void closeDestroysInReverseOrderAndForgetsThePlayer() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    PlayerScopeProvider scope = application.getContainer().getPlayerScope();
    join(alex);

    windows.close(alex.id());

    assertEquals(List.of("profile alex", "wallet alex"), journal.getEntries());
    assertTrue(scope.find(Wallet.class, alex.id()).isEmpty());
    assertThrows(IllegalStateException.class, () -> scope.get(Wallet.class, alex.id()));

    join(alex);
    assertNotSame(null, scope.get(Wallet.class, alex.id()));
    assertEquals(0, scope.get(Wallet.class, alex.id()).getCoins(), "a new instance after rejoin");
  }

  @Test
  public void syncBeanWaitsForTheSyncSource() {
    windows.useSyncSource();
    DiApplication application = started();
    PlayerScopeProvider scope = application.getContainer().getPlayerScope();
    join(alex);

    assertThrows(IllegalStateException.class, () -> scope.get(Inventory.class, alex.id()));
    assertTrue(scope.find(Inventory.class, alex.id()).isEmpty());

    windows.synced(alex.id());
    assertTrue(scope.find(Inventory.class, alex.id()).isPresent(), "created on sync");
  }

  @Test
  public void syncBeanWithoutSyncSourceIsCreatedOnJoin() {
    DiApplication application = started();
    join(alex);

    assertTrue(application.getContainer().getPlayerScope().find(Inventory.class, alex.id()).isPresent());
  }

  @Test
  public void failingPreDestroyDoesNotStopTheOthers() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    join(alex);

    windows.close(alex.id()); // Inventory.save throws

    assertTrue(journal.getEntries().contains("wallet alex"));
    assertTrue(journal.getEntries().contains("profile alex"));
  }

  @Test
  public void closeAllDestroysEveryone() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    join(alex);
    join(bob);

    application.getContainer().getPlayerScope().closeAll();

    assertEquals(4, journal.getEntries().size());
  }

  @Test
  public void playerMapDropsTheEntryOnQuitAndIgnoresLateWrites() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    join(alex);

    journal.getLastSeen().put(alex, 10L);
    journal.getFlagged().add(alex);
    assertEquals(10L, journal.getLastSeen().get(alex));

    windows.close(alex.id());
    assertNull(journal.getLastSeen().get(alex));
    assertFalse(journal.getFlagged().contains(alex));

    assertNull(journal.getLastSeen().put(alex, 20L));
    assertNull(journal.getLastSeen().computeIfAbsent(alex, player -> 30L));
    assertFalse(journal.getFlagged().add(alex));
    assertTrue(journal.getLastSeen().isEmpty(), "a write after the quit must not leak");
  }

  @Test
  public void allDuringCloseIsASnapshot() {
    DiApplication application = started();
    Journal journal = application.get(Journal.class).orElseThrow();
    join(alex);
    join(bob);
    journal.getWallets().get(alex);
    journal.getWallets().get(bob);

    journal.getWallets().forEach(wallet -> windows.close(wallet.getPlayer().id()));

    assertTrue(journal.getWallets().all().isEmpty());
  }

  @Test
  public void singletonCannotHoldAPlayerBean() {
    DiApplication application = application("isolated.playerscopebad.direct.");

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application::start);
    assertTrue(error.getMessage().contains("PlayerScope<Cart>"), error.getMessage());
  }

  @Test
  public void rawScopeIsRejected() {
    DiApplication application = application("isolated.playerscopebad.raw.");

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, application::start);
    assertTrue(error.getMessage().contains("PlayerScope<"), error.getMessage());
  }
}
