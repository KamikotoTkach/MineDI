package isolated.playerscope;

import java.util.UUID;

public record TestPlayer(UUID id, String name) {
  public static TestPlayer of(String name) {
    return new TestPlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
  }
}
