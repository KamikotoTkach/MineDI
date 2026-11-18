package ru.cwcode.tkach.minedi.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Conditions defined by every MineDI plugin of the server. A plugin registers its source when it is created; the source is
 * read on the first lookup, so a plugin may use a condition of a plugin that starts after it.
 */
public class ConditionRegistry {
  public static final ConditionRegistry GLOBAL = new ConditionRegistry();
  public static final ConditionRegistry NONE = new ConditionRegistry();

  private final List<ConditionSource> sources = new CopyOnWriteArrayList<>();
  private final Map<ConditionSource, List<MethodCondition>> loaded = new LinkedHashMap<>();
  private final Map<ConditionSource, String> failed = new LinkedHashMap<>();

  public void addSource(ConditionSource source) {
    if (this == NONE) return;
    sources.add(source);
  }

  /**
   * Conditions named so, defined by any plugin except {@code excludedOwner}.
   */
  public synchronized List<MethodCondition> find(String name, String excludedOwner) {
    List<MethodCondition> result = new ArrayList<>();
    forEachLoaded(excludedOwner, condition -> {
      if (condition.name().equals(name)) result.add(condition);
    });
    return result;
  }

  /**
   * Condition name → owner, for error messages.
   */
  public synchronized Map<String, String> available(String excludedOwner) {
    Map<String, String> result = new LinkedHashMap<>();
    forEachLoaded(excludedOwner, condition -> result.merge(condition.name(), condition.getOwner(), (a, b) -> a + ", " + b));
    return result;
  }

  public synchronized List<String> problems() {
    return failed.values().stream().toList();
  }

  private void forEachLoaded(String excludedOwner, Consumer<MethodCondition> action) {
    for (ConditionSource source : sources) {
      if (failed.containsKey(source)) continue;

      String owner;
      List<MethodCondition> conditions;
      try {
        owner = source.owner();
        if (owner.equals(excludedOwner)) continue;

        conditions = loaded.get(source);
        if (conditions == null) {
          conditions = new ArrayList<>();
          for (Class<?> definition : source.definitionClasses()) {
            conditions.addAll(MethodCondition.of(owner, definition));
          }
          loaded.put(source, conditions);
        }
      } catch (RuntimeException | LinkageError e) {
        failed.put(source, source + ": " + e);
        continue;
      }

      conditions.forEach(action);
    }
  }
}
