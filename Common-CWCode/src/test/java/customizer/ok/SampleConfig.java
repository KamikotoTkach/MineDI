package customizer.ok;

import ru.cwcode.tkach.config.jackson.yaml.YmlConfig;
import ru.cwcode.tkach.minedi.common.cwcode.config.Config;

@Config
public class SampleConfig extends YmlConfig {
  public static boolean customizedWhenCreated;

  public SampleConfig() {
    customizedWhenCreated = RecordingCustomizer.applied;
  }
}
