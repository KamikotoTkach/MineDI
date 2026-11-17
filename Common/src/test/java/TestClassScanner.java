import lombok.SneakyThrows;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tests that need their own set of beans live in packages under {@value #ISOLATED_PACKAGE}: the shared scanner skips them,
 * a scanner built with a package prefix sees only that package.
 */
public class TestClassScanner implements ClassScanner {
  static final String ISOLATED_PACKAGE = "isolated.";

  Path path;
  List<Predicate<String>> classNameFilters = new ArrayList<>();

  public TestClassScanner(String classPath) {
    this.path = Path.of(classPath);
    addClassNameFilter(className -> !className.startsWith(ISOLATED_PACKAGE));
  }

  public TestClassScanner(String classPath, String packagePrefix) {
    this.path = Path.of(classPath);
    addClassNameFilter(className -> className.startsWith(packagePrefix));
  }

  @Override
  public void addClassNameFilter(Predicate<String> filter) {
    classNameFilters.add(filter);
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
          if (classNameFilters.stream().allMatch(filter -> filter.test(className))) {
            classes.add(Class.forName(className, true, classLoader));
          }
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
