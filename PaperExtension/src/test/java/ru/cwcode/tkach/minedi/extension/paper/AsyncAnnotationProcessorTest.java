package ru.cwcode.tkach.minedi.extension.paper;

import org.bukkit.event.Listener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Async;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Sync;
import ru.cwcode.tkach.minedi.extension.paper.processor.AsyncAnnotationProcessor;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;

class AsyncAnnotationProcessorTest {
  private final AsyncAnnotationProcessor processor = new AsyncAnnotationProcessor();
  
  @Test
  void shouldRejectAsyncListenerService() {
    assertListenerProxyRejected(new AsyncListenerService());
  }
  
  @Test
  void shouldRejectSyncListenerService() {
    assertListenerProxyRejected(new SyncListenerService());
  }
  
  private void assertListenerProxyRejected(Object bean) {
    IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
      processor.process(new BeanCreatedEvent(bean), null)
    );
    
    Assertions.assertTrue(exception.getMessage().contains("Cannot proxy"));
    Assertions.assertTrue(exception.getMessage().contains("listener registration scans declared methods"));
  }
  
  @Service
  static class AsyncListenerService implements Listener {
    @Async
    public void runAsync() {
    }
  }
  
  @Service
  static class SyncListenerService implements Listener {
    @Sync
    public void runSync() {
    }
  }
}
