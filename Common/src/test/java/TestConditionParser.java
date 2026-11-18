import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.condition.Condition;
import ru.cwcode.tkach.minedi.condition.ConditionParser;
import ru.cwcode.tkach.minedi.condition.EnvCondition;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestConditionParser {
  final List<String> calls = new ArrayList<>();

  ConditionParser parser() {
    ConditionParser parser = new ConditionParser();
    parser.setOwner("test");
    parser.register(new Condition() {
      @Override
      public String name() {
        return "is";
      }

      @Override
      public boolean process(String value) {
        calls.add(value);
        return value.equals("true");
      }
    });
    return parser;
  }

  @Test
  public void legacyFormsStillWork() {
    ConditionParser parser = parser();

    assertTrue(parser.parse(""));
    assertTrue(parser.parse(null));
    assertTrue(parser.parse("is(true)"));
    assertFalse(parser.parse("!is(true)"));
    assertTrue(parser.parse("!is(false)"));
  }

  @Test
  public void andBindsTighterThanOr() {
    ConditionParser parser = parser();

    assertTrue(parser.parse("is(true) || is(false) && is(false)"));
    assertFalse(parser.parse("(is(true) || is(false)) && is(false)"));
    assertTrue(parser.parse("!(is(false) || is(false)) && is(true)"));
    assertTrue(parser.parse("  is( true )||is(false)  "));
  }

  @Test
  public void evaluationIsLazy() {
    ConditionParser parser = parser();

    assertFalse(parser.parse("is(false) && is(never1)"));
    assertTrue(parser.parse("is(true) || is(never2)"));
    assertTrue(parser.parse("is(true) || (is(never3) && unknown(x))"));

    assertEquals(List.of("false", "true", "true"), calls);
  }

  @Test
  public void syntaxErrorsNameThePosition() {
    ConditionParser parser = parser();

    for (String broken : List.of("is(true) &&", "is(true", "is true", "(is(true)", "is(true))", "&& is(true)")) {
      BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser.parse(broken), broken);
      assertTrue(error.getMessage().contains("invalid condition '" + broken + "'"), error.getMessage());
    }
  }

  @Test
  public void unknownConditionListsTheKnownOnes() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser().parse("is(true) && nope(1)"));

    assertTrue(error.getMessage().contains("unknown condition nope"), error.getMessage());
    assertTrue(error.getMessage().contains("is (built-in)"), error.getMessage());
  }

  @Test
  public void failingConditionIsReportedWithItsText() {
    ConditionParser parser = parser();
    parser.register(new Condition() {
      @Override
      public String name() {
        return "boom";
      }

      @Override
      public boolean process(String value) {
        throw new IllegalStateException("broken");
      }
    });

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser.parse("boom(x)"));
    assertTrue(error.getMessage().contains("boom(x)") && error.getMessage().contains("broken"), error.getMessage());
  }

  @Test
  public void envMatchesPresenceAndGlob() {
    Map<String, String> environment = Map.of("SERVER_API_CLIENT_NAME", "hub-01", "EMPTY", " ");
    EnvCondition env = new EnvCondition(environment::get);

    assertTrue(env.process("SERVER_API_CLIENT_NAME"));
    assertFalse(env.process("EMPTY"));
    assertFalse(env.process("MISSING"));
    assertTrue(env.process("SERVER_API_CLIENT_NAME=hub-*"));
    assertTrue(env.process("SERVER_API_CLIENT_NAME = hub-01"));
    assertFalse(env.process("SERVER_API_CLIENT_NAME=hub"));
    assertFalse(env.process("SERVER_API_CLIENT_NAME=*.1"));
    assertFalse(env.process("MISSING=*"));
  }
}
