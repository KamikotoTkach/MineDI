package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * The built-in extractors and the choice of one for an event type.
 */
public final class PlayerExtractors {
  public static final String DAMAGER = "damager";

  static final List<PlayerExtractor<?>> BUILTIN = List.of(
    of(PlayerEvent.class, "", PlayerEvent::getPlayer),
    of(EntityEvent.class, "", event -> asPlayer(event.getEntity())),
    of(InventoryInteractEvent.class, "", event -> asPlayer(event.getWhoClicked())),
    of(InventoryOpenEvent.class, "", event -> asPlayer(event.getPlayer())),
    of(InventoryCloseEvent.class, "", event -> asPlayer(event.getPlayer())),
    of(EntityDamageByEntityEvent.class, DAMAGER, event -> attacker(event.getDamager()))
  );

  private PlayerExtractors() {
  }

  /**
   * The extractor of the most specific event type that {@code eventType} extends, among those with the name.
   *
   * @return null when there is none
   */
  @SuppressWarnings("unchecked")
  public static <E extends Event> @Nullable PlayerExtractor<E> find(Class<E> eventType, String name, Collection<? extends PlayerExtractor<?>> custom) {
    List<PlayerExtractor<?>> all = new ArrayList<>(custom);
    all.addAll(BUILTIN);

    PlayerExtractor<?> best = null;
    for (PlayerExtractor<?> candidate : all) {
      if (!candidate.name().equals(name) || !candidate.eventType().isAssignableFrom(eventType)) continue;
      if (best == null || best.eventType().isAssignableFrom(candidate.eventType()) && best.eventType() != candidate.eventType()) {
        best = candidate;
      }
    }
    return (PlayerExtractor<E>) best;
  }

  public static @Nullable Player attacker(Entity damager) {
    if (damager instanceof Player player) return player;
    if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) return shooter;
    return null;
  }

  private static @Nullable Player asPlayer(@Nullable Entity entity) {
    return entity instanceof Player player ? player : null;
  }

  private static @Nullable Player asPlayer(@Nullable HumanEntity entity) {
    return entity instanceof Player player ? player : null;
  }

  private static <E extends Event> PlayerExtractor<E> of(Class<E> type, String name, Function<E, @Nullable Player> extract) {
    return new PlayerExtractor<>() {
      @Override
      public Class<E> eventType() {
        return type;
      }

      @Override
      public String name() {
        return name;
      }

      @Override
      public @Nullable Player extract(E event) {
        return extract.apply(event);
      }
    };
  }
}
