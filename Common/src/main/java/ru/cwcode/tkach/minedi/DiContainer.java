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

import ru.cwcode.tkach.minedi.data.CollectionRequest;
import ru.cwcode.tkach.minedi.extensionpoint.BeanOrder;
import ru.cwcode.tkach.minedi.extensionpoint.ConfigSubtype;
import ru.cwcode.tkach.minedi.extensionpoint.ExtensionRegistry;
import ru.cwcode.tkach.minedi.annotation.PlayerScoped;
import ru.cwcode.tkach.minedi.annotation.Scope;
import ru.cwcode.tkach.minedi.scope.KeyedMap;
import ru.cwcode.tkach.minedi.scope.KeyedSet;
import ru.cwcode.tkach.minedi.scope.PlayerScopeProvider;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;
import ru.cwcode.tkach.minedi.scope.ScopedBeans;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
  final List<Predicate<Class<?>>> collectionInjectionFilters = new CopyOnWriteArrayList<>();
  List<ScanResult.SubtypeCandidate> subtypeCandidates = List.of();
  @Getter
  final PlayerScopeProvider playerScope;

  public DiContainer(ClassScanner scanner, DiApplication application) {
    this.scanner = scanner;
    this.application = application;

    registerProvider(new SingletonBeanProvider(this));
    registerProvider(playerScope = new PlayerScopeProvider(this));

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

  /**
   * Classes found by the scan without conditions; empty before {@link DiApplication#load()}.
   */
  public Set<Class<?>> scannedClasses() {
    return classes == null ? Set.of() : Set.copyOf(classes);
  }

  /**
   * Every registered key, a snapshot.
   */
  public Set<Class<?>> beanTypes() {
    return Set.copyOf(beans.keySet());
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
    publishExtension(null, bean);
  }

  /**
   * A singleton created by {@code factory} on the first injection or lookup, never by the eager pass over singletons.
   */
  public <T> void registerLazy(Class<T> type, Supplier<? extends T> factory) {
    BeanData beanData = new BeanData(type, this);
    beanData.setFactory(factory);

    beans.put(type, beanData);
    compatibleBeans.clear();
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
    Object old = singletonBeanProvider().getExisting(clazz);
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
      publishExtension(old, bean);
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
    boolean playerBean = playerScope.isPlayerBean(instance.getClass());
    if (playerBean) injectPlayer(instance);
    injectScopeHandles(instance, playerBean);

    List<InjectionPoint> points = injectionPoints(instance);
    points.sort(Comparator.comparingInt(point -> Modifier.isFinal(point.field().getModifiers()) ? 0 : 1)); //first final

    for (InjectionPoint point : points) {
      BeanData beanData = beans.get(instance.getClass());
      if (playerBean) {
        // an instance dies with its player: it must not be remembered for reload updates
        injectField(instance, point);
        continue;
      }
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

    injectCollections(instance);
  }

  private void injectField(Object instance, InjectionPoint point) {
    Field field = point.field();
    try {
      field.setAccessible(true);
      if (field.get(instance) == null) field.set(instance, provide(point.key()));
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot inject " + describe(field), e);
    }
  }

  private boolean isPlayerType(Class<?> type) {
    Class<?> keyType = application.getPlayerWindows().keyType();
    return keyType != null && type != Object.class && type.isAssignableFrom(keyType);
  }

  private Object currentPlayer(Class<?> owner) {
    UUID id = PlayerScopeProvider.currentPlayer();
    Object player = id == null ? null : application.getPlayerWindows().key(id);
    if (player == null) {
      throw new IllegalStateException("%s: %s is created outside its player's window".formatted(application.getName(), owner.getName()));
    }
    return player;
  }

  /**
   * The player of a player bean goes into every field of the platform player type (or a supertype of it).
   */
  private void injectPlayer(Object instance) {
    for (Field field : ReflectionUtils.getFields(instance.getClass())) {
      if (Modifier.isStatic(field.getModifiers()) || !isPlayerType(field.getType())) continue;

      try {
        field.setAccessible(true);
        if (field.get(instance) == null) field.set(instance, currentPlayer(instance.getClass()));
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot inject the player into " + describe(field), e);
      }
    }
  }

  private void injectScopeHandles(Object instance, boolean playerBean) {
    for (Field field : ReflectionUtils.getFields(instance.getClass())) {
      // Type first: setAccessible on a JDK-module class (a Clock bean) throws InaccessibleObjectException.
      if (Modifier.isStatic(field.getModifiers()) || !isScopeHandleType(field.getType())) continue;

      try {
        field.setAccessible(true);
        if (field.get(instance) != null) continue;

        Object handle = scopeHandle(field.getType(), field.getGenericType(), describe(field), playerBean);
        if (handle != null) field.set(instance, handle);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot inject " + describe(field), e);
      }
    }
  }

  /**
   * A new {@code PlayerScope<T>}, {@code PlayerMap<V>} or {@code PlayerSet} (or their base classes) for a field or
   * constructor parameter of that type; null for any other type.
   */
  private @Nullable Object scopeHandle(Class<?> raw, java.lang.reflect.Type generic, String requiredBy, boolean playerBean) {
    PlayerWindows windows = application.getPlayerWindows();
    try {
      if (ScopedBeans.class.isAssignableFrom(raw)) {
        Class<?> element = lastTypeArgument(generic);
        if (element == null) {
          throw new BeanDefinitionException("%s: %s needs the bean type, as in PlayerScope<MyPlayerState>".formatted(application.getName(), requiredBy));
        }
        if (!playerScope.isPlayerBean(element)) {
          throw new BeanDefinitionException("%s: %s asks for a scope of %s, which is not a @PlayerScoped bean of this plugin"
                                              .formatted(application.getName(), requiredBy, element.getName()));
        }
        return raw.getDeclaredConstructor(PlayerScopeProvider.class, Class.class).newInstance(playerScope, element);
      }

      if (KeyedMap.class.isAssignableFrom(raw) || KeyedSet.class.isAssignableFrom(raw)) {
        if (playerBean) {
          throw new BeanDefinitionException("%s: %s is a player map or set inside a player bean; a plain field holds that state already"
                                              .formatted(application.getName(), requiredBy));
        }
        return raw.getDeclaredConstructor(PlayerWindows.class).newInstance(windows);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot create " + raw.getName() + " for " + requiredBy, e);
    }
    return null;
  }

  private static boolean isScopeHandleType(Class<?> raw) {
    return ScopedBeans.class.isAssignableFrom(raw) || KeyedMap.class.isAssignableFrom(raw) || KeyedSet.class.isAssignableFrom(raw);
  }

  private static @Nullable Class<?> lastTypeArgument(java.lang.reflect.Type generic) {
    if (!(generic instanceof java.lang.reflect.ParameterizedType parameterized)) return null;

    java.lang.reflect.Type[] arguments = parameterized.getActualTypeArguments();
    java.lang.reflect.Type last = arguments[arguments.length - 1];
    if (last instanceof Class<?> clazz) return clazz;
    if (last instanceof java.lang.reflect.ParameterizedType nested && nested.getRawType() instanceof Class<?> clazz) return clazz;
    return null;
  }

  /**
   * Instances of classes the filter rejects get no injected collections: in a config a null list means "not set".
   */
  public void addCollectionInjectionFilter(Predicate<Class<?>> accepts) {
    collectionInjectionFilters.add(accepts);
  }

  /**
   * The value of a constructor parameter: a bean, or the beans of a collection parameter.
   */
  public Object resolveParameter(Parameter parameter, Class<?> owner) {
    String requiredBy = owner.getName() + " constructor parameter " + parameter.getName();
    boolean playerBean = playerScope.isPlayerBean(owner);
    if (playerBean && isPlayerType(parameter.getType())) return currentPlayer(owner);

    Object handle = scopeHandle(parameter.getType(), parameter.getParameterizedType(), requiredBy, playerBean);
    if (handle != null) return handle;

    CollectionRequest request = CollectionRequest.of(parameter.getType(), parameter.getParameterizedType(), requiredBy);
    if (request != null) {
      Object collection = resolveCollection(request, requiredBy);
      if (collection != null) return collection;
    }

    return createOrGet(parameter.getType());
  }

  private void injectCollections(Object instance) {
    if (!collectionInjectionFilters.stream().allMatch(filter -> filter.test(instance.getClass()))) return;

    for (Field field : ReflectionUtils.getFields(instance.getClass())) {
      if (Modifier.isStatic(field.getModifiers())) continue;

      CollectionRequest request = CollectionRequest.of(field.getType(), field.getGenericType(), describe(field));
      if (request == null) continue;

      try {
        field.setAccessible(true);
        if (field.get(instance) != null) continue;

        Object collection = resolveCollection(request, describe(field));
        if (collection != null) field.set(instance, collection);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot inject " + describe(field), e);
      }
    }
  }

  /**
   * A live view for an extension point, a snapshot of this container's compatible beans for a plugin interface,
   * null for anything else (a List of strings is not ours to fill).
   */
  private @Nullable Object resolveCollection(CollectionRequest request, String requiredBy) {
    Class<?> element = request.element();
    ExtensionRegistry registry = application.getExtensionRegistry();

    if (ExtensionRegistry.isExtensionPoint(element)) {
      if (request.isExtensions()) return registry.view(element, application.getLogger());
      return request.isSet() ? registry.liveSet(element, application.getLogger()) : registry.liveList(element, application.getLogger());
    }

    if (request.isExtensions()) {
      throw new BeanDefinitionException("%s: %s asks for Extensions<%s>, but %s is not annotated with @ExtensionPoint"
                                          .formatted(application.getName(), requiredBy, element.getName(), element.getSimpleName()));
    }
    if (!element.isInterface() && !Modifier.isAbstract(element.getModifiers())) return null;
    if (!isApplicationType(element)) return null;

    Map<Object, Boolean> seen = new IdentityHashMap<>();
    List<Object> found = new ArrayList<>();
    for (Class<?> key : compatibleCandidates(element)) {
      if (playerScope.isPlayerBean(key)) continue;
      Object bean = provide(key);
      if (bean != null && bean != LAZY_OBJECT && seen.put(bean, Boolean.TRUE) == null) found.add(bean);
    }
    found.sort(BeanOrder.COMPARATOR);

    return request.isSet() ? Collections.unmodifiableSet(new LinkedHashSet<>(found)) : List.copyOf(found);
  }

  /**
   * Makes a singleton visible through the extension points it implements; a recreated one replaces {@code old}.
   */
  private void publishExtension(@Nullable Object old, Object bean) {
    ExtensionRegistry registry = application.getExtensionRegistry();
    if (registry.extensionPointsOf(bean.getClass()).isEmpty()) return;

    registry.replace(old, bean);
    application.getLogger().debug("{} published to extension points {}", bean.getClass().getName(), registry.extensionPointsOf(bean.getClass()));
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
    if (value.getScope().equals(BeanScope.PLAYER)
        && (value.isAnnotationPresent(Lazy.class) || clazz.isAnnotationPresent(Scope.class)
            || !application.getExtensionRegistry().extensionPointsOf(clazz).isEmpty())) {
      throw new BeanDefinitionException("%s: @PlayerScoped %s cannot also be @Lazy, carry @Scope or implement an extension point"
                                          .formatted(application.getName(), clazz.getName()));
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
    publishExtension(null, instance);
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
    subtypeCandidates = result.subtypes();
    scanLoader = result.loader();
    application.getLogger().info("Founded {} classes, {} more under conditions", classes.size(), conditionalClasses.size());

    staticFields.clear();
    scanForStaticFields(classes);
  }

  private void registerOwnConditions() {
    for (String definition : conditionDefinitions) {
      Class<?> clazz = scanLoader.apply(definition);
      if (clazz == null) {
        throw new BeanDefinitionException("%s: condition class %s cannot be loaded".formatted(application.getName(), definition));
      }
      MethodCondition.of(application.getName(), clazz).forEach(application.getConditionParser()::registerOwn);
    }
    conditionDefinitions = List.of();
  }

  /**
   * Resolves early the conditional classes of one kind that an extension needs before {@link #resolveConditions()}
   * (config mapper customizers run before any config is read). A class is loaded only when its conditions hold, so
   * foreign types stay safe; loaded classes of the kind join the scanned ones, the rest wait for the normal pass.
   */
  public void resolveConditionsEarly(Predicate<Class<?>> kind) {
    registerOwnConditions();

    List<ScanResult.ConditionalClass> pending = new ArrayList<>();
    List<Class<?>> accepted = new ArrayList<>();
    for (ScanResult.ConditionalClass conditional : conditionalClasses) {
      boolean holds;
      try {
        holds = conditional.conditions().stream().allMatch(application.getConditionParser()::parse);
      } catch (BeanDefinitionException e) {
        throw new BeanDefinitionException("%s (in @Conditional of %s)".formatted(e.getMessage(), conditional.name()));
      }
      Class<?> clazz = holds ? scanLoader.apply(conditional.name()) : null;
      if (clazz != null && kind.test(clazz)) {
        accepted.add(clazz);
      } else {
        pending.add(conditional);
      }
    }

    classes.addAll(accepted);
    scanForStaticFields(accepted);
    conditionalClasses = pending;
  }

  /**
   * Registers the conditions this plugin defines, then loads the conditional classes whose conditions hold.
   * Runs when the container starts: other plugins and the configs are there by then.
   */
  protected void resolveConditions() {
    registerOwnConditions();

    Map<String, Boolean> results = new HashMap<>();
    // unguarded subtypes go first: a property(...) condition below may already read a config
    subtypeCandidates.stream().filter(candidate -> candidate.conditions().isEmpty()).forEach(this::publishSubtype);

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

    for (ScanResult.SubtypeCandidate candidate : subtypeCandidates) {
      if (candidate.conditions().isEmpty()) continue;

      String failed = candidate.conditions().stream()
                               .filter(condition -> !results.computeIfAbsent(condition, application.getConditionParser()::parse))
                               .findFirst()
                               .orElse(null);
      if (failed == null) {
        publishSubtype(candidate);
      } else {
        application.getLogger().info("Config subtype {} skipped: condition '{}' is false", candidate.name(), failed);
      }
    }

    conditionalClasses = List.of();
    conditionDefinitions = List.of();
    subtypeCandidates = List.of();

    if (skipped > 0) {
      application.getLogger().info("{} classes skipped by conditions, {} loaded", skipped, accepted.size());
    }
  }

  private void publishSubtype(ScanResult.SubtypeCandidate candidate) {
    Class<?> type = scanLoader.apply(candidate.name());
    if (type == null) return;

    List<Class<?>> bases = ExtensionRegistry.configSubtypeBasesOf(type);
    if (bases.isEmpty()) {
      application.getLogger().warn("Config subtype {} skipped: no @ConfigSubtypes base among its loaded supertypes", candidate.name());
      return;
    }

    for (Class<?> base : bases) {
      ConfigSubtype subtype = new ConfigSubtype(base, type, candidate.typeName(), application.getName());
      application.getExtensionRegistry().addSubtype(subtype).ifPresentOrElse(
        existing -> application.getLogger().warn("Config subtype '{}' of {}: {} of plugin {} is ignored, the name already belongs to {} of plugin {}",
                                                 subtype.name(), base.getName(), type.getName(), subtype.plugin(),
                                                 existing.type().getName(), existing.plugin()),
        () -> application.getLogger().info("Config subtype '{}' of {}: {}", subtype.name(), base.getSimpleName(), type.getName()));
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
    if (playerScope.isPlayerBean(clazz)) return;
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

      if (!data.isAnnotationPresent(Lazy.class) && data.getFactory() == null) {
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

    T instance = data.getFactory() != null
                 ? clazz.cast(data.getFactory().get())
                 : application.getBeanConstructors().construct(clazz, data);

    if (data.getScope().equals(BeanScope.SINGLETON)) { //todo catch by events
      ((SingletonBeanProvider) getBeanProvider(BeanScope.SINGLETON)).set(clazz, instance);
    }

    populateBeanFields(instance);

    // listeners, tasks and placeholders of a player bean are registered once per class, not per instance
    if (data.getScope().equals(BeanScope.PLAYER)) return instance;

    application.getEventHandler().handleEvent(new BeanConstructedEvent(instance));
    if (data.getScope().equals(BeanScope.SINGLETON)) publishExtension(null, instance);

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
