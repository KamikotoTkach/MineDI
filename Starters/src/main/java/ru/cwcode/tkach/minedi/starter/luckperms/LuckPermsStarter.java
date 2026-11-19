package ru.cwcode.tkach.minedi.starter.luckperms;

import org.bukkit.plugin.Plugin;
import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;

@Starter(plugin = "LuckPerms")
public class LuckPermsStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    context.processor(new PermissionsRecalculatedProcessor((Plugin) context.getPlatformPlugin()));
  }
}
