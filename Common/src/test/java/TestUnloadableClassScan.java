import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import ru.cwcode.tkach.minedi.utils.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A plugin may shade a library whose Java 25 classes (major 69) land in the scanned package and cannot be defined on an
 * older JVM. The scan must skip them instead of failing the whole plugin.
 */
public class TestUnloadableClassScan {
  private static final int UNSUPPORTED_MAJOR = 99;

  @TempDir
  Path temp;

  @Test
  public void classOfUnsupportedVersionIsSkipped() throws IOException {
    File jar = temp.resolve("plugin.jar").toFile();
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
      write(out, "probe/Loadable", Opcodes.V1_8);
      write(out, "probe/compat/v26/FromTheFuture", UNSUPPORTED_MAJOR);
    }

    try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, getClass().getClassLoader())) {
      Set<Class<?>> classes = ReflectionUtils.getClasses(jar, "probe", null, loader);

      assertEquals(Set.of("probe.Loadable"), classes.stream().map(Class::getName).collect(Collectors.toSet()));
    }
  }

  private void write(JarOutputStream out, String internalName, int version) throws IOException {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(version, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);
    writer.visitEnd();

    out.putNextEntry(new JarEntry(internalName + ".class"));
    out.write(writer.toByteArray());
    out.closeEntry();
  }
}
