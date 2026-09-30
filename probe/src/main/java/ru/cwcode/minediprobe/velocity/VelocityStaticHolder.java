package ru.cwcode.minediprobe.velocity;

/**
 * Not a bean: MineDI must still fill its static field.
 */
public class VelocityStaticHolder {
  private static VelocityProbeConfig config;

  public static VelocityProbeConfig config() {
    return config;
  }
}
