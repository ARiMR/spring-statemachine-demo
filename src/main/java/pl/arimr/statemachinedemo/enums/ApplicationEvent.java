package pl.arimr.statemachinedemo.enums;

public enum ApplicationEvent implements FsmEvent {

    ACCEPT(EventType.EVENT),
    APPROVE(EventType.EVENT),
    DISCARD(EventType.EVENT);

    private final EventType eventType;

    ApplicationEvent(final EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }
}
