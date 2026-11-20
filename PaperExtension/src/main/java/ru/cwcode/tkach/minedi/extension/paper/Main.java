package ru.cwcode.tkach.minedi.extension.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cwcode.tkach.minedi.extension.paper.scope.PlayerWindowListener;
import ru.cwcode.tkach.minedi.scope.PlayerWindows;

public class Main extends JavaPlugin {
  @Override
  public void onLoad() {
    PlayerWindows.GLOBAL.setKeyType(Player.class, Player::getUniqueId);
  }

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(new PlayerWindowListener(PlayerWindows.GLOBAL), this);

    for (Player player : Bukkit.getOnlinePlayers()) {
      PlayerWindows.GLOBAL.open(player);
      PlayerWindows.GLOBAL.joined(player.getUniqueId());
    }
  }
}
