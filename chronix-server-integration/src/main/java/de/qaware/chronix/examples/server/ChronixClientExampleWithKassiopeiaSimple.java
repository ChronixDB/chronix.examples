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
import de.qaware.chronix.timeseries.dt.DoubleList;
import de.qaware.chronix.timeseries.dt.LongList;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An example showcase of how to integrate chronix into your application using kassiopeia-simple
 * Works with the release 0.1.1 of the chronix-server
 * Download at <a href="https://github.com/ChronixDB/chronix.server/releases/download/v0.1.1/chronix-0.1.1.zip">chronix-server-0.1.1</a>
 *
 * @author f.lautenschlager
 */
public class ChronixClientExampleWithKassiopeiaSimple {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChronixClientExampleWithKassiopeiaSimple.class);


    public static void main(String[] args) {
        SolrClient solr = new HttpSolrClient("http://localhost:8983/solr/chronix/");

        //Define a group by function for the time series records
        Function<MetricTimeSeries, String> groupBy = ts -> ts.getMetric() + "-" + ts.attribute("host");

        //Define a reduce function for the grouped time series records
        BinaryOperator<MetricTimeSeries> reduce = (ts1, ts2) -> {
            if (ts1 == null || ts2 == null) {
                return new MetricTimeSeries.Builder("empty").build();
            }
            ts1.addAll(ts2.getTimestampsAsArray(), ts2.getValuesAsArray());
            return ts1;
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
                    .append(ts.getValues())
                    .append("]")
                    .append("\n");
        }
        return sb.toString();
    }

    private static LongList concat(LongList first, LongList second) {
        first.addAll(second);
        return first;
    }

    private static DoubleList concat(DoubleList first, DoubleList second) {
        first.addAll(second);
        return first;
    }

}
