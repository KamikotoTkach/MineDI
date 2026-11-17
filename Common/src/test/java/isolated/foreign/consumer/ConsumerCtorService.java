package isolated.foreign.consumer;

import isolated.foreign.owner.OwnerService;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Component;

@Getter
@Component
public class ConsumerCtorService {
  private final OwnerService ownerService;
  
  public ConsumerCtorService(OwnerService ownerService) {
    this.ownerService = ownerService;
  }
}
