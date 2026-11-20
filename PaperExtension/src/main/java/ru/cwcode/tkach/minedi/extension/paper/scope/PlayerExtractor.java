package ru.cwcode.tkach.minedi.extension.paper.scope;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.annotation.ExtensionPoint;

/**
 * Tells whose event it is for the {@code @EventHandler} methods of player beans. A bean of any plugin implementing it
 * adds an event type; the most specific event type wins, {@link #name()} selects a role ({@link PlayerOf}).
 */
@ExtensionPoint
public interface PlayerExtractor<E extends Event> {
  Class<E> eventType();

  /**
   * Empty for the default player of the event.
   */
  default String name() {
    return "";
  }

  /**
   * @return null when the event has no such player (a mob was hit)
   */
  @Nullable Player extract(E event);
}
