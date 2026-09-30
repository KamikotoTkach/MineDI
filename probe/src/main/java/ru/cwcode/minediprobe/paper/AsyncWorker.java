package ru.cwcode.minediprobe.paper;

import org.bukkit.Bukkit;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Async;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Sync;

/**
 * {@code @Async}/{@code @Sync} make MineDI replace the bean with a generated subclass.
 */
@Service
public class AsyncWorker {
  volatile Boolean ranOnMainThread;
  volatile Boolean syncResultFromMainThread;

  @Async
  public void work() {
    ranOnMainThread = Bukkit.isPrimaryThread();
    syncResultFromMainThread = isMainThread();
  }

  @Sync
  public Boolean isMainThread() {
    return Bukkit.isPrimaryThread();
  }
}
