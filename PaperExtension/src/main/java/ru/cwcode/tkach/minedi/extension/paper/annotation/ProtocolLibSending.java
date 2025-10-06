package ru.cwcode.tkach.minedi.extension.paper.annotation;

import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtocolLibSending {
  String[] packetTypes();
  ListenerPriority priority() default ListenerPriority.NORMAL;
  ListenerOptions[] options() default {};
}
