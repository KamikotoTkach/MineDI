package ru.cwcode.tkach.minedi.extension.paper.annotation;

import org.bukkit.plugin.ServicePriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BukkitService {
  ServicePriority value() default ServicePriority.Normal;
}
