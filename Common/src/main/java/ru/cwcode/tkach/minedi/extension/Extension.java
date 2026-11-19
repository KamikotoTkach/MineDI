package ru.cwcode.tkach.minedi.extension;

import ru.cwcode.tkach.minedi.DiApplication;

public interface Extension {
  void onRegister(DiApplication application);

  /**
   * Before conditions are resolved and beans are created: the last moment to change what beans will see.
   */
  default void onBeforeStart(DiApplication application) {
  }
  
  void onStart(DiApplication application);
}
