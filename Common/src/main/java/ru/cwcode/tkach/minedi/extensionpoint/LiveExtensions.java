package ru.cwcode.tkach.minedi.extensionpoint;

import ru.cwcode.tkach.minedi.logging.Log;

import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The view one plugin holds on an extension point; listeners report their failures to that plugin's log.
 */
final class LiveExtensions<T> implements Extensions<T> {
  private final Class<T> type;
  private final ExtensionRegistry registry;
  private final Log log;
  private final List<Consumer<? super T>> addListeners = new CopyOnWriteArrayList<>();
  private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
  private volatile List<T> snapshot;

  LiveExtensions(Class<T> type, ExtensionRegistry registry, Log log) {
    this.type = type;
    this.registry = registry;
    this.log = log;
    this.snapshot = collect();
  }

  @Override
  public Class<T> type() {
    return type;
  }

  @Override
  public List<T> list() {
    return snapshot;
  }

  @Override
  public void onAdd(Consumer<? super T> listener) {
    synchronized (registry) {
      addListeners.add(listener);
      snapshot.forEach(bean -> notifyAdd(listener, bean));
    }
  }

  @Override
  public void onChange(Runnable listener) {
    changeListeners.add(listener);
  }

  List<T> asList() {
    return new AbstractList<>() {
      @Override
      public T get(int index) {
        return snapshot.get(index);
      }

      @Override
      public int size() {
        return snapshot.size();
      }
    };
  }

  Set<T> asSet() {
    return new AbstractSet<>() {
      @Override
      public Iterator<T> iterator() {
        return snapshot.iterator();
      }

      @Override
      public int size() {
        return snapshot.size();
      }
    };
  }

  boolean accepts(Object bean) {
    return type.isInstance(bean);
  }

  void added(Object bean) {
    snapshot = collect();
    addListeners.forEach(listener -> notifyAdd(listener, type.cast(bean)));
    changed();
  }

  void removed() {
    snapshot = collect();
    changed();
  }

  private void changed() {
    for (Runnable listener : changeListeners) {
      try {
        listener.run();
      } catch (RuntimeException e) {
        log.warn("Extension point {}: change listener failed", type.getName(), e);
      }
    }
  }

  private void notifyAdd(Consumer<? super T> listener, T bean) {
    try {
      listener.accept(bean);
    } catch (RuntimeException e) {
      log.warn("Extension point {}: listener failed on {}", type.getName(), bean.getClass().getName(), e);
    }
  }

  private List<T> collect() {
    return registry.published()
                   .stream()
                   .filter(type::isInstance)
                   .sorted(BeanOrder.COMPARATOR)
                   .map(type::cast)
                   .toList();
  }
}
