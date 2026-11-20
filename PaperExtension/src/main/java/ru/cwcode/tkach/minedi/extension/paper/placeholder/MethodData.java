package ru.cwcode.tkach.minedi.extension.paper.placeholder;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * @param parameters the parameter types the request parameters are parsed into
 * @param call       runs the method for the requesting player with the parsed arguments
 */
public record MethodData(Class<?>[] parameters, Call call) {
  @FunctionalInterface
  public interface Call {
    @Nullable Object call(@Nullable OfflinePlayer player, Object[] arguments) throws Exception;
  }
}
