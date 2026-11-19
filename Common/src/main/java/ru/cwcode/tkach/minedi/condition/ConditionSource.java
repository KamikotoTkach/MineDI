package ru.cwcode.tkach.minedi.condition;

import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.scanner.ClassIndex;
import ru.cwcode.tkach.minedi.scanner.PluginJar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Where a plugin's {@link ru.cwcode.tkach.minedi.annotation.DefineCondition} methods live.
 */
public interface ConditionSource {
  String owner();

  /**
   * Loads only the classes that declare condition methods.
   */
  List<Class<?>> definitionClasses();

  /**
   * Suppliers are read on the first lookup: on Velocity the plugin id is not known while the plugin is constructed.
   */
  static ConditionSource ofJar(Supplier<String> owner, Supplier<File> jar, String packageName, ClassLoader loader) {
    return of(new PluginJar(owner, jar, packageName, loader));
  }

  static ConditionSource of(PluginJar pluginJar) {
    return new ConditionSource() {
      @Override
      public String owner() {
        return pluginJar.owner();
      }

      @Override
      public List<Class<?>> definitionClasses() {
        ClassIndex index = pluginJar.index();
        List<Class<?>> classes = new ArrayList<>();

        for (ClassIndex.Entry entry : index.entries()) {
          if (!entry.definesConditions()) continue;

          List<String> conditions = index.conditionsOf(entry.name());
          if (!conditions.isEmpty()) {
            throw new BeanDefinitionException("%s defines conditions but is itself under %s: a condition cannot depend on a condition"
                                                .formatted(entry.name(), conditions));
          }

          try {
            classes.add(Class.forName(entry.name(), false, pluginJar.loader()));
          } catch (ClassNotFoundException e) {
            throw new BeanDefinitionException("Cannot load condition class " + entry.name() + ": " + e);
          }
        }

        return classes;
      }

      @Override
      public String toString() {
        return pluginJar.toString();
      }
    };
  }
}
