package ru.cwcode.minediprobe.paper;

/**
 * Not a bean: MineDI must still fill its static field.
 */
public class StaticHolder {
  private static GreetingService greetingService;

  public static GreetingService greetingService() {
    return greetingService;
  }
}
