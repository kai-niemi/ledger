package io.cockroachdb.ledger.domain;

import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransferTypeConverter implements AttributeConverter<TransferType, String> {
    @Override
    public String convertToDatabaseColumn(TransferType accountType) {
        if (accountType == null) {
            return null;
        }
        return accountType.getCode();
    }

    @Override
    public TransferType convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        return Stream.of(TransferType.values())
                .filter(c -> c.getCode().equals(code))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}


