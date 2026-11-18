package ru.cwcode.tkach.minedi.condition;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * {@code env(NAME)} — the variable is set and not blank; {@code env(NAME=glob)} — its value matches, {@code *} is any text.
 */
public class EnvCondition implements Condition {
  private final Function<String, String> environment;

  public EnvCondition() {
    this(System::getenv);
  }

  public EnvCondition(Function<String, String> environment) {
    this.environment = environment;
  }

  @Override
  public String name() {
    return "env";
  }

  @Override
  public boolean process(String value) {
    int equals = value.indexOf('=');
    String name = (equals < 0 ? value : value.substring(0, equals)).strip();
    if (name.isEmpty()) throw new IllegalArgumentException("env(NAME) or env(NAME=glob) expected");

    String actual = environment.apply(name);
    if (equals < 0) return actual != null && !actual.isBlank();
    if (actual == null) return false;

    String glob = value.substring(equals + 1).strip();
    String regex = Pattern.quote(glob).replace("*", "\\E.*\\Q");
    return actual.matches(regex);
  }
}
