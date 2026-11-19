package ru.cwcode.tkach.minedi.scanner;

import java.io.File;
import java.util.function.Supplier;

/**
 * A MineDI plugin's jar as other plugins see it: its {@link ClassIndex} is read once, on the first lookup, and shared by
 * the condition and starter registries.
 */
public final class PluginJar {
  private final Supplier<String> owner;
  private final Supplier<File> jar;
  private final String packageName;
  private final ClassLoader loader;
  private volatile ClassIndex index;

  /**
   * Suppliers are read on the first lookup: on Velocity the plugin id is not known while the plugin is constructed.
   */
  public PluginJar(Supplier<String> owner, Supplier<File> jar, String packageName, ClassLoader loader) {
    this.owner = owner;
    this.jar = jar;
    this.packageName = packageName;
    this.loader = loader;
  }

  public String owner() {
    return owner.get();
  }

  public ClassLoader loader() {
    return loader;
  }

  public ClassIndex index() {
    ClassIndex current = index;
    if (current == null) {
      synchronized (this) {
        current = index;
        if (current == null) index = current = ClassIndex.ofJar(jar.get(), packageName);
      }
    }
    return current;
  }

  @Override
  public String toString() {
    return "plugin " + owner.get();
  }
}
