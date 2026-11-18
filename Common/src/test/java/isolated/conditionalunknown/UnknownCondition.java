package isolated.conditionalunknown;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

@Conditional("flag(on) && nope(1)")
@Service
public class UnknownCondition {
}
