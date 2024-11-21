package se.cockroachdb.ledger.event;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

public class TransferCreatedEvent extends ApplicationEvent {
    private UUID id;

    private String city;

    public TransferCreatedEvent(Object source) {
        super(source);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
