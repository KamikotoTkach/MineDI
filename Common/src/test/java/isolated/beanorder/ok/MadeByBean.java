package isolated.beanorder.ok;

/**
 * Excluded from the scan in the test, like an *Integration: only the @Bean method creates it.
 */
public class MadeByBean {
  public static Producer producer;
  public static Consumer consumer;
}
