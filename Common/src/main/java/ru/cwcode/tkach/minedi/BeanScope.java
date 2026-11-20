package ru.cwcode.tkach.minedi;

public final class BeanScope {
  public static final String SINGLETON = "singleton";
  public static final String PROTOTYPE = "prototype";
  /**
   * One instance per online player, see {@link ru.cwcode.tkach.minedi.annotation.PlayerScoped}.
   */
  public static final String PLAYER = "player";
}
