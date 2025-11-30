package ru.cwcode.tkach.minedi.extension.paper.condition;

import org.bukkit.Bukkit;
import ru.cwcode.cwutils.server.ServerUtils;
import ru.cwcode.tkach.minedi.condition.Condition;

import java.util.Objects;
import java.util.function.BiFunction;

public class VersionCondition implements Condition {
  @Override
  public String name() {
    return "version";
  }
  
  @Override
  public boolean process(String value) {
    value = value.trim();
    
    for (Comparison comparison : Comparison.values()) {
      if (value.startsWith(comparison.prefix)) {
        String version = value.substring(comparison.prefix.length()).trim();
        int versionWeight = ServerUtils.getVersionWeight(version);
        int bukkitVersionWeight = ServerUtils.getVersionWeight(Bukkit.getBukkitVersion());
        
        return comparison.compare(bukkitVersionWeight, versionWeight);
      }
    }
    
    throw new IllegalArgumentException("Cannot process VersionCondition");
  }
  
  private enum Comparison {
    gte(">=", (one, two) -> one >= two),
    lte("<=", (one, two) -> one <= two),
    gt(">", (one, two) -> one > two),
    lt("<", (one, two) -> one < two),
    eq("==", Objects::equals);
    
    private final String prefix;
    private final BiFunction<Integer, Integer, Boolean> comparator;
    
    Comparison(String prefix, BiFunction<Integer, Integer, Boolean> comparator) {
      this.prefix = prefix;
      this.comparator = comparator;
    }
    
    public boolean compare(int one, int two) {
      return comparator.apply(one, two);
    }
  }
}
