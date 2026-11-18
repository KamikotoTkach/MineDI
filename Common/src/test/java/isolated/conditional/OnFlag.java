package isolated.conditional;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

@Conditional("flag(on)")
@Service
public class OnFlag {
}
