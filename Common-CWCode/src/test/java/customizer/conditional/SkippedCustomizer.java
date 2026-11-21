package customizer.conditional;

import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigMapperCustomizer;

@Service
@Conditional("!hasClass(java.lang.String)")
public class SkippedCustomizer implements ConfigMapperCustomizer {
  public static boolean applied;

  @Override
  public void customize(YmlConfigManager manager) {
    applied = true;
  }
}
