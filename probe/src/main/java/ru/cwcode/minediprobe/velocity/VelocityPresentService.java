package ru.cwcode.minediprobe.velocity;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * The proxy registers itself as plugin {@code velocity}.
 */
@Conditional("hasPlugin(velocity) && velocityAnswer(42)")
@Service
public class VelocityPresentService {
  static VelocityProbeConfig config;
}
