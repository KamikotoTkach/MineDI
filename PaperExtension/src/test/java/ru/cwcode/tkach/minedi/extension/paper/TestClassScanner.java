package ru.cwcode.tkach.minedi.extension.paper;

import lombok.SneakyThrows;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestClassScanner implements ClassScanner {
  Path path;
  
  public TestClassScanner(String classPath) {
    this.path = Path.of(classPath);
  }
  
  Set<Class<?>> loadClassesFromDirectory(File directory) throws IOException, ClassNotFoundException {
    Set<Class<?>> classes = new HashSet<>();
    URL url = directory.toURI().toURL();
    URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          classes.addAll(loadClassesFromDirectory(file));
        } else if (file.getName().endsWith(".class")) {
          String className = file.getPath()
                                 .replace(path + File.separator, "")
                                 .replace(File.separator, ".")
                                 .replace(".class", "");
          classes.add(Class.forName(className, true, classLoader));
        }
      }
    }
    return classes;
  }
  
  @SneakyThrows
  @Override
  public Set<Class<?>> scan() {
    return loadClassesFromDirectory(path.toFile());
  }
}
