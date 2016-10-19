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

import de.qaware.chronix.ChronixClient;
import de.qaware.chronix.converter.MetricTimeSeriesConverter;
import de.qaware.chronix.converter.serializer.gen.MetricProtocolBuffers;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import de.qaware.chronix.timeseries.dts.Point;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by f.lautenschlager on 10.06.2015.
 */
public class DatabaseImporter {

    public static final String DATABASE_SERVER_IP = "localhost";
    //Solr connection stuff
    public static final String SOLR_BASE_URL = "http://" + DATABASE_SERVER_IP + ":8983/solr/";
    public static final SolrClient CHRONIX_SOLR_CLIENT = new HttpSolrClient.Builder(SOLR_BASE_URL + "chronix").build();
    //serialized size of the list
    private static final int LIST_SERIALIZED_SIZE = 2;
    //serialized size of a point
    private static final int POINT_SERIALIZED_SIZE = MetricProtocolBuffers.Point.newBuilder().setTlong(Instant.now().toEpochMilli()).setV(4711).build().getSerializedSize();
    private static final int SER_SIZE = LIST_SERIALIZED_SIZE + POINT_SERIALIZED_SIZE;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseImporter.class);
    public static ChronixClient<MetricTimeSeries, SolrClient, SolrQuery> CHRONIX = new ChronixClient<>(new MetricTimeSeriesConverter(), new ChronixSolrStorage<>(200, null, null));


    private DatabaseImporter() {

    }

    public static BiConsumer<List<ImportPoint>, Metadata> importToChronix() {

        return (importPoints, metadata) -> {


            LOGGER.info("Chronix ---> Importing {}", metadata);

            //String metric = metadata.joinWithoutMetric() + "." + metadata.getMetric();
            MetricTimeSeries.Builder builder = new MetricTimeSeries.Builder(metadata.getMetric())
                    .attribute("host", metadata.getHost())
                    .attribute("process", metadata.getProcess())
                    .attribute("group", metadata.getMetricGroup())
                    .attribute("measurement", metadata.getMeasurement());

            //Convert points
            importPoints.forEach(point -> builder.point(point.getDate().toEpochMilli(), point.getValue()));

            MetricTimeSeries timeSeries = builder.build();
            timeSeries.sort();
            List<Point> points = timeSeries.points().collect(Collectors.toList());

            final int chunkSize = 128 * 1024;

            //number of points
            int numberOfPoints = chunkSize / SER_SIZE;
            int start = 0;
            int end;

            List<MetricTimeSeries> splits = new ArrayList<>();
            //Loop over the time series
            while (start <= timeSeries.size()) {

                if (timeSeries.size() - (start + numberOfPoints) > 0) {
                    end = start + numberOfPoints;
                } else {
                    end = timeSeries.size();
                }

                List<Point> sublist = points.subList(start, end);
                start += numberOfPoints;

                MetricTimeSeries.Builder toAddTs = new MetricTimeSeries.Builder(metadata.getMetric())
                        .attribute("host", metadata.getHost())
                        .attribute("process", metadata.getProcess())
                        .attribute("group", metadata.getMetricGroup());
                toAddTs.attributes(timeSeries.attributes());

                sublist.forEach(pair -> toAddTs.point(pair.getTimestamp(), pair.getValue()));

                splits.add(toAddTs.build());
            }

            CHRONIX.add(splits, CHRONIX_SOLR_CLIENT);
        };

    }
}
