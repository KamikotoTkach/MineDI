package ru.cwcode.tkach.minedi.extension.paper.processor;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.bukkit.event.Listener;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Async;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Sync;
import ru.cwcode.tkach.minedi.extension.paper.processor.proxy.AsyncMethodInterceptor;
import ru.cwcode.tkach.minedi.extension.paper.processor.proxy.SyncMethodInterceptor;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * Replaces a {@code @Service} that has {@code @Async}/{@code @Sync} methods with a generated subclass.
 * <p>
 * The subclass is defined through a private lookup into the bean's own class, so it lands in the consumer plugin's
 * class loader and package. That needs no {@code --add-opens}: cglib's reflective {@code ClassLoader.defineClass}
 * is refused since Java 17.
 */
public class AsyncAnnotationProcessor extends EventProcessor<BeanCreatedEvent> {
  public AsyncAnnotationProcessor() {
    super(BeanCreatedEvent.class);
  }

  @Override
  public void process(BeanCreatedEvent event, DiApplication application) {
    Class<?> beanClass = event.getBean().getClass();
    if (!beanClass.isAnnotationPresent(Service.class)) return;

    List<Method> proxiedMethods = Arrays.stream(beanClass.getDeclaredMethods())
                                        .filter(x -> x.isAnnotationPresent(Async.class) || x.isAnnotationPresent(Sync.class))
                                        .collect(Collectors.toList());
    if (proxiedMethods.isEmpty()) return;

    if (event.getBean() instanceof Listener) {
      throw new IllegalStateException("Cannot proxy " + beanClass.getName() +
                                      " because Bukkit listener registration scans declared methods of listener class. " +
                                      "Move @Async/@Sync methods to a separate @Service and call it from the listener.");
    }
    validate(beanClass, proxiedMethods);

    Class<?> proxyType = createProxyType(beanClass, application);
    event.setReplacement(instantiate(beanClass, proxyType, application));
  }

  private static void validate(Class<?> beanClass, List<Method> proxiedMethods) {
    if (Modifier.isFinal(beanClass.getModifiers())) {
      throw new IllegalStateException("Cannot proxy final class " + beanClass.getName() + " with @Async/@Sync methods");
    }

    for (Method method : proxiedMethods) {
      int modifiers = method.getModifiers();
      if (Modifier.isPrivate(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
        throw new IllegalStateException("Method " + method + " annotated @Async/@Sync must be overridable (not private, final or static)");
      }
      if (method.isAnnotationPresent(Async.class) && method.getReturnType() != void.class) {
        throw new IllegalStateException("Method " + method + " annotated @Async but has a return value");
      }
    }
  }

  private static Class<?> createProxyType(Class<?> beanClass, DiApplication application) {
    try {
      return new ByteBuddy().subclass(beanClass, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                            .method(isAnnotatedWith(Async.class)).intercept(MethodDelegation.to(new AsyncMethodInterceptor(application)))
                            .method(isAnnotatedWith(Sync.class)).intercept(MethodDelegation.to(new SyncMethodInterceptor(application)))
                            .make()
                            .load(beanClass.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.privateLookupIn(beanClass, MethodHandles.lookup())))
                            .getLoaded();
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot define a proxy next to " + beanClass.getName(), e);
    }
  }

  private static Object instantiate(Class<?> beanClass, Class<?> proxyType, DiApplication application) {
    Constructor<?> beanConstructor = beanClass.getDeclaredConstructors()[0];
    Class<?>[] parameterTypes = beanConstructor.getParameterTypes();
    Object[] parameters = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; i++) {
      parameters[i] = application.get(parameterTypes[i]).orElseThrow();
    }

    try {
      return proxyType.getDeclaredConstructor(parameterTypes).newInstance(parameters);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Cannot instantiate the proxy of " + beanClass.getName(), e);
    }
  }
}
