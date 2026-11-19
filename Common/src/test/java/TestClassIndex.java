import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.DefineCondition;
import ru.cwcode.tkach.minedi.scanner.ClassIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestClassIndex {
  static final int JAVA_25 = 69;

  @TempDir
  Path temp;

  @Test
  public void readsJava25Classes() throws IOException {
    File jar = temp.resolve("plugin.jar").toFile();
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
      write(out, "probe/integration/package-info", "hasPlugin(A)", false);
      write(out, "probe/integration/deep/Outer", "hasPlugin(B)", false);
      write(out, "probe/integration/deep/Outer$1", null, false);
      write(out, "probe/Conditions", null, true);
    }

    ClassIndex index = ClassIndex.ofJar(jar, "probe");

    assertEquals(List.of("hasPlugin(A)", "hasPlugin(B)"), index.conditionsOf("probe.integration.deep.Outer$1"));
    assertEquals(List.of(), index.conditionsOf("probe.Conditions"));
    assertTrue(index.entries().stream().anyMatch(entry -> entry.name().equals("probe.Conditions") && entry.definesConditions()));
  }

  private void write(JarOutputStream out, String internalName, String condition, boolean definesConditions) throws IOException {
    ClassWriter writer = new ClassWriter(0);
    writer.visit(JAVA_25, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);

    if (condition != null) {
      AnnotationVisitor annotation = writer.visitAnnotation(Type.getDescriptor(Conditional.class), true);
      annotation.visit("value", condition);
      annotation.visitEnd();
    }

    if (definesConditions) {
      MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "check", "()Z", null, null);
      method.visitAnnotation(Type.getDescriptor(DefineCondition.class), true).visitEnd();
      method.visitEnd();
    }

    writer.visitEnd();
    out.putNextEntry(new JarEntry(internalName + ".class"));
    out.write(writer.toByteArray());
    out.closeEntry();
  }
}
