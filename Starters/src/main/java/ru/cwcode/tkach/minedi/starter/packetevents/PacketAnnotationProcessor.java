package ru.cwcode.tkach.minedi.starter.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registers one PacketEvents listener per bean and priority for its {@link PacketReceive}/{@link PacketSend} methods.
 */
public class PacketAnnotationProcessor extends EventProcessor<BeanConstructedEvent> {
  public PacketAnnotationProcessor() {
    super(BeanConstructedEvent.class);
  }

  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    Object bean = event.getBean();
    Map<PacketListenerPriority, BeanPacketListener> listeners = new EnumMap<>(PacketListenerPriority.class);

    for (Class<?> clazz = bean.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        PacketReceive receive = method.getAnnotation(PacketReceive.class);
        if (receive != null) {
          listener(listeners, receive.priority(), application)
            .receivers().add(PacketHandler.of(application, bean, method, receive.value(), true));
        }

        PacketSend send = method.getAnnotation(PacketSend.class);
        if (send != null) {
          listener(listeners, send.priority(), application)
            .senders().add(PacketHandler.of(application, bean, method, send.value(), false));
        }
      }
    }

    listeners.forEach((priority, listener) -> {
      PacketEvents.getAPI().getEventManager().registerListener(listener, priority);
      application.getLogger().debug("Registered PacketEvents listener of {} with priority {}", bean.getClass().getName(), priority);
    });
  }

  private static BeanPacketListener listener(Map<PacketListenerPriority, BeanPacketListener> listeners,
                                             PacketListenerPriority priority,
                                             DiApplication application) {
    return listeners.computeIfAbsent(priority, key -> new BeanPacketListener(application));
  }
}
