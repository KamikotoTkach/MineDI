package ru.cwcode.tkach.minedi.extension.paper.processor.proxy;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.DiApplication;

import java.util.concurrent.Callable;

/**
 * Target of an {@code @Async} method in a generated bean subclass. Public, because the subclass lives in the
 * consumer plugin's class loader and package.
 */
public class AsyncMethodInterceptor {
  private final DiApplication application;

  public AsyncMethodInterceptor(DiApplication application) {
    this.application = application;
  }

  @RuntimeType
  public Object intercept(@SuperCall Callable<?> original) throws Exception {
    if (!Bukkit.isPrimaryThread()) {
      original.call();
      return null;
    }

    JavaPlugin plugin = application.get(JavaPlugin.class).orElseThrow();
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      try {
        original.call();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    return null;
  }
}
