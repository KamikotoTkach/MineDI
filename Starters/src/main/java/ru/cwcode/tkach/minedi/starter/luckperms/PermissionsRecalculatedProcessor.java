package ru.cwcode.tkach.minedi.starter.luckperms;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;
import ru.cwcode.tkach.minedi.processing.event.BeanConstructedEvent;
import ru.cwcode.tkach.minedi.processing.processor.EventProcessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects {@link PermissionsRecalculated} methods of the consumer's beans behind one LuckPerms subscription. The event is
 * async: only the UUID leaves the callback, the methods run on the main thread for a player still online.
 */
public class PermissionsRecalculatedProcessor extends EventProcessor<BeanConstructedEvent> {
  private final Plugin plugin;
  private final List<Target> targets = new CopyOnWriteArrayList<>();
  private DiApplication application;
  private boolean subscribed;

  public PermissionsRecalculatedProcessor(Plugin plugin) {
    super(BeanConstructedEvent.class);
    this.plugin = plugin;
  }

  @Override
  public void process(BeanConstructedEvent event, DiApplication application) {
    Object bean = event.getBean();

    for (Class<?> clazz = bean.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(PermissionsRecalculated.class)) continue;

        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != Player.class) {
          throw new BeanDefinitionException("%s: @PermissionsRecalculated %s#%s must take one Player parameter"
                                              .formatted(application.getName(), clazz.getName(), method.getName()));
        }
        method.setAccessible(true);
        targets.add(new Target(bean, method));
        subscribe(application);
      }
    }
  }

  private synchronized void subscribe(DiApplication application) {
    if (subscribed) return;
    subscribed = true;
    this.application = application;

    LuckPermsProvider.get().getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
      UUID uuid = event.getUser().getUniqueId();
      if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> dispatch(uuid));
    });
  }

  private void dispatch(UUID uuid) {
    Player player = Bukkit.getPlayer(uuid);
    if (player == null) return;

    for (Target target : targets) {
      try {
        target.method().invoke(target.bean(), player);
      } catch (InvocationTargetException e) {
        application.getLogger().warn("{} failed for {}", target, player.getName(), e.getCause());
      } catch (IllegalAccessException e) {
        application.getLogger().warn("{} is not accessible", target, e);
      }
    }
  }

  private record Target(Object bean, Method method) {
    @Override
    public String toString() {
      return method.getDeclaringClass().getName() + "#" + method.getName();
    }
  }
}
