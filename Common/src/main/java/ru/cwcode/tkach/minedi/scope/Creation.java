package ru.cwcode.tkach.minedi.scope;

/**
 * When a {@link ru.cwcode.tkach.minedi.annotation.PlayerScoped} instance is created.
 */
public enum Creation {
  /**
   * On the first lookup, event or placeholder of the player.
   */
  LAZY,
  /**
   * When the player joins, once persistent player storage is loaded.
   */
  JOIN,
  /**
   * When the player's data is synchronised (HuskSync); the instance is unreachable before that. Without a sync source
   * on the server it is the same as {@link #JOIN}.
   */
  SYNC
}
