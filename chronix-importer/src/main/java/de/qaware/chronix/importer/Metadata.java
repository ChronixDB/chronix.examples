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
package de.qaware.chronix.importer;

/**
 * Created by f.lautenschlager on 10.06.2015.
 */
public class Metadata {
    private final String measurement;
    private String host;
    private String process;
    private String metricGroup;
    private String metric;

    public Metadata(String host, String process, String metricGroup, String metric, String measurement) {
        this.host = host;
        this.process = process;
        this.metricGroup = metricGroup;
        this.metric = metric;
        this.measurement = measurement;
    }

    public String getHost() {
        return host;
    }

    public String getProcess() {
        return process;
    }

    public String getMetricGroup() {
        return metricGroup;
    }

    public String getMetric() {
        return metric;
    }

    public String getMeasurement() {
        return measurement;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "host='" + host + '\'' +
                ", process='" + process + '\'' +
                ", metricGroup='" + metricGroup + '\'' +
                ", metric='" + metric + '\'' +
                '}';
    }

    public String joinWithoutMetric() {
        return host + "." + process + "." + metricGroup;
    }

    public String join() {
        return host + "." + process + "." + metricGroup + "." + metric;
    }
}
