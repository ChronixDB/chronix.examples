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
import de.qaware.chronix.converter.AdvancedTimeSeriesConverter;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.TimeSeries;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.qaware.chronix.timeseries.TimeSeries.merge;

/**
 * An example showcase of how to integrate chronix into your application.
 * Note: The example data stored in the release
 *
 * @author f.lautenschlager
 */
public class ChronixClientExampleWithTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChronixClientExampleWithTimeSeries.class);

    public static void main(String[] args) {
        SolrClient solr = new HttpSolrClient.Builder("http://localhost:8983/solr/chronix/").build();

        //Define a group by function for the time series records
        Function<TimeSeries<Long, Double>, String> groupBy = ts -> ts.getAttribute("metric") + "-" + ts.getAttribute("host");

        //Define a reduce function for the grouped time series records. We use the average.
        BinaryOperator<TimeSeries<Long, Double>> reduce = (ts1, ts2) -> merge(ts1, ts2, (y1, y2) -> (y1 + y2) / 2);

        //Instantiate a Chronix Client
        ChronixClient<TimeSeries<Long, Double>, SolrClient, SolrQuery> chronix = new ChronixClient<>(
                new AdvancedTimeSeriesConverter(), new ChronixSolrStorage<>(200, groupBy, reduce));

        //We want the maximum of all time series that metric matches *load*.
        SolrQuery query = new SolrQuery("metric:*Load*");
        query.addFilterQuery("function=max");

        //The result is a Java Stream. We simply collect the result into a list.
        List<TimeSeries<Long, Double>> maxTS = chronix.stream(solr, query).collect(Collectors.toList());

        //Just print it out.
        LOGGER.info("Result for query {} is: {}", query, maxTS);
    }


}
