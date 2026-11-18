import isolated.conditionsource.ArgConditions;
import isolated.conditionsource.ForeignConditions;
import isolated.conditionsource.InstanceConditions;
import org.junit.jupiter.api.Test;
import ru.cwcode.tkach.minedi.condition.ConditionParser;
import ru.cwcode.tkach.minedi.condition.ConditionRegistry;
import ru.cwcode.tkach.minedi.condition.ConditionSource;
import ru.cwcode.tkach.minedi.condition.MethodCondition;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDefineCondition {
  static ConditionSource source(String owner, Class<?>... classes) {
    return new ConditionSource() {
      @Override
      public String owner() {
        return owner;
      }

      @Override
      public List<Class<?>> definitionClasses() {
        return List.of(classes);
      }
    };
  }

  ConditionParser parser(String owner, ConditionRegistry registry) {
    ConditionParser parser = new ConditionParser();
    parser.setOwner(owner);
    parser.setRegistry(registry);
    return parser;
  }

  ConditionParser withArgs() {
    ConditionParser parser = parser("me", ConditionRegistry.NONE);
    MethodCondition.of("me", ArgConditions.class).forEach(parser::registerOwn);
    return parser;
  }

  @Test
  public void convertsArgumentsToParameterTypes() {
    ConditionParser parser = withArgs();

    assertTrue(parser.parse("numbers(1, 10000000000, 0.5, TRUE)"));
    assertFalse(parser.parse("numbers(2, 10000000000, 0.5, true)"));
    assertTrue(parser.parse("mode(creative)"));
    assertFalse(parser.parse("mode(Survival)"));
    assertTrue(parser.parse("type(java.lang.String)"));
    assertTrue(parser.parse("whole(a, b, c)"));
    assertTrue(parser.parse("none()"));
    assertTrue(parser.parse("renamed(x)"));
  }

  @Test
  public void wrongArgumentsNameTheMethod() {
    ConditionParser parser = withArgs();

    for (String wrong : List.of("numbers(1, 2)", "numbers(x, 1, 1, true)", "numbers(1, 1, 1, yes)", "mode(adventure)",
                                "type(no.such.Type)", "none(1)")) {
      BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser.parse(wrong), wrong);
      assertTrue(error.getMessage().contains(ArgConditions.class.getName()), error.getMessage());
    }
  }

  @Test
  public void nullAndExceptionsAreErrors() {
    ConditionParser parser = withArgs();

    assertTrue(assertThrows(BeanDefinitionException.class, () -> parser.parse("renamed()")).getMessage().contains("returned null"));
    assertTrue(assertThrows(BeanDefinitionException.class, () -> parser.parse("throwing()")).getMessage().contains("boom"));
  }

  @Test
  public void instanceMethodIsRejected() {
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> MethodCondition.of("me", InstanceConditions.class));
    assertTrue(error.getMessage().contains("must be static"), error.getMessage());
  }

  @Test
  public void ownNameMayNotRepeatOrShadowBuiltIn() {
    ConditionParser parser = withArgs();

    assertThrows(BeanDefinitionException.class, () -> MethodCondition.of("me", ArgConditions.class).forEach(parser::registerOwn));

    ConditionParser withBuiltIn = parser("me", ConditionRegistry.NONE);
    MethodCondition.of("me", ForeignConditions.class).forEach(withBuiltIn::register);
    BeanDefinitionException error = assertThrows(BeanDefinitionException.class,
                                                 () -> MethodCondition.of("me", ForeignConditions.class).forEach(withBuiltIn::registerOwn));
    assertTrue(error.getMessage().contains("built-in"), error.getMessage());
  }

  @Test
  public void conditionOfAnotherPluginIsFoundWithoutDepend() {
    ConditionRegistry registry = new ConditionRegistry();
    ConditionParser parser = parser("consumer", registry);

    // the owner registers after the consumer's parser exists, as a plugin that starts later
    registry.addSource(source("commons", ForeignConditions.class));

    assertTrue(parser.parse("serverType(hub)"));
    assertFalse(parser.parse("serverType(resource)"));
  }

  @Test
  public void ownSourceIsNotTakenFromTheRegistry() {
    ConditionRegistry registry = new ConditionRegistry();
    registry.addSource(source("commons", ForeignConditions.class));

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser("commons", registry).parse("serverType(hub)"));
    assertTrue(error.getMessage().contains("unknown condition serverType"), error.getMessage());
  }

  @Test
  public void sameNameInTwoPluginsIsAnError() {
    ConditionRegistry registry = new ConditionRegistry();
    registry.addSource(source("commons", ForeignConditions.class));
    registry.addSource(source("other", ForeignConditions.class));

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser("consumer", registry).parse("serverType(hub)"));
    assertTrue(error.getMessage().contains("commons") && error.getMessage().contains("other"), error.getMessage());
  }

  @Test
  public void brokenSourceDoesNotHideOthersAndIsReported() {
    ConditionRegistry registry = new ConditionRegistry();
    registry.addSource(source("broken", InstanceConditions.class));
    registry.addSource(source("commons", ForeignConditions.class));
    ConditionParser parser = parser("consumer", registry);

    assertTrue(parser.parse("serverType(hub)"));

    BeanDefinitionException error = assertThrows(BeanDefinitionException.class, () -> parser.parse("notStatic()"));
    assertTrue(error.getMessage().contains("unknown condition notStatic"), error.getMessage());
    assertTrue(error.getMessage().contains("must be static"), error.getMessage());
    assertTrue(error.getMessage().contains("serverType (commons)"), error.getMessage());
  }
}
