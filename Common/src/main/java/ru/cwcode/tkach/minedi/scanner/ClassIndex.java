package ru.cwcode.tkach.minedi.scanner;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.DefineCondition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * What the scanner needs to know before loading a class, read from bytecode: its {@link Conditional} and whether it
 * declares {@link DefineCondition} methods.
 */
public final class ClassIndex {
  static final String PACKAGE_INFO = "package-info";
  private static final String CONDITIONAL = Type.getDescriptor(Conditional.class);
  private static final String DEFINE_CONDITION = Type.getDescriptor(DefineCondition.class);

  private final Map<String, Entry> entries;

  private ClassIndex(Map<String, Entry> entries) {
    this.entries = entries;
  }

  public static ClassIndex ofJar(File jar, String packageName) {
    Map<String, Entry> entries = new LinkedHashMap<>();
    String prefix = packageName.replace('.', '/');

    try (JarFile file = new JarFile(jar)) {
      for (Enumeration<JarEntry> enumeration = file.entries(); enumeration.hasMoreElements(); ) {
        JarEntry jarEntry = enumeration.nextElement();
        String path = jarEntry.getName();
        if (!path.startsWith(prefix) || !path.endsWith(".class") || path.startsWith("META-INF/")) continue;

        try (InputStream in = file.getInputStream(jarEntry)) {
          Entry entry = read(in.readAllBytes());
          if (entry != null) entries.put(entry.name(), entry);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read classes of " + jar, e);
    }

    return new ClassIndex(entries);
  }

  public Collection<Entry> entries() {
    return Collections.unmodifiableCollection(entries.values());
  }

  /**
   * Conditions guarding the class: those of its package and every enclosing package, of its outer classes and its own.
   * All of them must hold.
   */
  public List<String> conditionsOf(String className) {
    List<String> conditions = new ArrayList<>();

    String packageName = packageOf(className);
    List<String> packages = new ArrayList<>();
    for (String current = packageName; !current.isEmpty(); current = packageOf(current)) {
      packages.add(0, current);
    }
    for (String current : packages) {
      addCondition(conditions, current + "." + PACKAGE_INFO);
    }

    int nested = className.indexOf('$');
    while (nested > 0) {
      addCondition(conditions, className.substring(0, nested));
      nested = className.indexOf('$', nested + 1);
    }

    addCondition(conditions, className);
    return conditions;
  }

  public static boolean isPackageInfo(String className) {
    return className.endsWith("." + PACKAGE_INFO) || className.equals(PACKAGE_INFO);
  }

  private void addCondition(List<String> conditions, String className) {
    Entry entry = entries.get(className);
    if (entry != null && entry.condition() != null) conditions.add(entry.condition());
  }

  private static String packageOf(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(0, dot);
  }

  static @Nullable Entry read(byte[] bytes) {
    if (bytes.length < 8) return null;

    ClassReader reader;
    try {
      reader = new ClassReader(withReadableVersion(bytes));
    } catch (IllegalArgumentException e) {
      return null; // a class file version ASM does not know: the regular loader reports it
    }

    EntryVisitor visitor = new EntryVisitor();
    try {
      reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return new Entry(visitor.name.replace('/', '.'), visitor.condition, visitor.definesConditions);
  }

  /**
   * ASM refuses class files newer than it knows (plugins are built for Java 25, our ASM is older). Only the header,
   * annotations and method declarations are read here, whose format has not changed, so a copy claims Java 17.
   */
  private static byte[] withReadableVersion(byte[] bytes) {
    int major = ((bytes[6] & 0xFF) << 8) | (bytes[7] & 0xFF);
    if (major <= Opcodes.V17) return bytes;

    byte[] copy = bytes.clone();
    copy[6] = 0;
    copy[7] = (byte) Opcodes.V17;
    return copy;
  }

  public record Entry(String name, @Nullable String condition, boolean definesConditions) {
  }

  private static final class EntryVisitor extends ClassVisitor {
    String name;
    String condition;
    boolean definesConditions;

    EntryVisitor() {
      super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.name = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (!CONDITIONAL.equals(descriptor)) return null;

      return new AnnotationVisitor(Opcodes.ASM9) {
        @Override
        public void visit(String name, Object value) {
          if ("value".equals(name)) condition = (String) value;
        }
      };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      if (definesConditions) return null;

      return new MethodVisitor(Opcodes.ASM9) {
        @Override
        public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
          if (DEFINE_CONDITION.equals(annotation)) definesConditions = true;
          return null;
        }
      };
    }
  }
}
