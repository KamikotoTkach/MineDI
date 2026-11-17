package isolated.foreign.owner;

import ru.cwcode.tkach.minedi.annotation.Component;

/**
 * Injected only into ConsumerRegisteredObject, so its import happens while singletons are populated.
 */
@Component
public class OwnerAuditService {
}
