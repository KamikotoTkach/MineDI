package beans.condition;

import ru.cwcode.tkach.minedi.condition.Condition;

import java.util.Arrays;
import java.util.List;

public class EqualsCondition implements Condition {
  @Override
  public String name() {
    return "equals";
  }
  
  @Override
  public boolean process(String value) {
    List<String> parts = Arrays.stream(value.split(",")).map(String::strip).toList();
    return parts.stream().findFirst().map(x -> parts.stream().allMatch(x::equals)).orElseThrow();
  }
}
