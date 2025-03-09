package ru.cwcode.tkach.minedi.processing.event;

public record BeanDestroyEvent(Object bean) implements ApplicationEvent {
}
