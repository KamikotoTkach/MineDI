package ru.cwcode.tkach.minedi;

import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.scanner.JarClassScanner;

import java.io.File;
import java.util.Optional;

public class DiApplication {
  private final DiContainer container;
  
  public DiApplication(File jarfile, String packageName) {
    this(JarClassScanner.builder()
                        .jar(jarfile)
                        .packageName(packageName)
                        .filter(s -> true)
                        .build());
  }
  
  public DiApplication(ClassScanner scanner) {
    this.container = new DiContainer(scanner);
    start();
  }
  
  public <T> Optional<T> get(Class<T> clazz) {
    return container.get(clazz);
  }
  
  private void start() {
    container.scanClasses();
    container.registerComponents();
  }
}
