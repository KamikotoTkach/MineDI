package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.base.Config;
import ru.cwcode.tkach.minedi.processing.event.ApplicationEvent;

public record ConfigReloadEvent<C extends Config<C>>(C config) implements ApplicationEvent {
}
