package ru.cwcode.tkach.minedi.starter.luckperms;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A bean method {@code (Player)} called on the main thread after LuckPerms has rebuilt the cached data of an online player
 * ({@code UserDataRecalculateEvent}). Needs {@code LuckPerms} in the plugin's {@code depend}/{@code softdepend}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermissionsRecalculated {
}
