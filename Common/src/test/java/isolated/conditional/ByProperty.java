package isolated.conditional;

import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.Service;

@Conditional("property(Settings.enabled) && !property(isolated.conditional.Settings.inner.on)")
@Service
public class ByProperty {
}
