package isolated.beanorder.ok;

import lombok.Getter;

@Getter
public class Consumer {
  private final Producer producer;

  public Consumer(Producer producer) {
    this.producer = producer;
  }
}
