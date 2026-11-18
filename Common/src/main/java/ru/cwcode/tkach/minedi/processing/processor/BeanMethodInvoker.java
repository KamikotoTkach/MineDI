package ru.cwcode.tkach.minedi.processing.processor;

import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.exception.NoSuchBeanException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calls the {@link Bean} methods of one configuration bean, each once. A parameter is taken from the container; when a
 * sibling method produces its type, that method runs first, whatever the declaration order.
 */
final class BeanMethodInvoker {
  private final DiApplication application;
  private final Object configuration;
  private final List<Method> methods;
  private final Set<Method> done = new HashSet<>();
  private final Set<Method> disabled = new HashSet<>();
  private final LinkedHashSet<Method> running = new LinkedHashSet<>();

  private BeanMethodInvoker(DiApplication application, Object configuration, List<Method> methods) {
    this.application = application;
    this.configuration = configuration;
    this.methods = methods;
  }

  static void invokeAll(DiApplication application, Object configuration) {
    List<Method> methods = new ArrayList<>();
    for (Class<?> clazz = configuration.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Bean.class)) methods.add(method);
      }
    }
    if (methods.isEmpty()) return;

    methods.sort(Comparator.comparing(Method::getName).thenComparing(method -> method.getParameterCount()));

    BeanMethodInvoker invoker = new BeanMethodInvoker(application, configuration, methods);
    methods.forEach(invoker::invoke);
  }

  private void invoke(Method method) {
    if (done.contains(method)) return;

    if (!running.add(method)) {
      String chain = running.stream().map(BeanMethodInvoker::describe).collect(Collectors.joining(" -> "));
      throw new BeanDefinitionException("%s: @Bean methods depend on each other in a cycle: %s -> %s"
                                          .formatted(application.getName(), chain, describe(method)));
    }

    try {
      Bean annotation = method.getAnnotation(Bean.class);
      boolean enabled;
      try {
        enabled = application.getConditionParser().parse(annotation.condition());
      } catch (BeanDefinitionException e) {
        throw new BeanDefinitionException("%s (in @Bean of %s)".formatted(e.getMessage(), describe(method)));
      }

      if (!enabled) {
        disabled.add(method);
        return;
      }

      Class<?>[] types = method.getParameterTypes();
      Object[] arguments = new Object[types.length];
      for (int i = 0; i < types.length; i++) {
        arguments[i] = argument(method, types[i]);
      }

      Object result;
      try {
        method.setAccessible(true);
        result = method.invoke(configuration, arguments);
      } catch (InvocationTargetException e) {
        throw new RuntimeException("@Bean method " + describe(method) + " failed", e.getCause());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Cannot call @Bean method " + describe(method), e);
      }

      if (result != null) {
        application.getContainer().registerSingletonBean(producedType(method), result);
      }
    } finally {
      running.remove(method);
      done.add(method);
    }
  }

  private Object argument(Method method, Class<?> type) {
    Method producer = producerOf(type, method);
    if (producer != null) {
      invoke(producer);
      if (disabled.contains(producer)) {
        throw new BeanDefinitionException("%s: parameter %s of @Bean %s comes from %s, whose condition is false. Put the same condition on %s"
                                            .formatted(application.getName(), type.getName(), describe(method), describe(producer), describe(method)));
      }
    }

    try {
      return application.getContainer().createOrGet(type);
    } catch (NoSuchBeanException e) {
      throw new NoSuchBeanException("%s (parameter of @Bean %s)".formatted(e.getMessage(), describe(method)));
    }
  }

  private Method producerOf(Class<?> type, Method consumer) {
    Method compatible = null;
    for (Method candidate : methods) {
      if (candidate == consumer) continue;

      Class<?> produced = producedType(candidate);
      if (produced == type) return candidate;
      if (compatible == null && type.isAssignableFrom(produced)) compatible = candidate;
    }
    return compatible;
  }

  private static Class<?> producedType(Method method) {
    Bean annotation = method.getAnnotation(Bean.class);
    return annotation.as() == Object.class ? method.getReturnType() : annotation.as();
  }

  private static String describe(Method method) {
    return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()";
  }
}
