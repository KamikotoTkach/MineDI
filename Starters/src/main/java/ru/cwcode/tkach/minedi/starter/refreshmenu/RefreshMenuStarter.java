package ru.cwcode.tkach.minedi.starter.refreshmenu;

import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.starter.StarterContext;
import ru.cwcode.tkach.minedi.starter.StarterExtension;
import ru.cwcode.tkach.refreshmenu.MenuManager;
import ru.cwcode.tkach.refreshmenu.RefreshMenu;

/**
 * The consumer's {@link MenuManager} as a bean. A reference, not a bean: its constructor already registers it as a Bukkit
 * listener, a second registration would run every menu handler twice.
 */
@Starter(plugin = "RefreshMenu")
public class RefreshMenuStarter implements StarterExtension {
  @Override
  public void apply(StarterContext context) {
    if (context.getApplication().getContainer().isBean(MenuManager.class)) return;

    context.reference(RefreshMenu.getManager((JavaPlugin) context.getPlatformPlugin()), MenuManager.class);
  }
}
