package ru.cwcode.tkach.minedi.condition;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ConditionParser {
  Map<String, Condition> conditions = new HashMap<>();
  
  public void register(Condition condition) {
    conditions.put(condition.name(), condition);
  }
  
  public boolean parse(String condition) {
    if (condition == null || condition.isEmpty()) return true;
    
    boolean inverse = condition.startsWith("!");
    int argStarts = condition.indexOf("(");
    int argEnds = condition.indexOf(")");
    
    String conditionName = condition.substring(inverse ? 1 : 0, argStarts);
    Condition conditionInstance = conditions.get(conditionName);
    if (conditionInstance == null) throw new NoSuchElementException("Condition with name %s not found. (%s)".formatted(conditionName, condition));
    
    String args = condition.substring(argStarts + 1, argEnds);
    
    return conditionInstance.process(args) ^ inverse;
  }
}
