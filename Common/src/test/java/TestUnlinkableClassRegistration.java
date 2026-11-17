import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.scanner.JarClassScanner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * javac emits a synthetic Outer$1 for a switch over an enum. Its name does not end with "Integration", so the scanner
 * keeps it even when Outer is an excluded integration that implements a type of an absent soft-dependency.
 * Asking such a class for its simple name loads Outer and throws NoClassDefFoundError; registration must skip it.
 */
public class TestUnlinkableClassRegistration {
  @TempDir
  Path temp;

  @Test
  public void syntheticClassOfBrokenOuterIsSkipped() throws IOException {
    File jar = temp.resolve("plugin.jar").toFile();
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
      write(out, outerImplementingMissingType());
      write(out, syntheticSwitchMap());
    }

    try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, getClass().getClassLoader())) {
      DiApplication application = new DiApplication(new TestLogger(), JarClassScanner.builder()
                                                                                    .jar(jar)
                                                                                    .packageName("probe")
                                                                                    .classLoader(loader)
                                                                                    .build());

      assertDoesNotThrow(application::start);
    }
  }

  private ClassFile outerImplementingMissingType() {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "probe/Outer", null, "java/lang/Object", new String[]{"probe/absent/MissingType"});
    writer.visitInnerClass("probe/Outer$1", null, null, Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC);
    writer.visitEnd();
    return new ClassFile("probe/Outer", writer.toByteArray());
  }

  private ClassFile syntheticSwitchMap() {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(Opcodes.V17, Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, "probe/Outer$1", null, "java/lang/Object", null);
    writer.visitOuterClass("probe/Outer", null, null);
    writer.visitInnerClass("probe/Outer$1", null, null, Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC);
    writer.visitEnd();
    return new ClassFile("probe/Outer$1", writer.toByteArray());
  }

  private void write(JarOutputStream out, ClassFile classFile) throws IOException {
    out.putNextEntry(new JarEntry(classFile.internalName() + ".class"));
    out.write(classFile.bytes());
    out.closeEntry();
  }

  private record ClassFile(String internalName, byte[] bytes) {
  }
}
