package de.qaware.chronix.importer;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by f.lautenschlager on 05.05.2015.
 */

public final class Pair<T, R> {

    private final T first;
    private final R second;

    public static <T, R> Pair<T, R> of(T first, R second) {
        return new Pair<>(first, second);
    }

    private Pair(T first, R second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public R getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        return new EqualsBuilder()
                .append(first, pair.first)
                .append(second, pair.second)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(first)
                .append(second)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}

