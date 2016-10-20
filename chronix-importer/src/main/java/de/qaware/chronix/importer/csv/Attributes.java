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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The attributes representation
 *
 * @author f.lautenschlager
 */
public class Attributes {

    private String[] attributes;
    private String metric;

    /**
     * Constructs the attributes
     *
     * @param metric     the metric is a required field
     * @param attributes the arbitrary attributes
     */
    Attributes(String metric, String... attributes) {
        this.metric = metric;
        this.attributes = attributes;
    }

    /**
     * Gets an attribute value
     *
     * @param i gets the attribute at position i
     * @return the attribute value
     */
    String get(int i) {
        return attributes[i];
    }

    /**
     * @return the metric
     */
    String getMetric() {
        return metric;
    }

    /**
     * @return the size / amount of the attributes
     */
    int size() {
        return attributes.length;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Attributes that = (Attributes) o;

        return new EqualsBuilder()
                .append(attributes, that.attributes)
                .append(metric, that.metric)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(attributes)
                .append(metric)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("attributes", attributes)
                .append("metric", metric)
                .toString();
    }
}
