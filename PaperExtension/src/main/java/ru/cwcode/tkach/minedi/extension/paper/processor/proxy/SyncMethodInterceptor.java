package ru.cwcode.tkach.minedi.extension.paper.processor.proxy;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.DiApplication;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Target of a {@code @Sync} method in a generated bean subclass. Off the main thread a {@code void} method is only
 * scheduled; a method with a result blocks the caller until the main thread has produced it.
 */
public class SyncMethodInterceptor {
  private final DiApplication application;

  public SyncMethodInterceptor(DiApplication application) {
    this.application = application;
  }

  @RuntimeType
  public Object intercept(@SuperCall Callable<?> original, @Origin Method method) throws Exception {
    if (Bukkit.isPrimaryThread()) return original.call();

    JavaPlugin plugin = application.get(JavaPlugin.class).orElseThrow();
    if (method.getReturnType() == void.class) {
      Bukkit.getScheduler().runTask(plugin, () -> {
        try {
          original.call();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
      return null;
    }

    return Bukkit.getScheduler().callSyncMethod(plugin, original).get();
  }
}
