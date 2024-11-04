package ru.cwcode.tkach.minedi;

import lombok.Getter;
import ru.cwcode.tkach.minedi.constructor.BeanConstructorImpl;
import ru.cwcode.tkach.minedi.constructor.BeanConstructors;
import ru.cwcode.tkach.minedi.extension.Extension;
import ru.cwcode.tkach.minedi.processing.EventHandler;
import ru.cwcode.tkach.minedi.processing.EventHandlerImpl;
import ru.cwcode.tkach.minedi.scanner.ClassScanner;
import ru.cwcode.tkach.minedi.scanner.JarClassScanner;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Getter
public class DiApplication {
  private final DiContainer container;
  private final EventHandler eventHandler;
  private final BeanConstructors beanConstructors;
  private final List<Extension> extensions = new LinkedList<>();
  
  public DiApplication(File jarfile, String packageName) {
    this(JarClassScanner.builder()
                        .jar(jarfile)
                        .packageName(packageName)
                        .filter(s -> true)
                        .build());
  }
  
  public DiApplication(ClassScanner scanner) {
    this.eventHandler = new EventHandlerImpl(this);
    this.container = new DiContainer(scanner, this);
    this.beanConstructors = new BeanConstructors(this);
    
    this.beanConstructors.getConstructors().addFirst(new BeanConstructorImpl());
  }
  
  public void registerExtension(Extension extension) {
    extensions.add(extension);
    extension.onRegister(this);
  }
  
  public <T> Optional<T> get(Class<T> clazz) {
    return container.get(clazz);
  }
  
  public <T> void register(T bean, Class<T> as) {
    container.registerSingleton(bean, as);
  }
  
  public void register(Object bean) {
    container.registerSingleton(bean, bean.getClass());
  }
  
  public void start() {
    container.scanClasses();
    container.registerComponents();
    
    extensions.forEach(extension -> extension.onStart(this));
  }
  
}
