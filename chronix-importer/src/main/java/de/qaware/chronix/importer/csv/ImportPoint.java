/*
 * Copyright (C) 2015 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package de.qaware.chronix.importer.csv;

import java.time.Instant;

/**
 * The representation of a point
 *
 * @author f.lautenschlager
 */
class ImportPoint implements Comparable<ImportPoint> {

    private Instant date;
    private double value;

    ImportPoint(Instant date, Double value) {
        this.date = date;
        this.value = value;
    }

    Instant getDate() {
        return date;
    }

    double getValue() {
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
