package ru.cwcode.tkach.minedi.extensionpoint;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Live, ordered implementations of an {@link ru.cwcode.tkach.minedi.annotation.ExtensionPoint} from every MineDI plugin.
 * Implementations of plugins that start later are added as they start, so read it when used or subscribe with
 * {@link #onAdd}: iterating it in a constructor sees only what has started so far.
 */
public interface Extensions<T> extends Iterable<T> {
  Class<T> type();

  /**
   * A snapshot ordered by {@link ru.cwcode.tkach.minedi.annotation.Priority}, then by class name.
   */
  List<T> list();

  default Stream<T> stream() {
    return list().stream();
  }

  default int size() {
    return list().size();
  }

  default boolean isEmpty() {
    return list().isEmpty();
  }

  @Override
  default Iterator<T> iterator() {
    return list().iterator();
  }

  /**
   * Called for every implementation already present, then for each added later, including the new instance of a
   * recreated bean. A listener failure is logged and does not stop the others.
   */
  void onAdd(Consumer<? super T> listener);

  /**
   * Called after every addition, replacement or removal.
   */
  void onChange(Runnable listener);
}
