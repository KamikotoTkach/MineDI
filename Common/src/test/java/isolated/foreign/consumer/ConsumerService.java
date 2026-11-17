package isolated.foreign.consumer;

import isolated.foreign.owner.*;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class ConsumerService {
  OwnerService ownerService;
  OwnerSettings settings;
  OwnerApi api;
  OwnerDto dto;
  OwnerExcludedService excluded;
}
