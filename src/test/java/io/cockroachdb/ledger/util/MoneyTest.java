package io.cockroachdb.ledger.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.cockroachdb.ledger.util.Money.SEK;
import static io.cockroachdb.ledger.util.Money.of;

public class MoneyTest {
    @Test
    public void givenDifferentCurrencies_whenUsingConstructors_thenExpectFractions() {
        Money sek = Money.of("0.00", SEK);
        Assertions.assertEquals("0.00 SEK", sek.toString());

        sek = Money.of(BigDecimal.valueOf(0.000), SEK);
        Assertions.assertEquals("0.00 SEK", sek.toString());
    }

    @Test
    public void whenBinaryArithmetics_thenSucceed() {
        assertEquals(
                of("100.00", SEK),
                of("80.00", SEK).plus(of("20.00", SEK)));

        assertEquals(
                of("19.50", SEK),
                of("10.05", SEK).plus(of("9.95", SEK), of("-0.50", SEK)));

        assertEquals(
                of("80.00", SEK),
                of("100.00", SEK).minus(of("20.00", SEK)));

        assertEquals(
                of("100.00", SEK),
                of("10.00", SEK).multiply(10));

        assertEquals(
                of("20.00", SEK),
                of("100.00", SEK).divide(5));

        assertEquals(
                of("16.67", SEK),
                of("100.00", SEK).divideAndRound(6));

        assertEquals(
                of("0.00", SEK),
                of("100.00", SEK).remainder(100));
    }

    @Test
    public void whenComparisonOperators_thenSucceed() {
        assertTrue(of("110.00", SEK).isGreaterThan(of("100.00", SEK)));
        assertTrue(of("100.00", SEK).isGreaterThanOrEqualTo(of("100.00", SEK)));
        assertTrue(of("99.00", SEK).isLessThan(of("100.00", SEK)));
        assertTrue(of("100.00", SEK).isLessThanOrEqualTo(of("100.00", SEK)));
        assertTrue(of("100.00", SEK).isSameCurrency(of("100.00", SEK)));
    }

    @Test
    public void whenUnaryOperators_thenExpectImmutability() {
        Money m = Money.of("15.00", "SEK");
        Assertions.assertNotSame(m, m.negate().negate());
        assertEquals(m, m.negate().negate());
    }

    @Test
    public void whenMixingCurrencies_thenFail() {
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").minus(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").plus(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").isGreaterThan(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").isGreaterThanOrEqualTo(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").isLessThan(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
        Assertions.assertThrows(CurrencyMismatchException.class, () -> {
            Money.of("15.00", "SEK").isLessThanOrEqualTo(Money.of("0.00", "USD"));
            Assertions.fail("Must not succeed");
        });
    }
}
