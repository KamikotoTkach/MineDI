package ru.cwcode.tkach.minedi.starter.packetevents;

import com.github.retrooper.packetevents.event.PacketListenerPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A bean method called for packets the server receives. Needs {@code packetevents} in the plugin's
 * {@code depend}/{@code softdepend}.
 * <p>
 * Parameter: {@code PacketReceiveEvent}, or a wrapper such as {@code WrapperPlayClientInteractEntity} built from the event
 * (then {@link #value()} may be left empty: the packets of that wrapper are taken).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketReceive {
  /**
   * Packet type names ({@code PacketType.Play.Client} and the other client-side enums), any connection state; empty with an
   * event parameter means every packet.
   */
  String[] value() default {};

  PacketListenerPriority priority() default PacketListenerPriority.NORMAL;
}
