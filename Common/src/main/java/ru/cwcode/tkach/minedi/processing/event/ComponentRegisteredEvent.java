package ru.cwcode.tkach.minedi.processing.event;

public record ComponentRegisteredEvent(Class<?> component) implements Event {
  @Override
  public String toString() {
    return "ComponentRegisteredEvent{" +
           "component=" + component.getSimpleName() +
           '}';
  }
}
