package isolated.foreign.consumer;

import isolated.foreign.owner.OwnerAuditService;
import lombok.Getter;

/**
 * Not a component: registered by hand before start, so its fields are first populated while singletons are iterated.
 */
@Getter
public class ConsumerRegisteredObject {
  OwnerAuditService auditService;
}
