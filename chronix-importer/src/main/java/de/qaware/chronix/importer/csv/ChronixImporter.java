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


import de.qaware.chronix.ChronixClient;
import de.qaware.chronix.converter.MetricTimeSeriesConverter;
import de.qaware.chronix.converter.serializer.gen.MetricProtocolBuffers;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import de.qaware.chronix.timeseries.dts.Point;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.opentsdb.client.ExpectResponse;
import org.opentsdb.client.HttpClientImpl;
import org.opentsdb.client.builder.Metric;
import org.opentsdb.client.builder.MetricBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author f.lautenschlager
 */
public class ChronixImporter {


    private static final Logger LOGGER = LoggerFactory.getLogger(ChronixImporter.class);

    //serialized size of the list
    private static final int LIST_SERIALIZED_SIZE = 2;
    //serialized size of a point
    private static final int POINT_SERIALIZED_SIZE = MetricProtocolBuffers.Point.newBuilder()
            .setTlong(Instant.now().toEpochMilli())
            .setV(4711).build()
            .getSerializedSize();
    private static final int SER_SIZE = LIST_SERIALIZED_SIZE + POINT_SERIALIZED_SIZE;
    private final String URL;


    private final String[] SCHEMA_FIELDS;

    private final HttpSolrClient CHRONIX_SOLR_CLIENT;
    private ChronixClient<MetricTimeSeries, HttpSolrClient, SolrQuery> CHRONIX;

    /**
     * Constructs a Chronix importer
     *
     * @param url the url to chronix server
     */
    public ChronixImporter(String url, String[] attributeFields) {
        URL = url;
        CHRONIX_SOLR_CLIENT = new HttpSolrClient.Builder().withBaseSolrUrl(url).build();
        SCHEMA_FIELDS = attributeFields;
        CHRONIX = new ChronixClient<>(new MetricTimeSeriesConverter(),
                new ChronixSolrStorage(200, null, null));
    }

    public BiConsumer<List<ImportPoint>, Attributes> doNothing() {
        return (importPoints, attributes) -> {
            //simple ignore the values
        };
    }


    /**
     * Imports the time series to Chronix. Splits a time series up into chunks and stores the records.
     * Does not do a commit on the Chronix connection.
     *
     * @return a BiConsumer handling the given list of import points and attributes
     */
    public BiConsumer<List<ImportPoint>, Attributes> importToChronix(boolean cleanImport, boolean useOpenTSDB) {

        if (cleanImport) {
            deleteIndex();
        }

        if (useOpenTSDB) {
            LOGGER.info("Using OpenTSDB protocol");
        }


        return (importPoints, attributes) -> {

            LOGGER.info("Chronix ---> Importing {}", attributes);
            MetricTimeSeries.Builder builder = getPrefilledTimeSeriesBuilder(attributes);


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

            List<MetricTimeSeries> records = new ArrayList<>();
            //Loop over the time series
            while (start <= timeSeries.size()) {

                if (timeSeries.size() - (start + numberOfPoints) > 0) {
                    end = start + numberOfPoints;
                } else {
                    end = timeSeries.size();
                }

                List<Point> chunk = points.subList(start, end);
                start += numberOfPoints;

                MetricTimeSeries.Builder record = getPrefilledTimeSeriesBuilder(attributes);

                chunk.forEach(pair -> record.point(pair.getTimestamp(), pair.getValue()));

                records.add(record.build());
            }

            if (useOpenTSDB) {
                org.opentsdb.client.HttpClient client = new HttpClientImpl(URL + "/ingest/opentsdb/http");
                MetricBuilder openTSDBBuilder = MetricBuilder.getInstance();


                for (MetricTimeSeries chunk : records) {
                    chunk.points().forEach(point -> {
                        Metric metric = openTSDBBuilder.addMetric(attributes.getMetric());
                        for (int i = 0; i < SCHEMA_FIELDS.length; i++) {
                            metric.addTag(SCHEMA_FIELDS[i], attributes.get(i));
                        }
                        metric.setDataPoint(point.getTimestamp(), point.getValue());
                    });

                }

                try {
                    client.setCommit(false);
                    client.pushMetrics(openTSDBBuilder, ExpectResponse.STATUS_CODE);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                CHRONIX.add(records, CHRONIX_SOLR_CLIENT);
            }

        };

    }

    private void deleteIndex() {
        try {
            CHRONIX_SOLR_CLIENT.deleteByQuery("*:*");
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Could not delete index due to an exception", e);
        }
    }

    private MetricTimeSeries.Builder getPrefilledTimeSeriesBuilder(Attributes attributes) {
        //String metric = metadata.joinWithoutMetric() + "." + metadata.getMetric();
        MetricTimeSeries.Builder builder = new MetricTimeSeries.Builder(attributes.getMetric(), "metric");
        for (int i = 0; i < SCHEMA_FIELDS.length; i++) {
            builder.attribute(SCHEMA_FIELDS[i], attributes.get(i));
        }
        return builder;
    }

    public void commit() {
        try {
            CHRONIX_SOLR_CLIENT.commit();
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Could not delete index due to an exception", e);
        }
    }
}
