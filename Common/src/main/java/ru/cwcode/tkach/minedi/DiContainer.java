package ru.cwcode.tkach.minedi;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.annotation.Component;
import ru.cwcode.tkach.minedi.annotation.Lazy;
import ru.cwcode.tkach.minedi.data.BeanData;
import ru.cwcode.tkach.minedi.data.ForeignOwner;
import ru.cwcode.tkach.minedi.exception.AmbiguousBeanException;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.exception.CircularDependencyException;
import ru.cwcode.tkach.minedi.exception.ForeignBeanUnavailableException;
import ru.cwcode.tkach.minedi.exception.NoSuchBeanException;
import ru.cwcode.tkach.minedi.logging.LogLevel;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.event.BeanDestroyEvent;
import ru.cwcode.tkach.minedi.processing.event.ComponentRegisteredEvent;
import ru.cwcode.tkach.minedi.processing.event.ComponentsRegisteredEvent;
import ru.cwcode.tkach.minedi.provider.BeanProvider;
import ru.cwcode.tkach.minedi.provider.SingletonBeanProvider;
import ru.cwcode.tkach.minedi.condition.MethodCondition;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.scanner.ScanResult;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiContainer {
  private static final Object LAZY_OBJECT = new Object();
  final ThreadLocal<LinkedHashSet<Class<?>>> creatingStack = ThreadLocal.withInitial(LinkedHashSet::new);
  @Getter
  final DiApplication application;
  @Getter
  ClassScanner scanner;

  Set<Class<?>> classes;
  List<ScanResult.ConditionalClass> conditionalClasses = List.of();
  List<String> conditionDefinitions = List.of();
  Function<String, @Nullable Class<?>> scanLoader = name -> null;
  Map<Class<?>, BeanData> beans = new ConcurrentHashMap<>();
  Map<String, BeanProvider> beanProviders = new ConcurrentHashMap<>();
  Map<Class<?>, List<Field>> staticFields = new ConcurrentHashMap<>();

  final Map<Class<?>, Class<?>> compatibleBeans = new ConcurrentHashMap<>();
  final Map<Class<?>, Optional<ForeignOwner>> foreignOwners = new ConcurrentHashMap<>();
  final Set<Class<?>> notForeignTypes = ConcurrentHashMap.newKeySet();
  final Map<Class<?>, Set<ForeignImport>> importers = new ConcurrentHashMap<>();
  final Set<String> reportedProblems = ConcurrentHashMap.newKeySet();

  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;

    registerProvider(new SingletonBeanProvider(this));

    registerSingleton(this, this.getClass());
    registerSingleton(application, application.getClass());
  }

  public static Object getLazyObject() {
    return LAZY_OBJECT;
  }

  public void registerProvider(BeanProvider beanProvider) {
    beanProviders.put(beanProvider.scope(), beanProvider);
  }

  /**
   * Exact type first, then the only compatible bean of this application, then a bean of the plugin that owns the type.
   */
  public <T> Optional<T> get(Class<T> type) {
    return resolveKey(type, ResolveMode.STRICT, null).map(key -> type.cast(provide(key)));
  }

  /**
   * Exact lookup only: true when the type itself is registered in this container.
   */
  public boolean isBean(Class<?> clazz) {
    return beans.containsKey(clazz);
  }

  public void registerSingleton(Object bean, Class<?> as) {
    if (!beans.containsKey(as)) {
      BeanData beanData = new BeanData(as, this);
      this.beans.put(as, beanData);
      compatibleBeans.clear();
    }

    singletonBeanProvider().set(as, bean);
    injectBeanInStaticFields(as);

    application.getEventHandler().handleEvent(new BeanConstructedEvent(bean));
  }

  public void registerReference(Object bean, Class<?> as) {
    putReference(bean, as);
    linkToForeignOwner(bean, as);
  }

  public <T> T create(Class<T> clazz) {
    boolean added = creatingStack.get().add(clazz);
    if (!added) {
      throw new CircularDependencyException(clazz, creatingStack.get(), application.getName());
    }

    try {
      return create0(clazz);
    } finally {
      creatingStack.get().remove(clazz);
    }
  }

  public <T> T recreate(Class<T> clazz, @Nullable Object bean) {
    BeanData beanData = beans.get(clazz);
    if (beanData == null) return null;

    BeanProvider beanProvider = getBeanProvider(beanData.getScope());
    if (beanProvider.getBeanClasses().contains(clazz)) {
      application.getEventHandler().handleEvent(new BeanDestroyEvent(beanProvider.provide(beanData)));
    }

    if (bean == null) {
      bean = create(clazz);
    } else {
      populateBeanFields(bean);
      application.getEventHandler().handleEvent(new BeanConstructedEvent(bean));
    }

    Object finalBean = bean;
    setInjectedFields(beanData, finalBean);

    if (beanProvider instanceof SingletonBeanProvider sbp) {
      sbp.set(clazz, bean);
    }

    injectBeanInStaticFields(clazz);

    importers.getOrDefault(clazz, Set.of())
             .forEach(foreignImport -> foreignImport.container().replaceImported(foreignImport.localType(), finalBean));

    return (T) bean;
  }

  /**
   * @throws NoSuchBeanException when neither this application nor the plugin owning the type provides it
   */
  public <T> T createOrGet(Class<T> clazz) {
    Class<?> key = resolveKey(clazz, ResolveMode.STRICT, describeCreationChain())
      .orElseThrow(() -> new NoSuchBeanException(missingBeanMessage(clazz)));

    Object bean = provide(key);
    if (bean == null) throw new NoSuchBeanException(missingBeanMessage(clazz));

    return clazz.cast(bean);
  }

  public BeanData getData(Class<?> clazz) {
    return beans.get(clazz);
  }

  public BeanProvider getBeanProvider(String scope) {
    return beanProviders.get(scope);
  }

  public void populateExternalObject(Object instance) {
    for (InjectionPoint point : injectionPoints(instance)) {
      Field field = point.field();
      try {
        field.setAccessible(true);

        if (field.get(instance) == null) {
          field.set(instance, provide(point.key()));
        }
      } catch (Exception e) {
        application.getLogger().warn("Cannot inject {} into {}", point.key().getName(), describe(field), e);
      }
    }
  }

  public void populateBeanFields(Object instance) {
    List<InjectionPoint> points = injectionPoints(instance);
    points.sort(Comparator.comparingInt(point -> Modifier.isFinal(point.field().getModifiers()) ? 0 : 1)); //first final

    for (InjectionPoint point : points) {
      BeanData beanData = beans.get(instance.getClass());
      if (beanData != null && beanData.getScope().equals(BeanScope.SINGLETON) && isPopulated(instance, points)) {
        singletonBeanProvider().set(instance.getClass(), instance);
      }

      Field field = point.field();
      try {
        field.setAccessible(true);

        if (field.get(instance) == null) {
          field.set(instance, provide(point.key()));
        }

        beans.get(point.key()).getBeanFields().put(field, instance); //todo clean fields of removed objects
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void registerComponent(Class<?> clazz) {
    registerBean(clazz, false);
  }

  public void registerBean(Class<?> clazz, boolean ignoreComponentRequiredAnnotation) {
    if (beans.containsKey(clazz)) return;

    BeanData value;
    int constructors;
    try {
      value = new BeanData(clazz, this);

      if (!ignoreComponentRequiredAnnotation && !value.isAnnotationPresent(Component.class)) {
        application.getLogger().info("Class {} ignored", clazz.getName());
        return;
      }

      if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
        application.getLogger().info("Class {} skipped: interfaces and abstract classes are not beans", clazz.getName());
        return;
      }

      constructors = clazz.getDeclaredConstructors().length;
    } catch (LinkageError | TypeNotPresentException e) {
      // a nested or synthetic class of an integration over an absent soft-dependency (Outer$1 of an enum switch)
      application.getLogger().warn("Class {} skipped, it refers to a missing class: {}", clazz.getName(), e.toString());
      return;
    }
    if (constructors != 1) {
      throw new BeanDefinitionException(("%s: bean %s declares %d constructors, a bean needs exactly one. "
                                         + "Lombok @NoArgsConstructor/@AllArgsConstructor/@RequiredArgsConstructor count too")
                                          .formatted(application.getName(), clazz.getName(), constructors));
    }

    beans.put(clazz, value);
    compatibleBeans.clear();
    application.getEventHandler().handleEvent(new ComponentRegisteredEvent(clazz));
  }

  public void registerSingletonBean(Class<?> clazz, Object instance) {
    BeanData value = new BeanData(clazz, this);

    beans.put(clazz, value);
    compatibleBeans.clear();
    singletonBeanProvider().set(clazz, instance);

    application.getEventHandler().handleEvent(new ComponentRegisteredEvent(clazz));

    populateBeanFields(instance);
    injectOwnStaticFields(instance.getClass());
    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    injectBeanInStaticFields(clazz);
  }

  /**
   * A class created by a {@code @Bean} method is not scanned (an {@code *Integration} is even excluded), so its own static
   * fields are indexed here: filled now and refreshed on reload like those of scanned classes.
   */
  private void injectOwnStaticFields(Class<?> clazz) {
    if (classes != null && classes.contains(clazz)) return;

    Field[] declared;
    try {
      declared = clazz.getDeclaredFields();
    } catch (LinkageError | TypeNotPresentException e) {
      application.getLogger().warn("Static fields of {} are not injected: {}", clazz.getName(), e.toString());
      return;
    }

    for (Field field : declared) {
      if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;

      List<Field> fields = staticFields.computeIfAbsent(field.getType(), key -> new ArrayList<>());
      if (!fields.contains(field)) fields.add(field);

      resolveKey(field.getType(), ResolveMode.WARN, describe(field)).ifPresent(key -> {
        try {
          field.setAccessible(true);
          field.set(null, provide(key));
        } catch (IllegalAccessException e) {
          throw new IllegalStateException("Cannot inject " + describe(field), e);
        }
      });
    }
  }

  public boolean validateBean(Class<?> clazz) {
    return clazz.getDeclaredConstructors().length == 1 && !beans.containsKey(clazz);
  }

  protected void registerBeans() {
    application.getLogger().info("Registering components");
    classes.forEach(this::registerComponent);
    application.getEventHandler().handleEvent(new ComponentsRegisteredEvent());

    importForeignStaticFieldTypes();
    beans.forEach((clazz, data) -> injectBeanInStaticFields(clazz));

    constructSingletons();
  }

  protected void scanClasses() {
    application.getLogger().info("Scanning classes...");
    ScanResult result = scanner.scanResult();
    classes = ConcurrentHashMap.newKeySet();
    classes.addAll(result.classes());
    conditionalClasses = result.conditional();
    conditionDefinitions = result.conditionDefinitions();
    scanLoader = result.loader();
    application.getLogger().info("Founded {} classes, {} more under conditions", classes.size(), conditionalClasses.size());

    staticFields.clear();
    scanForStaticFields(classes);
  }

  /**
   * Registers the conditions this plugin defines, then loads the conditional classes whose conditions hold.
   * Runs when the container starts: other plugins and the configs are there by then.
   */
  protected void resolveConditions() {
    for (String definition : conditionDefinitions) {
      Class<?> clazz = scanLoader.apply(definition);
      if (clazz == null) {
        throw new BeanDefinitionException("%s: condition class %s cannot be loaded".formatted(application.getName(), definition));
      }
      MethodCondition.of(application.getName(), clazz).forEach(application.getConditionParser()::registerOwn);
    }

    Map<String, Boolean> results = new HashMap<>();
    List<Class<?>> accepted = new ArrayList<>();
    int skipped = 0;

    for (ScanResult.ConditionalClass conditional : conditionalClasses) {
      String failedCondition = null;
      for (String condition : conditional.conditions()) {
        boolean holds;
        try {
          holds = results.computeIfAbsent(condition, application.getConditionParser()::parse);
        } catch (BeanDefinitionException e) {
          throw new BeanDefinitionException("%s (in @Conditional of %s)".formatted(e.getMessage(), conditional.name()));
        }
        if (!holds) {
          failedCondition = condition;
          break;
        }
      }

      if (failedCondition != null) {
        skipped++;
        application.getLogger().info("Class {} skipped: condition '{}' is false", conditional.name(), failedCondition);
        continue;
      }

      Class<?> clazz = scanLoader.apply(conditional.name());
      if (clazz != null) accepted.add(clazz);
    }

    classes.addAll(accepted);
    scanForStaticFields(accepted);
    conditionalClasses = List.of();
    conditionDefinitions = List.of();

    if (skipped > 0) {
      application.getLogger().info("{} classes skipped by conditions, {} loaded", skipped, accepted.size());
    }
  }

  /**
   * A scanned class by full or simple name, for conditions that read a bean.
   */
  public @Nullable Class<?> findScannedClass(String name) {
    if (classes == null) return null;

    List<Class<?>> found = classes.stream()
                                  .filter(clazz -> clazz.getName().equals(name) || clazz.getSimpleName().equals(name))
                                  .toList();
    if (found.size() > 1) {
      throw new BeanDefinitionException("%s: several classes are named %s: %s, use the full name"
                                          .formatted(application.getName(), name, found.stream().map(Class::getName).toList()));
    }
    return found.isEmpty() ? null : found.get(0);
  }

  /**
   * The bean of a scanned class while conditions are evaluated, before the components are registered.
   */
  public Object getForCondition(Class<?> clazz) {
    registerComponent(clazz);
    if (!beans.containsKey(clazz)) {
      throw new BeanDefinitionException("%s: %s is not a component".formatted(application.getName(), clazz.getName()));
    }
    return createOrGet(clazz);
  }

  public SingletonBeanProvider singletonBeanProvider() {
    return (SingletonBeanProvider) beanProviders.get(BeanScope.SINGLETON);
  }

  private Object provide(Class<?> key) {
    BeanData beanData = beans.get(key);
    BeanProvider beanProvider = beanProviders.get(beanData.getScope());
    if (beanProvider == null) return null;

    return beanProvider.provide(beanData);
  }

  private void putReference(Object bean, Class<?> as) {
    if (!beans.containsKey(as)) {
      BeanData beanData = new BeanData(as, this);
      this.beans.put(as, beanData);
      compatibleBeans.clear();
    }

    singletonBeanProvider().set(as, bean);
    injectBeanInStaticFields(as);
  }

  private void setInjectedFields(BeanData beanData, Object bean) {
    beanData.getBeanFields().forEach((field, origin) -> {
      try {
        field.setAccessible(true);
        field.set(origin, bean);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot update " + describe(field) + " with a recreated " + beanData.getClazz().getName(), e);
      }
    });
  }

  private List<InjectionPoint> injectionPoints(Object instance) {
    List<InjectionPoint> points = new ArrayList<>();

    for (Field field : ReflectionUtils.getFields(instance.getClass())) {
      resolveKey(field.getType(), ResolveMode.WARN, describe(field))
        .ifPresent(key -> points.add(new InjectionPoint(field, key)));
    }

    return points;
  }

  private boolean isPopulated(Object instance, List<InjectionPoint> points) {
    return points.stream().allMatch(point -> {
      try {
        point.field().setAccessible(true);
        return point.field().get(instance) != null;
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Optional<Class<?>> resolveKey(Class<?> type, ResolveMode mode, @Nullable String requiredBy) {
    if (beans.containsKey(type)) return Optional.of(type);
    if (type.isPrimitive() || type.isArray()) return Optional.empty();

    Optional<Class<?>> compatible = findCompatible(type, mode, requiredBy);
    if (compatible.isPresent()) return compatible;

    return importForeign(type, mode, requiredBy) ? Optional.of(type) : Optional.empty();
  }

  private Optional<Class<?>> resolveLocalKey(Class<?> type) {
    if (beans.containsKey(type)) return Optional.of(type);

    return findCompatible(type, ResolveMode.STRICT, null);
  }

  /**
   * Only interfaces and abstract classes of plugin code take part: a field of a JDK, server or library type
   * (Object, Runnable, Plugin) must never pick up a random bean.
   */
  private Optional<Class<?>> findCompatible(Class<?> type, ResolveMode mode, @Nullable String requiredBy) {
    if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) return Optional.empty();
    if (!isApplicationType(type)) return Optional.empty();

    Class<?> cached = compatibleBeans.get(type);
    if (cached != null && beans.containsKey(cached)) return Optional.of(cached);

    List<Class<?>> candidates = compatibleCandidates(type);
    if (candidates.isEmpty()) return Optional.empty();

    if (candidates.size() > 1) {
      String names = candidates.stream().map(Class::getName).sorted().collect(Collectors.joining(", "));
      String message = ("%s: %d beans are compatible with %s: [%s]. Register the wanted one with an exact type "
                        + "(@Bean(as = ...) or registerReference(bean, type))")
        .formatted(application.getName(), candidates.size(), type.getName(), names);

      fail(mode, new AmbiguousBeanException(withRequiredBy(message, requiredBy)), LogLevel.WARNING);
      return Optional.empty();
    }

    compatibleBeans.put(type, candidates.get(0));
    return Optional.of(candidates.get(0));
  }

  private List<Class<?>> compatibleCandidates(Class<?> type) {
    List<Class<?>> candidates = new ArrayList<>();
    Map<Object, Class<?>> seenInstances = new IdentityHashMap<>();

    for (Class<?> key : beans.keySet()) {
      if (key == type || !type.isAssignableFrom(key)) continue;

      Object existing = singletonBeanProvider().getExisting(key);
      if (existing != null && seenInstances.putIfAbsent(existing, key) != null) continue;

      candidates.add(key);
    }

    return candidates;
  }

  private boolean isApplicationType(Class<?> type) {
    return (classes != null && classes.contains(type)) || foreignOwnerOf(type).isPresent();
  }

  private Optional<ForeignOwner> foreignOwnerOf(Class<?> type) {
    return foreignOwners.computeIfAbsent(type, key -> application.getForeignResolver()
                                                                  .ownerOf(key)
                                                                  .filter(owner -> owner.application() != application));
  }

  private synchronized boolean importForeign(Class<?> type, ResolveMode mode, @Nullable String requiredBy) {
    if (beans.containsKey(type)) return true;
    if (notForeignTypes.contains(type)) return false;

    Optional<ForeignOwner> foreignOwner = foreignOwnerOf(type);
    if (foreignOwner.isEmpty()) {
      notForeignTypes.add(type);
      return false;
    }

    ForeignOwner owner = foreignOwner.get();
    if (!owner.application().isStarted()) {
      String message = "%s: %s belongs to plugin %s, which has not started yet. Add %s to depend/softdepend (Velocity: @Plugin dependencies) of %s"
        .formatted(application.getName(), type.getName(), owner.name(), owner.name(), application.getName());
      // A field may legitimately point at a plugin that enables later and resolve it lazily; constructors and get() still throw
      return fail(mode, new ForeignBeanUnavailableException(withRequiredBy(message, requiredBy)), LogLevel.WARNING);
    }

    DiContainer ownerContainer = owner.application().getContainer();
    Optional<Class<?>> ownerKey;
    try {
      ownerKey = ownerContainer.resolveLocalKey(type);
    } catch (AmbiguousBeanException e) {
      return fail(mode, new AmbiguousBeanException(withRequiredBy(e.getMessage(), requiredBy)), LogLevel.WARNING);
    }

    if (ownerKey.isEmpty()) {
      if (isComponent(type)) {
        String message = "%s: plugin %s has no bean %s: its @Bean condition did not pass, the class is excluded from scanning or the plugin failed to start"
          .formatted(application.getName(), owner.name(), type.getName());
        return fail(mode, new ForeignBeanUnavailableException(withRequiredBy(message, requiredBy)), LogLevel.ERROR);
      }

      notForeignTypes.add(type);
      return false;
    }

    BeanData ownerData = ownerContainer.getData(ownerKey.get());
    if (!BeanScope.SINGLETON.equals(ownerData.getScope())) {
      String message = "%s: %s of plugin %s has scope %s, only singletons are shared between plugins"
        .formatted(application.getName(), type.getName(), owner.name(), ownerData.getScope());
      return fail(mode, new BeanDefinitionException(withRequiredBy(message, requiredBy)), LogLevel.ERROR);
    }

    Object bean = ownerContainer.provide(ownerKey.get());
    if (bean == null) return false;

    putReference(bean, type);
    ownerContainer.addImporter(ownerKey.get(), this, type);
    application.getLogger().info("Imported {} from plugin {}", type.getName(), owner.name());

    return true;
  }

  /**
   * A manual registerReference of another plugin's bean still gets reload updates from its owner.
   */
  private void linkToForeignOwner(Object bean, Class<?> as) {
    try {
      Optional<ForeignOwner> owner = foreignOwnerOf(as);
      if (owner.isEmpty() || !owner.get().application().isStarted()) return;

      DiContainer ownerContainer = owner.get().application().getContainer();
      ownerContainer.resolveLocalKey(as)
                    .filter(key -> ownerContainer.singletonBeanProvider().getExisting(key) == bean)
                    .ifPresent(key -> ownerContainer.addImporter(key, this, as));
    } catch (RuntimeException | LinkageError e) {
      application.getLogger().debug("Reference {} is not linked to its owner: {}", as.getName(), e.getMessage());
    }
  }

  private void addImporter(Class<?> ownerKey, DiContainer importer, Class<?> localType) {
    importers.computeIfAbsent(ownerKey, key -> ConcurrentHashMap.newKeySet())
             .add(new ForeignImport(importer, localType));
  }

  private void replaceImported(Class<?> type, Object bean) {
    BeanData beanData = beans.get(type);
    if (beanData == null) return;

    singletonBeanProvider().set(type, bean);
    setInjectedFields(beanData, bean);
    injectBeanInStaticFields(type);
  }

  private void importForeignStaticFieldTypes() {
    staticFields.forEach((type, fields) -> {
      if (beans.containsKey(type)) return;

      fields.stream()
            .filter(field -> !Modifier.isFinal(field.getModifiers()))
            .findFirst()
            .ifPresent(field -> importForeign(type, ResolveMode.WARN, describe(field)));
    });
  }

  private boolean fail(ResolveMode mode, RuntimeException problem, LogLevel level) {
    if (mode == ResolveMode.STRICT) throw problem;

    if (reportedProblems.add(problem.getMessage())) {
      application.getLogger().log(level, "{}", problem.getMessage());
    }
    return false;
  }

  private boolean isComponent(Class<?> type) {
    try {
      return ReflectionUtils.getAnnotations(type).stream().anyMatch(annotation -> annotation.annotationType() == Component.class);
    } catch (LinkageError | TypeNotPresentException e) {
      return false;
    }
  }

  private String missingBeanMessage(Class<?> type) {
    String chain = describeCreationChain();
    String requiredBy = chain == null ? "requested directly" : "required by " + chain;

    return "%s: no bean of type %s, %s. Annotate it with @Service/@Component, produce it with @Bean or register it with registerReference"
      .formatted(application.getName(), type.getName(), requiredBy);
  }

  private @Nullable String describeCreationChain() {
    List<Class<?>> stack = new ArrayList<>(creatingStack.get());
    if (stack.isEmpty()) return null;

    Collections.reverse(stack);
    return stack.stream().map(Class::getName).collect(Collectors.joining(" <- "));
  }

  private static String withRequiredBy(String message, @Nullable String requiredBy) {
    return requiredBy == null ? message : message + " (required by " + requiredBy + ")";
  }

  private static String describe(Field field) {
    return field.getDeclaringClass().getName() + "." + field.getName();
  }

  private void scanForStaticFields(Collection<Class<?>> scanned) {
    application.getLogger().info("Scanning for static fields");

    for (Class<?> clazz : scanned) {
      try {
        for (Field declaredField : clazz.getDeclaredFields()) {
          if (Modifier.isStatic(declaredField.getModifiers())) {
            staticFields.computeIfAbsent(declaredField.getType(), (e) -> new ArrayList<>())
                        .add(declaredField);
          }
        }
      } catch (LinkageError | TypeNotPresentException e) {
        application.getLogger().warn("Static fields scan skipped for {}: {}", clazz.getName(), e.getMessage());
      }
    }
  }

  private void injectBeanInStaticFields(Class<?> clazz) {
    application.getLogger().info("Injecting {} in static fields", clazz);

    for (Field field : staticFields.getOrDefault(clazz, List.of())) {

      try {
        field.setAccessible(true);

        if (Modifier.isFinal(field.getModifiers())) continue;

        Object toInject = get(clazz).orElseThrow();

        application.getLogger().info("Injecting {} to static field {} of {}", toInject.getClass(), field.getName(), field.getDeclaringClass());

        field.set(null, toInject);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void constructSingletons() {
    application.getLogger().info("Constructing singletons");

    SingletonBeanProvider singletonBeanProvider = (SingletonBeanProvider) getBeanProvider(BeanScope.SINGLETON);

    beans.forEach((clazz, data) -> {
      if (!data.getScope().equals(BeanScope.SINGLETON)) return;

      if (!data.isAnnotationPresent(Lazy.class)) {
        createOrGet(clazz);
      }
    });

    // a copy: populating a field may import another plugin's bean, which adds a singleton
    List.copyOf(singletonBeanProvider.getBeanClasses())
        .stream()
        .map(x -> get(x).orElseThrow())
        .forEach(this::populateBeanFields);
  }

  private <T> T create0(Class<T> clazz) {
    BeanData data = beans.get(clazz);

    T instance = application.getBeanConstructors().construct(clazz, data);

    if (data.getScope().equals(BeanScope.SINGLETON)) { //todo catch by events
      ((SingletonBeanProvider) getBeanProvider(BeanScope.SINGLETON)).set(clazz, instance);
    }

    populateBeanFields(instance);

    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));

    return instance;
  }

  private enum ResolveMode {
    STRICT,
    WARN
  }

  private record InjectionPoint(Field field, Class<?> key) {
  }

  private record ForeignImport(DiContainer container, Class<?> localType) {
  }
}
