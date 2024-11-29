package beans.events;

import ru.cwcode.tkach.minedi.processing.event.ApplicationEvent;

public record CustomEvent(String someData) implements ApplicationEvent {
}
