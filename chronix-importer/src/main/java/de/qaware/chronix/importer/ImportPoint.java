package de.qaware.chronix.importer;

import java.time.Instant;

/**
 * Created by f.lautenschlager on 10.06.2015.
 */
public class ImportPoint implements Comparable<ImportPoint>{

    private Instant date;
    private double value;

    public ImportPoint(Instant date, Double value) {
        this.date = date;
        this.value = value;
    }

    public Instant getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }

    @Override
    public int compareTo(ImportPoint o) {
        return this.date.compareTo(o.date);
    }

    @Override
    public String toString() {
        return "ImportPoint{" +
                "date=" + date +
                ", value=" + value +
                '}';
    }
}
