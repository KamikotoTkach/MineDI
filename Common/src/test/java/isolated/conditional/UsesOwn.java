package isolated.conditional;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

@Conditional("answer(42) && !answer(1) && allOf(yes, yes)")
@Service
public class UsesOwn {
}
