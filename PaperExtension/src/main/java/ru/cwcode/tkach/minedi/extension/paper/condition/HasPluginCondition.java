package ru.cwcode.tkach.minedi.extension.paper.condition;

import org.bukkit.Bukkit;
import ru.cwcode.tkach.minedi.condition.Condition;

public class HasPluginCondition implements Condition {
  @Override
  public String name() {
    return "hasPlugin";
  }
  
  @Override
  public boolean process(String value) {
    return Bukkit.getPluginManager().getPlugin(value) != null;
  }
}
