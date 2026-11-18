package ru.cwcode.tkach.minedi.condition;

import lombok.Setter;
import ru.cwcode.tkach.minedi.exception.BeanDefinitionException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Evaluates {@code hasPlugin(X) && !(serverType(hub) || env(MODE=dev))}: {@code ||}, {@code &&}, {@code !} and parentheses,
 * evaluated lazily. A name resolves to a built-in condition, then to one this plugin defines, then to one another plugin
 * defines with {@link ru.cwcode.tkach.minedi.annotation.DefineCondition}.
 */
public class ConditionParser {
  final Map<String, Condition> conditions = new ConcurrentHashMap<>();
  final Map<String, Condition> own = new ConcurrentHashMap<>();
  @Setter
  String owner = "MineDI";
  @Setter
  ConditionRegistry registry = ConditionRegistry.NONE;

  public void register(Condition condition) {
    conditions.put(condition.name(), condition);
  }

  /**
   * A condition this plugin defines; it may not shadow a built-in one or repeat a name.
   */
  public void registerOwn(Condition condition) {
    String name = condition.name();
    if (conditions.containsKey(name)) {
      throw new BeanDefinitionException("%s: condition %s (%s) clashes with the built-in condition of the same name, rename it"
                                          .formatted(owner, name, condition));
    }

    Condition previous = own.putIfAbsent(name, condition);
    if (previous != null) {
      throw new BeanDefinitionException("%s: condition %s is defined twice: %s and %s"
                                          .formatted(owner, name, previous, condition));
    }
  }

  /**
   * @throws BeanDefinitionException on a syntax error, an unknown condition or a condition that cannot evaluate
   */
  public boolean parse(String condition) {
    if (condition == null || condition.isBlank()) return true;

    return new Expression(condition).evaluate();
  }

  Condition resolve(String name, String expression) {
    Condition builtIn = conditions.get(name);
    if (builtIn != null) return builtIn;

    Condition defined = own.get(name);
    if (defined != null) return defined;

    List<MethodCondition> foreign = registry.find(name, owner);
    if (foreign.size() == 1) return foreign.get(0);

    if (foreign.size() > 1) {
      String owners = foreign.stream().map(MethodCondition::toString).collect(Collectors.joining(", "));
      throw new BeanDefinitionException("%s: condition %s in '%s' is defined by several plugins: %s. Rename one of them"
                                          .formatted(owner, name, expression, owners));
    }

    Map<String, String> available = new TreeMap<>();
    conditions.keySet().forEach(known -> available.put(known, "built-in"));
    own.keySet().forEach(known -> available.put(known, owner));
    registry.available(owner).forEach(available::putIfAbsent);

    String list = available.entrySet().stream()
                           .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                           .collect(Collectors.joining(", "));
    String problems = registry.problems().isEmpty() ? "" : ". Plugins whose conditions failed to load: " + String.join("; ", registry.problems());

    throw new BeanDefinitionException("%s: unknown condition %s in '%s'. Available: %s%s"
                                        .formatted(owner, name, expression, list, problems));
  }

  public Collection<String> builtInNames() {
    return conditions.keySet();
  }

  private final class Expression {
    final String text;
    int position;

    Expression(String text) {
      this.text = text;
    }

    boolean evaluate() {
      boolean result = or(true);
      skipSpaces();
      if (position < text.length()) throw error("unexpected '" + text.charAt(position) + "'");
      return result;
    }

    boolean or(boolean evaluate) {
      boolean result = and(evaluate);
      while (accept("||")) {
        boolean right = and(evaluate && !result);
        result = result || right;
      }
      return result;
    }

    boolean and(boolean evaluate) {
      boolean result = unary(evaluate);
      while (accept("&&")) {
        boolean right = unary(evaluate && result);
        result = result && right;
      }
      return result;
    }

    boolean unary(boolean evaluate) {
      if (accept("!")) return !unary(evaluate);

      if (accept("(")) {
        boolean result = or(evaluate);
        if (!accept(")")) throw error("')' expected");
        return result;
      }

      return atom(evaluate);
    }

    boolean atom(boolean evaluate) {
      skipSpaces();
      int start = position;
      while (position < text.length() && isNameChar(text.charAt(position))) position++;
      if (start == position) throw error("condition name expected");

      String name = text.substring(start, position);
      if (!accept("(")) throw error("'(' expected after " + name);

      int close = text.indexOf(')', position);
      if (close < 0) throw error("')' expected");

      String arguments = text.substring(position, close).strip();
      position = close + 1;

      if (!evaluate) return false;

      Condition condition = resolve(name, text);
      try {
        return condition.process(arguments);
      } catch (BeanDefinitionException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new BeanDefinitionException("%s: condition %s(%s) in '%s' failed: %s".formatted(owner, name, arguments, text, e));
      }
    }

    boolean accept(String token) {
      skipSpaces();
      if (!text.startsWith(token, position)) return false;
      position += token.length();
      return true;
    }

    void skipSpaces() {
      while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
    }

    BeanDefinitionException error(String problem) {
      return new BeanDefinitionException("%s: invalid condition '%s' at position %d: %s".formatted(owner, text, position, problem));
    }

    static boolean isNameChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-';
    }
  }
}
