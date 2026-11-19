package isolated.starterconsumer;

import isolated.starter.StarterService;
import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;

@Service
@Getter
public class NeedsService {
  private final StarterService service;

  public NeedsService(StarterService service) {
    this.service = service;
  }
}
