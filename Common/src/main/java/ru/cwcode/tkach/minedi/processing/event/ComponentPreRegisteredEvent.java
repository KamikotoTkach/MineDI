package ru.cwcode.tkach.minedi.processing.event;

public record ComponentPreRegisteredEvent(Class<?> component) implements Event {
}
