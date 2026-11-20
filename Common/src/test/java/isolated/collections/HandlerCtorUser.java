package isolated.collections;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;

import java.util.List;

@Getter
@Service
public class HandlerCtorUser {
  private final List<Handler> handlers;

  public HandlerCtorUser(List<Handler> handlers) {
    this.handlers = handlers;
  }
}
