import beans.condition.EqualsCondition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.DiApplication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConditions {
  static DiApplication application;
  
  @BeforeAll
  public static void init() {
    application = new DiApplication(new TestLogger(), new TestClassScanner("target/test-classes/"));
    application.getConditionParser().register(new EqualsCondition());
    application.start();
  }
  
  @Test
  public void testCondition() {
    boolean equals = application.getConditionParser().parse("equals(a, a)");
    assertTrue(equals);
    
    boolean notEquals = application.getConditionParser().parse("equals(a, b)");
    assertFalse(notEquals);
  }
}
