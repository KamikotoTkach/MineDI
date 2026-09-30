package ru.cwcode.minediprobe.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.slf4j.Logger;
import ru.cwcode.tkach.minedi.extension.velocity.VelocityPlatform;

import java.nio.file.Path;

@Plugin(id = "minediprobe", name = "MineDIProbe", version = "1", dependencies = @Dependency(id = "minedi"))
@Getter
public class MineDIProbeVelocity extends VelocityPlatform {
  @Inject
  private Logger logger;

  @Inject
  private ProxyServer server;

  @Inject
  @DataDirectory
  private Path dataDirectory;
}
