package ru.cwcode.tkach.minedi.extension.paper.processor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;
import ru.cwcode.tkach.minedi.extension.paper.annotation.BukkitService;
import ru.cwcode.tkach.minedi.extension.paper.annotation.BukkitServiceProvider;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.util.LinkedHashMap;
import java.util.Map;

public class BukkitServiceProcessor extends EventProcessor<BeanConstructedEvent> {
  private final PaperExtension extension;

  public BukkitServiceProcessor(PaperExtension extension) {
    super(BeanConstructedEvent.class);
    this.extension = extension;
  }

  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    Object bean = event.getBean();
    Map<Class<?>, ServicePriority> services = findServices(bean.getClass());
    if (services.isEmpty()) return;
    services.keySet().forEach(serviceType -> validateServiceType(serviceType, bean));

    extension.addDelayedTask(() -> services.forEach((serviceType, priority) -> registerService(serviceType, bean, extension.getPlugin(), priority)));
  }

  private Map<Class<?>, ServicePriority> findServices(Class<?> beanClass) {
    Map<Class<?>, ServicePriority> services = new LinkedHashMap<>();

    for (Class<?> current = beanClass; current != null && current != Object.class; current = current.getSuperclass()) {
      addBukkitService(services, current);
      addBukkitServiceProviders(services, current);
      addInterfaceServices(services, current);
    }

    return services;
  }

  private void addBukkitService(Map<Class<?>, ServicePriority> services, Class<?> serviceType) {
    BukkitService annotation = serviceType.getAnnotation(BukkitService.class);
    if (annotation == null) return;

    addService(services, serviceType, annotation.value());
  }

  private void addBukkitServiceProviders(Map<Class<?>, ServicePriority> services, Class<?> providerClass) {
    for (BukkitServiceProvider provider : providerClass.getAnnotationsByType(BukkitServiceProvider.class)) {
      addService(services, provider.value(), provider.priority());
    }
  }

  private void addInterfaceServices(Map<Class<?>, ServicePriority> services, Class<?> type) {
    for (Class<?> serviceInterface : type.getInterfaces()) {
      addInterfaceServices0(services, serviceInterface);
    }
  }

  private void addInterfaceServices0(Map<Class<?>, ServicePriority> services, Class<?> serviceInterface) {
    addBukkitService(services, serviceInterface);

    for (Class<?> parentInterface : serviceInterface.getInterfaces()) {
      addInterfaceServices0(services, parentInterface);
    }
  }

  private void addService(Map<Class<?>, ServicePriority> services, Class<?> serviceType, ServicePriority priority) {
    ServicePriority previousPriority = services.putIfAbsent(serviceType, priority);
    if (previousPriority != null && previousPriority != priority) {
      throw new IllegalStateException("Bukkit service " + serviceType.getName() + " declared with different priorities: " + previousPriority + " and " + priority);
    }
  }

  private void validateServiceType(Class<?> serviceType, Object bean) {
    if (!serviceType.isInstance(bean)) {
      throw new IllegalStateException("Cannot register " + bean.getClass().getName() + " as Bukkit service " + serviceType.getName());
    }
  }

  private static <T> void registerService(Class<T> serviceType, Object bean, Plugin plugin, ServicePriority priority) {
    Bukkit.getServicesManager().register(serviceType, serviceType.cast(bean), plugin, priority);
  }
}
