package ru.cwcode.tkach.minedi.condition;

/**
 * {@code hasClass(fully.qualified.Name)} — the class is visible to the plugin's class loader; it is not initialized.
 */
public class HasClassCondition implements Condition {
  private final ClassLoader loader;

  public HasClassCondition(ClassLoader loader) {
    this.loader = loader;
  }

  @Override
  public String name() {
    return "hasClass";
  }

  @Override
  public boolean process(String value) {
    try {
      Class.forName(value.strip(), false, loader);
      return true;
    } catch (ClassNotFoundException | LinkageError e) {
      return false;
    }
  }
}
