package ru.cwcode.tkach.minedi;

import lombok.Getter;
import lombok.Setter;
import ru.cwcode.tkach.minedi.condition.ConditionParser;
import ru.cwcode.tkach.minedi.condition.ConditionRegistry;
import ru.cwcode.tkach.minedi.condition.EnvCondition;
import ru.cwcode.tkach.minedi.condition.HasClassCondition;
import ru.cwcode.tkach.minedi.condition.PropertyCondition;
import ru.cwcode.tkach.minedi.constructor.BeanConstructorImpl;
import ru.cwcode.tkach.minedi.constructor.BeanConstructors;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;
import ru.cwcode.tkach.minedi.foreign.ForeignBeanResolver;
import ru.cwcode.tkach.minedi.logging.Log;
import ru.cwcode.tkach.minedi.processing.EventHandler;
import ru.cwcode.tkach.minedi.processing.EventHandlerImpl;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.scanner.JarClassScanner;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@Getter
public class DiApplication {
  private final DiContainer container;
  private final EventHandler eventHandler;
  private final BeanConstructors beanConstructors;
  private final List<Extension> extensions = new LinkedList<>();
  private final Log logger;
  private final ConditionParser conditionParser = new ConditionParser();

  /**
   * Plugin name used in error messages.
   */
  private String name = "MineDI";
  @Setter
  private ForeignBeanResolver foreignResolver = ForeignBeanResolver.NONE;
  /**
   * Where extension point implementations and config subtypes are shared; {@link ExtensionRegistry#GLOBAL} by default.
   */
  @Setter
  private ExtensionRegistry extensionRegistry = ExtensionRegistry.GLOBAL;
  private boolean started = false;

  boolean isLoaded = false;

  public DiApplication(Log logger, File jarfile, String packageName) {
    this(logger, JarClassScanner.builder()
                                .jar(jarfile)
                                .packageName(packageName)
                                .loadFailureListener(loadFailureLogger(logger))
                                .build());

    excludeIntegrationsWithoutCondition();
  }

  public DiApplication(Log logger, File jarfile, String packageName, ClassLoader classLoader) {
    this(logger, JarClassScanner.builder()
                        .jar(jarfile)
                        .packageName(packageName)
                        .classLoader(classLoader)
                        .loadFailureListener(loadFailureLogger(logger))
                        .build());

    excludeIntegrationsWithoutCondition();
    conditionParser.register(new HasClassCondition(classLoader));
  }

  private static BiConsumer<String, Throwable> loadFailureLogger(Log logger) {
    return (className, error) -> logger.warn("Class {} is not loaded and will not be scanned: {}", className, error.toString());
  }
  
  public DiApplication(Log logger, ClassScanner scanner) {
    this.logger = logger;
    this.eventHandler = new EventHandlerImpl(this);
    this.container = new DiContainer(scanner, this);
    this.beanConstructors = new BeanConstructors(this);
    
    this.beanConstructors.getConstructors().addFirst(new BeanConstructorImpl());

    conditionParser.setRegistry(ConditionRegistry.GLOBAL);
    conditionParser.register(new EnvCondition());
    conditionParser.register(new HasClassCondition(DiApplication.class.getClassLoader()));
    conditionParser.register(new PropertyCondition(this));
  }

  /**
   * The {@code *Integration} suffix keeps a class out of the scan unless it carries an explicit
   * {@link ru.cwcode.tkach.minedi.annotation.Conditional}.
   */
  private void excludeIntegrationsWithoutCondition() {
    container.getScanner().addUnconditionalClassNameFilter(className -> !className.endsWith("Integration"));
  }

  public void setName(String name) {
    this.name = name;
    conditionParser.setOwner(name);
  }

  /**
   * Where conditions of other plugins come from; {@link ConditionRegistry#GLOBAL} by default.
   */
  public void setConditionRegistry(ConditionRegistry registry) {
    conditionParser.setRegistry(registry);
  }
  
  public void registerExtension(Extension extension) {
    extensions.add(extension);
    extension.onRegister(this);
  }

  public void excludePackageFromAutoScan(String packageName) {
    String normalized = packageName.endsWith(".") ? packageName.substring(0, packageName.length() - 1) : packageName;
    container.getScanner().addClassNameFilter(className -> !className.equals(normalized) && !className.startsWith(normalized + "."));
  }

  public void excludeClassFromAutoScan(String className) {
    container.getScanner().addClassNameFilter(name -> !name.equals(className));
  }

  public void excludeFromAutoScan(Predicate<String> excludeFilter) {
    container.getScanner().addClassNameFilter(className -> !excludeFilter.test(className));
  }
  
  public <T> Optional<T> get(Class<T> clazz) {
    return container.get(clazz);
  }
  
  public <T> void register(T bean, Class<T> as) {
    container.registerSingleton(bean, as);
  }
  
  public <T> void registerReference(T bean, Class<T> as) {
    container.registerReference(bean, as);
  }
  
  @Deprecated
  public void register(Optional<?> bean) {
    register(bean.orElseThrow());
  }
  
  public void register(Object bean) {
    container.registerSingleton(bean, bean.getClass());
  }
  
  public void registerReference(Object bean) {
    container.registerReference(bean, bean.getClass());
  }
  
  public void start() {
    if(!isLoaded) load();
    logger.info("Starting MineDI");
    List.copyOf(extensions).forEach(extension -> extension.onBeforeStart(this));
    container.resolveConditions();
    container.registerBeans();
    
    extensions.forEach(extension -> extension.onStart(this));
    started = true;
  }
  
  public void load() {
    logger.info("Loading MineDI");
    
    container.scanClasses();
  }
}
