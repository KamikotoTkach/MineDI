package isolated.collections;

import lombok.Getter;
import ru.cwcode.tkach.minedi.annotation.Service;

import java.util.List;

@Getter
@Service
public class FilteredHolder {
  List<Handler> handlers;
}
