package ru.cwcode.tkach.minedi.extension.paper.processor;

import org.bukkit.entity.Player;
import revxrsal.asm.BoundMethodCaller;
import revxrsal.asm.MethodCaller;
import ru.cwcode.tkach.ipmc.Packet;
import ru.cwcode.tkach.ipmc.paper.IPMC;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.ipmc.PacketListener;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class PacketListenerAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final PaperExtension extension;
  
  public PacketListenerAnnotationProcessor(PaperExtension extension) {
    super(CustomMethodAnnotationEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(PacketListener.class)) return;
    
    Method method = event.getMethod();
    method.setAccessible(true);
    
    Class<?> first = method.getParameterTypes()[0];
    Class<?> second = method.getParameterTypes().length == 2 ? method.getParameterTypes()[1] : null;
    
    int playerIndex = Player.class.isAssignableFrom(first) ? 0 : Player.class.isAssignableFrom(second) ? 1 : -1;
    int packetIndex = Packet.class.isAssignableFrom(first) ? 0 : Packet.class.isAssignableFrom(second) ? 1 : -1;
    
    if (packetIndex == -1) {
      extension.getPlugin().getLogger().warning("Cannot register packet listener due to no packet class parameter");
      return;
    }
    
    BoundMethodCaller boundMethodCaller = MethodCaller.wrap(method)
                                                      .bindTo(event.getBean());
    
    IPMC.packetManager().registerIncomingPacket((Class<? extends Packet>)(packetIndex == 0?first:second), (pl, packet) -> {
      Object response;
      if (playerIndex == -1) {
        response = boundMethodCaller.call(packet);
      } else {
        response = boundMethodCaller.call(packetIndex == 0 ? packet : pl, packetIndex == 1 ? packet : pl);
      }
      
      if (response instanceof Packet responsePacket) {
        IPMC.packetManager().send(responsePacket, pl);
      }
    });
  }
}
