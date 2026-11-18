package ru.cwcode.minediprobe.paper.missing;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.proxy.ProxyServer;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
public class AbsentApiService implements Command {
  ProxyServer server;

  public ProxyServer server(ProxyServer replacement) {
    return replacement == null ? server : replacement;
  }

  public MissingHelper helper() {
    return new MissingHelper(server);
  }
}
