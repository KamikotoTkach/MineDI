package ru.cwcode.tkach.minedi.extension.velocity.processor;

import com.velocitypowered.api.proxy.ServerConnection;
import revxrsal.asm.BoundMethodCaller;
import revxrsal.asm.MethodCaller;
import ru.cwcode.tkach.ipmc.Packet;
import ru.cwcode.tkach.ipmc.velocity.IPMC;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.common.cwcode.ipmc.PacketListener;
import ru.cwcode.tkach.minedi.extension.velocity.VelocityExtension;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Method;

public class PacketListenerAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final VelocityExtension extension;
  
  public PacketListenerAnnotationProcessor(VelocityExtension extension) {
    super(CustomMethodAnnotationEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(PacketListener.class)) return;
    
    Method method = event.getMethod();
    method.setAccessible(true);
    
    Class<?> first = method.getParameterTypes()[0];
    Class<?> second = method.getParameterTypes().length == 2 ? method.getParameterTypes()[1] : Class.class; // Class.class here to avoid NPE
    
    int connectionIndex = ServerConnection.class.isAssignableFrom(first) ? 0 : ServerConnection.class.isAssignableFrom(second) ? 1 : -1;
    int packetIndex = Packet.class.isAssignableFrom(first) ? 0 : Packet.class.isAssignableFrom(second) ? 1 : -1;
    
    if (packetIndex == -1) {
      extension.getPlugin().getLogger().warn("Cannot register packet listener due to no packet class parameter");
      return;
    }
    
    BoundMethodCaller boundMethodCaller = MethodCaller.wrap(method)
                                                      .bindTo(event.getBean());
    
    IPMC.packetManager().registerIncomingPacket((Class<? extends Packet>) (packetIndex == 0 ? first : second), (connection, packet) -> {
      Object response;
      if (connectionIndex == -1) {
        response = boundMethodCaller.call(packet);
      } else {
        response = boundMethodCaller.call(packetIndex == 0 ? packet : connection, packetIndex == 1 ? packet : connection);
      }
      
      if (response instanceof Packet responsePacket) {
        if (IPMC.packetManager().isAwaitingResponse(packet)) {
          IPMC.packetManager().sendResponse(packet, responsePacket);
        } else {
          IPMC.packetManager().send(responsePacket, connection);
        }
      }
    });
  }
}
