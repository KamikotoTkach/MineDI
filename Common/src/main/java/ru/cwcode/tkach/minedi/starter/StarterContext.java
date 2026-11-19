package ru.cwcode.tkach.minedi.starter;

import lombok.Getter;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.condition.Condition;
import ru.cwcode.tkach.minedi.constructor.BeanConstructor;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.processing.event.Event;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.io.File;
import java.util.function.Supplier;

/**
 * The consumer a starter is applied to, and what a starter may add to it. A type the consumer has already registered
 * itself is left alone: manual registration wins.
 */
@Getter
public class StarterContext {
  private final DiApplication application;
  private final String consumerName;
  private final File dataFolder;
  private final ClassLoader classLoader;
  /**
   * {@code JavaPlugin} on Paper, the plugin instance ({@code VelocityPlatform}) on Velocity.
   */
  private final Object platformPlugin;

  private String currentStarter = "";

  public StarterContext(DiApplication application, String consumerName, File dataFolder, ClassLoader classLoader, Object platformPlugin) {
    this.application = application;
    this.consumerName = consumerName;
    this.dataFolder = dataFolder;
    this.classLoader = classLoader;
    this.platformPlugin = platformPlugin;
  }

  void setCurrentStarter(String currentStarter) {
    this.currentStarter = currentStarter;
  }

  public <T> void bean(T bean, Class<? super T> as) {
    if (skipRegistered(as)) return;
    application.getContainer().registerSingleton(bean, as);
  }

  /**
   * Registered under its own class: the form for a bean found by a compatible type, such as a config mapper customizer.
   */
  public void bean(Object bean) {
    if (skipRegistered(bean.getClass())) return;
    application.getContainer().registerSingleton(bean, bean.getClass());
  }

  /**
   * Like {@link #bean(Object, Class)}, but no processor sees it: for an object that has already set itself up (a Bukkit
   * listener registered by its own constructor would otherwise be registered twice).
   */
  public <T> void reference(T bean, Class<? super T> as) {
    if (skipRegistered(as)) return;
    application.getContainer().registerReference(bean, as);
  }

  /**
   * Created on the first injection or lookup; a consumer that never asks for the type never runs {@code factory}.
   */
  public <T> void lazyBean(Class<T> type, Supplier<? extends T> factory) {
    if (skipRegistered(type)) return;
    application.getContainer().registerLazy(type, factory);
  }

  public void constructor(BeanConstructor constructor) {
    application.getBeanConstructors().getConstructors().addFirst(constructor);
  }

  public void processor(EventProcessor<? extends Event> processor) {
    application.getEventHandler().registerProcessor(processor);
  }

  public void condition(Condition condition) {
    application.getConditionParser().register(condition);
  }

  public void extension(Extension extension) {
    application.registerExtension(extension);
  }

  private boolean skipRegistered(Class<?> type) {
    if (!application.getContainer().isBean(type)) return false;

    application.getLogger().debug("Starter {} skips {}: {} registers it itself", currentStarter, type.getName(), consumerName);
    return true;
  }
}
