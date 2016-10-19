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
package de.qaware.chronix.examples.exploration.ui.dt;

import javafx.beans.property.SimpleStringProperty;

/**
 * Simple java fx result row
 *
 * @author f.lautenschlager
 */
public class ResultRow {

    private final SimpleStringProperty timeSeries = new SimpleStringProperty("");
    private final SimpleStringProperty aggregationOrAnalysis = new SimpleStringProperty("");
    private final SimpleStringProperty arguments = new SimpleStringProperty("");
    private final SimpleStringProperty values = new SimpleStringProperty("");
    private final SimpleStringProperty order = new SimpleStringProperty("");


    public String getTimeSeries() {
        return timeSeries.get();
    }

    public void setTimeSeries(String timeSeries) {
        this.timeSeries.set(timeSeries);
    }

    public SimpleStringProperty timeSeriesProperty() {
        return timeSeries;
    }

    public String getAggregationOrAnalysis() {
        return aggregationOrAnalysis.get();
    }

    public void setAggregationOrAnalysis(String aggregationOrAnalysis) {
        this.aggregationOrAnalysis.set(aggregationOrAnalysis);
    }

    public SimpleStringProperty aggregationOrAnalysisProperty() {
        return aggregationOrAnalysis;
    }

    public String getArguments() {
        return arguments.get();
    }

    public void setArguments(String arguments) {
        this.arguments.set(arguments);
    }

    public SimpleStringProperty argumentsProperty() {
        return arguments;
    }

    public String getValues() {
        return values.get();
    }

    public void setValues(String values) {
        this.values.set(values);
    }

    public SimpleStringProperty valuesProperty() {
        return values;
    }

    public String getOrder() {
        return order.get();
    }

    public void setOrder(String order) {
        this.order.set(order);
    }

    public SimpleStringProperty orderProperty() {
        return order;
    }
}