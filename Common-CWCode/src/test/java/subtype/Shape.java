package subtype;

import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.cwcode.tkach.minedi.annotation.ConfigSubtypes;

@ConfigSubtypes
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class Shape {
}
