package de.qaware.chronix.importer;

import de.qaware.chronix.ChronixClient;
import de.qaware.chronix.converter.KassiopeiaSimpleConverter;
import de.qaware.chronix.converter.serializer.gen.SimpleProtocolBuffers;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.MetricTimeSeries;
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

    //serialized size of the list
    private static final int LIST_SERIALIZED_SIZE = 2;
    //serialized size of a point
    private static final int POINT_SERIALIZED_SIZE = SimpleProtocolBuffers.Point.newBuilder().setT(Instant.now().toEpochMilli()).setV(4711).build().getSerializedSize();

    private static final int SER_SIZE = LIST_SERIALIZED_SIZE + POINT_SERIALIZED_SIZE;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseImporter.class);

    public static ChronixClient<MetricTimeSeries, SolrClient, SolrQuery> CHRONIX = new ChronixClient<>(new KassiopeiaSimpleConverter(), new ChronixSolrStorage<>(200, null, null));
    public static final String DATABASE_SERVER_IP = "localhost";
    //Solr connection stuff
    public static final String SOLR_BASE_URL = "http://" + DATABASE_SERVER_IP + ":8983/solr/";
    public static final SolrClient CHRONIX_SOLR_CLIENT = new HttpSolrClient(SOLR_BASE_URL + "chronix");


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
            List<de.qaware.chronix.timeseries.dt.Point> points = timeSeries.points().collect(Collectors.toList());

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

                List<de.qaware.chronix.timeseries.dt.Point> sublist = points.subList(start, end);
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
