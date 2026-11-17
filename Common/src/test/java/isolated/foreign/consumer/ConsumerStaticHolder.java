package isolated.foreign.consumer;

import isolated.foreign.owner.OwnerSettings;
import lombok.Getter;

public class ConsumerStaticHolder {
  @Getter
  private static OwnerSettings settings;
}
