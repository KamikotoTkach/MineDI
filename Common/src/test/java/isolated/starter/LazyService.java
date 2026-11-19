package isolated.starter;

public class LazyService {
  public static int created;

  public LazyService() {
    created++;
  }
}
