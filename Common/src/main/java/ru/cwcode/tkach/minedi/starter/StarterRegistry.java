package ru.cwcode.tkach.minedi.starter;

import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.scanner.ClassIndex;
import ru.cwcode.tkach.minedi.scanner.PluginJar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * {@link Starter}s of every MineDI plugin of the server and of MineDI itself, found by reading bytecode. A starter class is
 * loaded only when a consumer depends on its plugin.
 */
public class StarterRegistry {
  public static final StarterRegistry GLOBAL = new StarterRegistry();

  private final List<PluginJar> sources = new CopyOnWriteArrayList<>();
  private final Map<Class<?>, StarterExtension> instances = new ConcurrentHashMap<>();

  public void addSource(PluginJar source) {
    if (sources.stream().noneMatch(existing -> existing == source)) sources.add(source);
  }

  /**
   * Applies, for every installed plugin of {@code dependencies} in their order, the starters of that plugin ordered by class
   * name.
   *
   * @param dependencies {@code depend}, then {@code softdepend}, as declared
   * @throws BeanDefinitionException when a starter cannot be created or fails
   */
  public void apply(StarterContext context, List<String> dependencies, Predicate<String> installed) {
    for (String dependency : dependencies) {
      if (!installed.test(dependency)) continue;

      for (Found found : find(dependency, context)) {
        String starter = found.className();
        context.setCurrentStarter(starter);
        try {
          instance(found).apply(context);
        } catch (BeanDefinitionException e) {
          throw e;
        } catch (RuntimeException | LinkageError e) {
          throw new BeanDefinitionException("%s: starter %s of plugin %s failed: %s"
                                              .formatted(context.getConsumerName(), starter, dependency, e), e);
        }
        context.getApplication().getLogger().info("Applied starter {} of plugin {}", starter, dependency);
      }
    }
    context.setCurrentStarter("");
  }

  List<Found> find(String plugin, StarterContext context) {
    List<Found> found = new ArrayList<>();

    for (PluginJar source : sources) {
      ClassIndex index;
      try {
        index = source.index();
      } catch (RuntimeException e) {
        context.getApplication().getLogger().warn("Starters of {} are not read: {}", source, e.toString());
        continue;
      }

      for (ClassIndex.Entry entry : index.entries()) {
        if (entry.starterPlugins().stream().anyMatch(plugin::equalsIgnoreCase)) {
          found.add(new Found(entry.name(), source));
        }
      }
    }

    found.sort(Comparator.comparing(Found::className));
    return found;
  }

  private StarterExtension instance(Found found) {
    Class<?> clazz;
    try {
      clazz = Class.forName(found.className(), true, found.source().loader());
    } catch (ClassNotFoundException e) {
      throw new BeanDefinitionException("Cannot load starter %s of %s: %s".formatted(found.className(), found.source(), e), e);
    }

    if (!StarterExtension.class.isAssignableFrom(clazz)) {
      throw new BeanDefinitionException("%s is annotated with @Starter but does not implement %s"
                                          .formatted(clazz.getName(), StarterExtension.class.getName()));
    }

    return instances.computeIfAbsent(clazz, key -> {
      try {
        return (StarterExtension) key.getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException e) {
        throw new BeanDefinitionException("Starter %s needs a public no-arg constructor: %s".formatted(key.getName(), e), e);
      }
    });
  }

  record Found(String className, PluginJar source) {
  }
}
