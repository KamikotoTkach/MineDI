package isolated.beanorder.ok;

import ru.cwcode.tkach.minedi.annotation.Bean;
import ru.cwcode.tkach.minedi.annotation.Service;

/**
 * Methods run sorted by name unless a parameter needs a sibling: aConsumer sorts first but needs bProducer.
 */
@Service
public class OrderConfiguration {
  public static int markedCalls;

  @Bean
  public Consumer aConsumer(Producer producer, OrderConfiguration self) {
    return new Consumer(producer);
  }

  @Bean
  public Producer bProducer() {
    return new Producer();
  }

  @Bean
  public MadeByBean madeByBean() {
    return new MadeByBean();
  }

  @Marker
  @Bean
  public Marked marked() {
    markedCalls++;
    return new Marked();
  }

  @Bean(condition = "!hasClass(java.lang.String)")
  public Skipped skipped() {
    return new Skipped();
  }
}
