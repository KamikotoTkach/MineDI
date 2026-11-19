package ru.cwcode.tkach.minedi.starter;

/**
 * What a {@link ru.cwcode.tkach.minedi.annotation.Starter} adds to a consumer application. One instance per JVM;
 * {@link #apply} runs once for every consumer, before its conditions and beans.
 */
public interface StarterExtension {
  void apply(StarterContext context);
}
