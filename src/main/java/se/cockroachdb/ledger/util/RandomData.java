package se.cockroachdb.ledger.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public abstract class RandomData {
    private RandomData() {
    }

    public static Money randomMoneyBetween(double low, double high, Currency currency) {
        if (high <= low) {
            throw new IllegalArgumentException("high<=low");
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int fractions = currency.getDefaultFractionDigits();
        BigDecimal bd = BigDecimal.valueOf(random.nextDouble(low, high))
                .setScale(fractions, RoundingMode.HALF_EVEN);
        return Money.of(bd, currency);
    }

    public static <T extends Enum<?>> T selectRandom(Class<T> clazz) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public static <E> E selectRandom(List<E> collection) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return collection.get(random.nextInt(collection.size()));
    }

    public static <K> K selectRandom(Set<K> set) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Object[] keys = set.toArray();
        return (K) keys[random.nextInt(keys.length)];
    }

    public static <E> E selectRandom(E[] collection) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return collection[random.nextInt(collection.length)];
    }

    public static <E> Collection<E> selectRandomUnique(List<E> collection, int count) {
        if (count > collection.size()) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(collection));
        }

        return uniqueElements;
    }

    public static <E> Collection<E> selectRandomUnique(E[] array, int count) {
        if (count > array.length) {
            throw new IllegalArgumentException("Not enough elements");
        }

        Set<E> uniqueElements = new HashSet<>();
        while (uniqueElements.size() < count) {
            uniqueElements.add(selectRandom(array));
        }

        return uniqueElements;
    }

    public static <E extends WeightedItem> E selectRandomWeighted(Collection<E> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double totalWeight = items.stream().mapToDouble(WeightedItem::getWeight).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        for (E item : items) {
            cumulativeWeight += item.getWeight();
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static <T> T selectRandomWeighted(Collection<T> items, List<Double> weights) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        if (items.size() != weights.size()) {
            throw new IllegalArgumentException("Collection and weights mismatch");
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double totalWeight = weights.stream().mapToDouble(w -> w).sum();
        double randomWeight = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;

        int idx = 0;
        for (T item : items) {
            cumulativeWeight += weights.get(idx++);
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }
}
