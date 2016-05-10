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
