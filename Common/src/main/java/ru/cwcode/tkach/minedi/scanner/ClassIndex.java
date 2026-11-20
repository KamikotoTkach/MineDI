package ru.cwcode.tkach.minedi.scanner;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import ru.cwcode.tkach.minedi.annotation.ConfigSubtypes;
import ru.cwcode.tkach.minedi.annotation.Conditional;
import ru.cwcode.tkach.minedi.annotation.DefineCondition;
import ru.cwcode.tkach.minedi.annotation.Starter;
import ru.cwcode.tkach.minedi.annotation.Starters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * What the scanner needs to know before loading a class, read from bytecode: its {@link Conditional}, whether it
 * declares {@link DefineCondition} methods and which plugins it is a {@link Starter} for.
 */
public final class ClassIndex {
  static final String PACKAGE_INFO = "package-info";
  private static final String CONDITIONAL = Type.getDescriptor(Conditional.class);
  private static final String DEFINE_CONDITION = Type.getDescriptor(DefineCondition.class);
  private static final String STARTER = Type.getDescriptor(Starter.class);
  private static final String STARTERS = Type.getDescriptor(Starters.class);
  private static final String CONFIG_SUBTYPES = Type.getDescriptor(ConfigSubtypes.class);
  private static final String JSON_TYPE_NAME = "/jackson/annotation/JsonTypeName;";

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

  /**
   * A starter and the classes nested in it are loaded only for a consumer that depends on its plugin, never by a scan.
   */
  public boolean isStarterOrNested(String className) {
    if (isStarter(className)) return true;

    int nested = className.indexOf('$');
    while (nested > 0) {
      if (isStarter(className.substring(0, nested))) return true;
      nested = className.indexOf('$', nested + 1);
    }
    return false;
  }

  private boolean isStarter(String className) {
    Entry entry = entries.get(className);
    return entry != null && !entry.starterPlugins().isEmpty();
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
      reader = new ClassReader(bytes);
    } catch (IllegalArgumentException e) {
      return null; // a class file version ASM does not know: the regular loader reports it
    }

    EntryVisitor visitor = new EntryVisitor();
    try {
      reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    } catch (IllegalArgumentException e) {
      return null;
    }
    return new Entry(visitor.name.replace('/', '.'), visitor.condition, visitor.definesConditions, List.copyOf(visitor.starterPlugins),
                     visitor.supertypes, visitor.concrete, visitor.configSubtypesBase, visitor.typeName);
  }

  /**
   * Whether a concrete class with {@code @JsonTypeName} extends a {@link ConfigSubtypes} base. Supertypes in this jar
   * are followed through the index, the first one outside it is asked through {@code outside}.
   */
  public boolean extendsConfigSubtypesBase(String className, Predicate<String> outside) {
    Set<String> seen = new HashSet<>();
    Deque<String> queue = new ArrayDeque<>();
    Entry start = entries.get(className);
    if (start == null) return false;
    queue.addAll(start.supertypes());

    while (!queue.isEmpty()) {
      String current = queue.poll();
      if (!seen.add(current)) continue;

      Entry entry = entries.get(current);
      if (entry == null) {
        if (outside.test(current)) return true;
        continue;
      }
      if (entry.configSubtypesBase()) return true;
      queue.addAll(entry.supertypes());
    }
    return false;
  }

  /**
   * @param starterPlugins     values of {@link Starter#plugin()} on the class, empty when it is not a starter
   * @param supertypes         the superclass (unless {@code Object}) and the interfaces
   * @param concrete           neither an interface nor abstract
   * @param configSubtypesBase annotated with {@link ConfigSubtypes}
   * @param typeName           the value of a Jackson {@code @JsonTypeName}, in any (relocated) package
   */
  public record Entry(String name, @Nullable String condition, boolean definesConditions, List<String> starterPlugins,
                      List<String> supertypes, boolean concrete, boolean configSubtypesBase, @Nullable String typeName) {
  }

  private static final class EntryVisitor extends ClassVisitor {
    String name;
    String condition;
    boolean definesConditions;
    final List<String> starterPlugins = new ArrayList<>();
    List<String> supertypes = List.of();
    boolean concrete;
    boolean configSubtypesBase;
    String typeName;

    EntryVisitor() {
      super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.name = name;
      this.concrete = (access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) == 0;

      List<String> found = new ArrayList<>();
      if (superName != null && !superName.equals("java/lang/Object")) found.add(superName.replace('/', '.'));
      if (interfaces != null) {
        for (String implemented : interfaces) found.add(implemented.replace('/', '.'));
      }
      this.supertypes = List.copyOf(found);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (CONFIG_SUBTYPES.equals(descriptor)) {
        configSubtypesBase = true;
        return null;
      }
      if (descriptor.endsWith(JSON_TYPE_NAME)) {
        return new AnnotationVisitor(Opcodes.ASM9) {
          @Override
          public void visit(String name, Object value) {
            if ("value".equals(name) && value instanceof String string && !string.isEmpty()) typeName = string;
          }
        };
      }
      if (STARTER.equals(descriptor)) return starterVisitor();
      if (STARTERS.equals(descriptor)) {
        return new AnnotationVisitor(Opcodes.ASM9) {
          @Override
          public AnnotationVisitor visitArray(String name) {
            return new AnnotationVisitor(Opcodes.ASM9) {
              @Override
              public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                return STARTER.equals(descriptor) ? starterVisitor() : null;
              }
            };
          }
        };
      }
      if (!CONDITIONAL.equals(descriptor)) return null;

      return new AnnotationVisitor(Opcodes.ASM9) {
        @Override
        public void visit(String name, Object value) {
          if ("value".equals(name)) condition = (String) value;
        }
      };
    }

    private AnnotationVisitor starterVisitor() {
      return new AnnotationVisitor(Opcodes.ASM9) {
        @Override
        public void visit(String name, Object value) {
          if ("plugin".equals(name)) starterPlugins.add((String) value);
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
