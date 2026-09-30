package ru.cwcode.minediprobe.velocity;

import com.velocitypowered.api.event.Subscribe;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * A {@code @Service} with a {@code @Subscribe} method; MineDI must register it with Velocity.
 */
@Service
public class VelocityProbeListener {
  int received;

  @Subscribe
  void onProbe(VelocityProbeEvent event) {
    received++;
  }
}
