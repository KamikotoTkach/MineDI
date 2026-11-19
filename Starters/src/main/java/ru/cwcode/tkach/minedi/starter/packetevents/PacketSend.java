package ru.cwcode.tkach.minedi.starter.packetevents;

import com.github.retrooper.packetevents.event.PacketListenerPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A bean method called for packets the server sends; the counterpart of {@link PacketReceive} with
 * {@code PacketSendEvent} and server-side packet types.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketSend {
  String[] value() default {};

  PacketListenerPriority priority() default PacketListenerPriority.NORMAL;
}
