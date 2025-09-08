package se.cockroachdb.ledger.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.DynamicInsert;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import se.cockroachdb.ledger.annotations.EventAggregate;

/**
 * Represents a monetary transaction labelled 'transfer' to separate
 * it from database transactions.
 */
@Entity
@Table(name = "transfer")
//@DynamicInsert
public class TransferEntity extends AbstractEntity<UUID> implements EventAggregate<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "city")
    private String city;

    @Column(name = "transfer_type", updatable = false, nullable = false)
    @Convert(converter = TransferTypeConverter.class)
    private TransferType transferType;

    @Column(name = "transfer_date", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transferDate;

    @Column(name = "booking_date", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate bookingDate;

    @OneToMany(mappedBy = "transferEntity",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private final List<TransferItemEntity> items = new ArrayList<>();

    public TransferEntity() {
    }

    protected TransferEntity(UUID id,
                             String city,
                             TransferType transferType,
                             LocalDate bookingDate,
                             LocalDate transferDate) {
        this.id = id;
        this.city = city;
        this.transferType = transferType;
        this.bookingDate = bookingDate;
        this.transferDate = transferDate;
    }

    @Override
    public UUID getEntityId() {
        return id;
    }

    @Override
    public UUID getEventId() {
        return UUID.randomUUID();
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    protected void onCreate() {
        if (bookingDate == null) {
            bookingDate = LocalDate.now();
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public List<TransferItemEntity> getItems() {
        return Collections.unmodifiableList(items);
    }

    public TransferEntity addItems(List<TransferItemEntity> items) {
        this.items.addAll(items);
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;

        private String city;

        private TransferType transferType;

        private LocalDate bookingDate;

        private LocalDate transferDate;

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withCity(String city) {
            this.city = city;
            return this;
        }

        public Builder withTransferType(TransferType transferType) {
            this.transferType = transferType;
            return this;
        }

        public Builder withBookingDate(LocalDate bookingDate) {
            this.bookingDate = bookingDate;
            return this;
        }

        public Builder withTransferDate(LocalDate transferDate) {
            this.transferDate = transferDate;
            return this;
        }

        public TransferEntity build() {
            return new TransferEntity(id, city, transferType, bookingDate, transferDate);
        }
    }
}
