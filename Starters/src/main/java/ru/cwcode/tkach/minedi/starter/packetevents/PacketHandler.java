package ru.cwcode.tkach.minedi.starter.packetevents;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * One annotated method: which packets it takes and how its argument is made from the event.
 */
final class PacketHandler {
  private final Set<String> packets;
  private final Object bean;
  private final Method method;
  private final @Nullable Constructor<?> wrapper;

  private PacketHandler(Set<String> packets, Object bean, Method method, @Nullable Constructor<?> wrapper) {
    this.packets = packets;
    this.bean = bean;
    this.method = method;
    this.wrapper = wrapper;
  }

  /**
   * @param names packet type names from the annotation; empty means every packet, or those of the wrapper parameter
   */
  static PacketHandler of(DiApplication application, Object bean, Method method, String[] names, boolean receive) {
    String where = "%s: %s#%s".formatted(application.getName(), method.getDeclaringClass().getName(), method.getName());
    Class<?> eventType = receive ? PacketReceiveEvent.class : PacketSendEvent.class;
    Class<?>[] parameters = method.getParameterTypes();
    if (parameters.length != 1) {
      throw new BeanDefinitionException(where + " takes one parameter: " + eventType.getSimpleName() + " or a packet wrapper");
    }

    Class<?> parameter = parameters[0];
    Constructor<?> wrapper = null;
    Set<String> packets = new LinkedHashSet<>(Arrays.asList(names));

    if (!parameter.isAssignableFrom(eventType)) {
      if (!PacketWrapper.class.isAssignableFrom(parameter)) {
        throw new BeanDefinitionException(where + ": parameter " + parameter.getName() + " is neither "
                                          + eventType.getSimpleName() + " nor a packet wrapper");
      }
      try {
        wrapper = parameter.getConstructor(eventType);
      } catch (NoSuchMethodException e) {
        throw new BeanDefinitionException(where + ": wrapper " + parameter.getSimpleName() + " cannot be built from "
                                          + eventType.getSimpleName() + ", it belongs to the other direction");
      }
      if (packets.isEmpty()) packets.addAll(names(receive, parameter));
      if (packets.isEmpty()) {
        throw new BeanDefinitionException(where + ": no packet type uses " + parameter.getSimpleName() + ", list the types");
      }
    }

    Set<String> unknown = new TreeSet<>(packets);
    unknown.removeAll(names(receive, null));
    if (!unknown.isEmpty()) {
      throw new BeanDefinitionException(where + ": unknown " + (receive ? "client" : "server") + " packet types " + unknown);
    }

    method.setAccessible(true);
    return new PacketHandler(Set.copyOf(packets), bean, method, wrapper);
  }

  boolean accepts(@Nullable PacketTypeCommon type) {
    return type != null && (packets.isEmpty() || packets.contains(type.getName()));
  }

  void call(ProtocolPacketEvent event) throws Throwable {
    try {
      method.invoke(bean, wrapper == null ? event : wrapper.newInstance(event));
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /**
   * Names of the packet types of one side; only those read by {@code wrapperClass} when it is given.
   */
  private static Set<String> names(boolean receive, @Nullable Class<?> wrapperClass) {
    Set<String> names = new LinkedHashSet<>();
    for (PacketTypeCommon type : types(receive)) {
      if (wrapperClass == null || type.getWrapperClass() == wrapperClass) names.add(type.getName());
    }
    return names;
  }

  private static List<PacketTypeCommon> types(boolean receive) {
    Stream<PacketTypeCommon[]> groups = receive
                                        ? Stream.of(PacketType.Handshaking.Client.values(), PacketType.Status.Client.values(),
                                                    PacketType.Login.Client.values(), PacketType.Configuration.Client.values(),
                                                    PacketType.Play.Client.values())
                                        : Stream.of(PacketType.Handshaking.Server.values(), PacketType.Status.Server.values(),
                                                    PacketType.Login.Server.values(), PacketType.Configuration.Server.values(),
                                                    PacketType.Play.Server.values());
    return groups.flatMap(Arrays::stream).toList();
  }

  @Override
  public String toString() {
    return method.getDeclaringClass().getName() + "#" + method.getName();
  }
}
