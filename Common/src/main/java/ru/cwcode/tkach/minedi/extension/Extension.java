package ru.cwcode.tkach.minedi.extension;

import ru.cwcode.tkach.minedi.DiApplication;

public interface Extension {
  void onRegister(DiApplication application);
  
  void onStart(DiApplication application);
}
