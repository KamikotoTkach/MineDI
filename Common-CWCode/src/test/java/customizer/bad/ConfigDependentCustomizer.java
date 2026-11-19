package customizer.bad;

import customizer.ok.SampleConfig;
import ru.cwcode.tkach.config.jackson.yaml.YmlConfigManager;
import ru.cwcode.tkach.minedi.annotation.Service;
import ru.cwcode.tkach.minedi.common.cwcode.config.ConfigMapperCustomizer;

@Service
public class ConfigDependentCustomizer implements ConfigMapperCustomizer {
  public ConfigDependentCustomizer(SampleConfig config) {
  }

  @Override
  public void customize(YmlConfigManager manager) {
  }
}
