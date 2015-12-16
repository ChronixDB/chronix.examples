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
package de.qaware.chronix.examples.server;

import de.qaware.chronix.ChronixClient;
import de.qaware.chronix.converter.KassiopeiaSimpleConverter;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An example showcase of how to integrate chronix into your application.
 * Works with the release 0.1 of the chronix-server
 * Download at <a href="https://github.com/ChronixDB/chronix.server/releases/download/v0.1/chronix-0.1.zip">chronix-server-0.1</a>
 *
 * @author f.lautenschlager
 */
public class ChronixClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChronixClientExample.class);

    public static void main(String[] args) {
        SolrClient solr = new HttpSolrClient("http://localhost:8983/solr/chronix/");

        //Define a group by function for the time series records
        Function<MetricTimeSeries, String> groupBy = ts -> ts.getMetric() + "-" + ts.attribute("host");

        //Define a reduce function for the grouped time series records
        BinaryOperator<MetricTimeSeries> reduce = (ts1, ts2) -> {
            MetricTimeSeries.Builder reduced = new MetricTimeSeries
                    .Builder(ts1.getMetric())
                    .data(concat(ts1.getTimestamps(), ts2.getTimestamps()),
                            concat(ts1.getValues(), ts2.getValues()))
                    .attributes(ts2.attributes());
            return reduced.build();
        };
        //Instantiate a Chronix Client
        ChronixClient<MetricTimeSeries, SolrClient, SolrQuery> chronix = new ChronixClient<>(
                new KassiopeiaSimpleConverter(), new ChronixSolrStorage<>(200, groupBy, reduce));

        //We want the maximum of all time series that metric matches *load*.
        SolrQuery query = new SolrQuery("metric:*Load*");
        query.addFilterQuery("ag=max");

        //The result is a Java Stream. We simply collect the result into a list.
        List<MetricTimeSeries> maxTS = chronix.stream(solr, query).collect(Collectors.toList());

        //Just print it out.
        LOGGER.info("Result for query {} is: {}", query, prettyPrint(maxTS));
    }

    private static String prettyPrint(List<MetricTimeSeries> maxTS) {
        StringBuilder sb = new StringBuilder("\n");

        for (MetricTimeSeries ts : maxTS) {
            sb.append("metric:[")
                    .append(ts.getMetric())
                    .append("] with value: [")
                    .append(ts.getValues().collect(Collectors.toList()))
                    .append("]")
                    .append("\n");
        }
        return sb.toString();
    }

    private static <T> List<T> concat(Stream<T> timestamps, Stream<T> timestamps1) {
        return Stream.concat(timestamps, timestamps1).collect(Collectors.toList());
    }
}
