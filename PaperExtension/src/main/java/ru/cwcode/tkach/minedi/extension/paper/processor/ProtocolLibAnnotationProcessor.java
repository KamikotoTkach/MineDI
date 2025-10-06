package ru.cwcode.tkach.minedi.extension.paper.processor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.entity.Player;
import revxrsal.asm.BoundMethodCaller;
import revxrsal.asm.MethodCaller;
import ru.cwcode.tkach.ipmc.Packet;
import ru.cwcode.tkach.ipmc.paper.IPMC;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.extension.paper.annotation.ProtocolLibReceiving;
import ru.cwcode.tkach.minedi.extension.paper.annotation.ProtocolLibSending;
import ru.cwcode.tkach.minedi.processing.event.CustomMethodAnnotationEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;
import ru.cwcode.tkach.minedi.utils.CollectionUtils;
import ru.cwcode.tkach.minedi.utils.Utils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProtocolLibAnnotationProcessor extends EventProcessor<CustomMethodAnnotationEvent> {
  private final PaperExtension extension;
  
  public ProtocolLibAnnotationProcessor(PaperExtension extension) {
    super(CustomMethodAnnotationEvent.class);
    this.extension = extension;
  }
  
  @Override
  public void process(CustomMethodAnnotationEvent event, DiApplication application) {
    if (!event.getAnnotation().annotationType().equals(ProtocolLibSending.class)
        && !event.getAnnotation().annotationType().equals(ProtocolLibReceiving.class)) return;
    
    Method method = event.getMethod();
    method.setAccessible(true);
    
    Class<?> arg;
    Class<?>[] args = method.getParameterTypes();
    if (args.length != 1 || !PacketEvent.class.isAssignableFrom(arg = args[0])) {
      application.getLogger().warn("Cannot register ProtocolLib packet listener due to no PacketEvent parameter");
      return;
    }
    
    BoundMethodCaller boundMethodCaller = MethodCaller.wrap(method)
                                                      .bindTo(event.getBean());
    
    ProtocolLibReceiving receiving = event.getMethod().getAnnotation(ProtocolLibReceiving.class);
    if (receiving != null) {
      register(application, Arrays.asList(receiving.packetTypes()), receiving.priority(), receiving.options(), boundMethodCaller, true);
    }
    
    ProtocolLibSending sending = event.getMethod().getAnnotation(ProtocolLibSending.class);
    if (sending != null) {
      register(application, Arrays.asList(sending.packetTypes()), sending.priority(), sending.options(), boundMethodCaller, false);
    }
  }
  
  private void register(DiApplication application, List<String> packets, ListenerPriority priority, ListenerOptions[] options, BoundMethodCaller method, boolean receiving) {
    List<PacketType> validPackets = packets.stream().map(x -> new Pair<>(x, PacketType.fromName(x)))
                                   .filter(packetTypes -> {
                                     if (packetTypes.getSecond().isEmpty()) {
                                       application.getLogger().warn("Cannot register {} ProtocolLib packet: not found", packetTypes.getFirst());
                                       return false;
                                     } else if (packetTypes.getSecond().size() > 1) {
                                       application.getLogger().warn("Cannot register {} ProtocolLib packet: found more than 1 packets: {}", packetTypes.getFirst(), CollectionUtils.toString(packetTypes.getSecond(), "", ", ", true));
                                       return false;
                                     }
                                     return true;
                                   })
                                   .flatMap(x -> x.getSecond().stream())
                                   .toList();
    
    
    ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(extension.getPlugin(), priority, validPackets, options) {
      @Override
      public void onPacketReceiving(PacketEvent event) {
        if (receiving) method.call(event);
        else throw new IllegalStateException("Override onPacketReceiving to get notifcations of received packets!");
      }
      
      @Override
      public void onPacketSending(PacketEvent event) {
        if (!receiving) method.call(event);
        else throw new IllegalStateException("Override onPacketSending to get notifcations of sent packets!");
      }
    });
    
    application.getLogger().info("Registered ProtocolLib listener for {} {}, priority: {}, options:", CollectionUtils.toString(validPackets, "", ", ", true),
                                 receiving ? "receiving" : "sending",
                                 priority,
                                 CollectionUtils.toString(Arrays.asList(options), "", ", ", true)
    );
  }
}