package subtype;

import ru.cwcode.tkach.config.relocate.com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("circle")
public class Circle extends Shape {
  public int radius;
}
