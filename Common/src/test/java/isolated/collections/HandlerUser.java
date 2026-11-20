package isolated.collections;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Service
public class HandlerUser {
  List<Handler> handlers;
  Set<Handler> handlerSet;
  Collection<? extends Handler> wildcard;
  List<String> strings;
  List<Runnable> runnables;
  List<Handler> preset = List.of();
}
