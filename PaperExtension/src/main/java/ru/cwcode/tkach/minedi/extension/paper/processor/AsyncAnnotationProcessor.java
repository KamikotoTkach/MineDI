package ru.cwcode.tkach.minedi.extension.paper.processor;

import net.sf.cglib.proxy.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Async;
import ru.cwcode.tkach.minedi.extension.paper.annotation.Sync;
import ru.cwcode.tkach.minedi.processing.event.BeanCreatedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AsyncAnnotationProcessor extends EventProcessor<BeanCreatedEvent> {
  public AsyncAnnotationProcessor() {
    super(BeanCreatedEvent.class);
  }
  
  @Override
  public void process(BeanCreatedEvent event, DiApplication application) {
    if (!event.getBean().getClass().isAnnotationPresent(Service.class)) return;
    
    Set<Method> asyncMethods = Arrays.stream(event.getBean().getClass().getDeclaredMethods())
                                     .filter(x -> x.isAnnotationPresent(Async.class))
                                     .peek(method -> {
                                       if (!method.getReturnType().equals(void.class)) {
                                         throw new IllegalStateException("Method %s annotated @Async but has an return value");
                                       }
                                     })
                                     .collect(Collectors.toSet());
    
    Set<Method> syncMethods = Arrays.stream(event.getBean().getClass().getDeclaredMethods())
                                    .filter(x -> x.isAnnotationPresent(Sync.class))
                                    .collect(Collectors.toSet());
    
    if (asyncMethods.isEmpty() && syncMethods.isEmpty()) return;
    
    Enhancer enhancer = wrapMethods(event.getBean(), asyncMethods, syncMethods, application);
    
    Constructor<?> firstConstructor = event.getBean().getClass().getConstructors()[0];
    if (firstConstructor.getParameterTypes().length == 0) {
      event.setReplacement(enhancer.create());
    } else {
      firstConstructor.setAccessible(true);
      
      Class<?>[] parameterTypes = firstConstructor.getParameterTypes();
      Object[] parameters = new Object[parameterTypes.length];
      
      for (int i = 0; i < parameterTypes.length; i++) {
        Class<?> parameterType = parameterTypes[i];
        parameters[i] = application.get(parameterType).orElseThrow();
      }
      
      event.setReplacement(enhancer.create(parameterTypes, parameters));
    }
  }
  
  private static @NotNull Enhancer wrapMethods(Object object, Set<Method> asyncMethods, Set<Method> syncMethods, DiApplication application) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(object.getClass());
    
    enhancer.setCallbackFilter(method -> {
      if (asyncMethods.contains(method)) return 0;
      if (syncMethods.contains(method)) return 1;
      return 2;
    });
    
    enhancer.setCallbacks(new Callback[]{
      (MethodInterceptor) (o, method, args, methodProxy) -> handleAsyncMethod(application, o, args, methodProxy),
      (MethodInterceptor) (o, method, args, methodProxy) -> handleSyncMethod(application, o, args, methodProxy),
      NoOp.INSTANCE
    });
    
    return enhancer;
  }
  
  private static @Nullable Object handleAsyncMethod(DiApplication application, Object o, Object[] args, MethodProxy methodProxy) throws Throwable {
    if (!Bukkit.isPrimaryThread()) {
      methodProxy.invokeSuper(o, args);
      return null;
    }
    
    JavaPlugin javaPlugin = application.get(JavaPlugin.class).orElseThrow();
    Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> {
      try {
        methodProxy.invokeSuper(o, args);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    });
    
    return null;
  }
  
  private static @Nullable Object handleSyncMethod(DiApplication application, Object o, Object[] args, MethodProxy methodProxy) throws Throwable {
    if (Bukkit.isPrimaryThread()) {
      return methodProxy.invokeSuper(o, args);
    }
    
    JavaPlugin javaPlugin = application.get(JavaPlugin.class).orElseThrow();
    return Bukkit.getScheduler().callSyncMethod(javaPlugin, () -> {
      try {
        return methodProxy.invokeSuper(o, args);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    });
  }
}
