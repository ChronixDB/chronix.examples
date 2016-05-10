package de.qaware.chronix.importer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/**
 * Created by f.lautenschlager on 10.06.2015.
 */
public class FileImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    private static final String METRICS_FILE_PATH = "metrics.csv";

    private FileImporter() {

    }

    @SafeVarargs
    public static final Pair<Integer, Integer> importPoints(Map<Metadata, Pair<Instant, Instant>> points, File folder, BiConsumer<List<ImportPoint>, Metadata>... databases) {

        final AtomicInteger pointCounter = new AtomicInteger(0);
        final AtomicInteger tsCounter = new AtomicInteger(0);
        final File metricsFile = new File(METRICS_FILE_PATH);
        try {
            final FileWriter metricsFileWriter = new FileWriter(metricsFile);


            Collection<File> files = new ArrayList<>();
            if (folder.isFile()) {
                files.add(folder);
            } else {
                files.addAll(FileUtils.listFiles(folder, new String[]{"gz", "csv"}, true));
            }

            AtomicInteger counter = new AtomicInteger(0);

            files.parallelStream().forEach(file -> {
                boolean onlyMinusOne = true;
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");


                NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
                InputStream inputStream = null;
                BufferedReader reader = null;
                try {
                    inputStream = new FileInputStream(file);

                    if (file.getName().endsWith("gz")) {
                        inputStream = new GZIPInputStream(inputStream);
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    //Read the first line
                    String headerLine = reader.readLine();

                    if (headerLine == null || headerLine.isEmpty()) {
                        boolean deleted = deleteFile(file, inputStream, reader);
                        LOGGER.debug("File is empty {}. File {} removed {}", file.getName(), deleted);
                        return;
                    }


                    //host _ process _ metricGroup
                    String[] fileNameMetaData = file.getName().split("_");
                    //build meta data object
                    String host = fileNameMetaData[0];
                    String process = fileNameMetaData[1];
                    String metricGroup = fileNameMetaData[2];
                    String measurement = file.getParentFile().getName();


                    String[] metrics = headerLine.split(";");

                    Map<Integer, Metadata> metadatas = new HashMap<>(metrics.length);


                    for (int i = 1; i < metrics.length; i++) {
                        String metric = metrics[i];
                        String metricOnlyAscii = Normalizer.normalize(metric, Normalizer.Form.NFD);
                        metricOnlyAscii = metric.replaceAll("[^\\x00-\\x7F]", "");
                        Metadata metadata = new Metadata(host, process, metricGroup, metricOnlyAscii, measurement);

                        //Check if meta data is completely set
                        if (isEmpty(metadata)) {
                            boolean deleted = deleteFile(file, inputStream, reader);
                            LOGGER.info("Metadata contains empty values {}. File {} deleted {}", metadata, file.getName(), deleted);
                            continue;
                        }

                        if (metadata.getMetric().equals(".*")) {
                            boolean deleted = deleteFile(file, inputStream, reader);
                            LOGGER.info("Metadata metric{}. File {} deleted {}", metadata.getMetric(), file.getName(), deleted);
                            continue;
                        }
                        metadatas.put(i, metadata);
                        tsCounter.incrementAndGet();

                    }

                    Map<Integer, List<ImportPoint>> dataPoints = new HashMap<>();

                    String line;
                    boolean instantdate = true;
                    boolean onlyOnce = true;
                    while ((line = reader.readLine()) != null) {
                        String[] splits = line.split(";");
                        String date = splits[0];

                        if (onlyOnce) {
                            try {
                                Instant.parse(date);
                            } catch (Exception e) {
                                instantdate = false;
                            }
                            onlyOnce = false;
                        }
                        Instant dateObject;
                        if (instantdate) {
                            dateObject = Instant.parse(date);
                        } else {
                            dateObject = sdf.parse(date).toInstant();
                        }


                        String[] values = splits;

                        for (int column = 1; column < values.length; column++) {

                            String value = values[column];
                            double numericValue = nf.parse(value).doubleValue();

                            ImportPoint point;
                            if (instantdate) {
                                point = new ImportPoint(dateObject, numericValue);
                            } else {
                                point = new ImportPoint(dateObject, numericValue);

                            }

                            if (!dataPoints.containsKey(column)) {
                                dataPoints.put(column, new ArrayList<>());
                            }
                            dataPoints.get(column).add(point);
                            pointCounter.incrementAndGet();
                        }

                    }
/*
                    if (onlyMinusOne) {
                        pointCounter.addAndGet(-dataPoints.size());
                        tsCounter.decrementAndGet();

                        //close all streams
                        boolean deleted = deleteFile(file, inputStream, reader);
                        LOGGER.info("{} contains only -1. Deleted {}", file.getName(), deleted);

                        return;
                    }
                    */

                    dataPoints.values().forEach(Collections::sort);

                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(inputStream);

                    dataPoints.forEach((key, value) -> {
                        for (BiConsumer<List<ImportPoint>, Metadata> database : databases) {
                            database.accept(value, metadatas.get(key));
                        }
                        points.put(metadatas.get(key), Pair.of(value.get(0).getDate(), value.get(value.size() - 1).getDate()));
                        //write the stats to the file
                        Instant start = value.get(0).getDate();
                        Instant end = value.get(value.size() - 1).getDate();

                        try {
                            writeStatsLine(metricsFileWriter, metadatas.get(key), start, end);
                        } catch (IOException e) {
                            LOGGER.error("Could not write stats line", e);
                        }
                        LOGGER.info("{} of {} time series imported", counter.incrementAndGet(), tsCounter.get());
                    });


                } catch (Exception e) {
                    LOGGER.info("Exception while reading points.", e);
                } finally {
                    //close all streams
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(inputStream);
                }

            });
        } catch (Exception e) {
            LOGGER.error("Exception occurred during reading points.");
        }
        return Pair.of(tsCounter.get(), pointCounter.get());
    }

    private static void writeStatsLine(FileWriter metricsFile, Metadata metadata, Instant start, Instant end) throws IOException {
        //host:process:metric-group:metric:start:end
        StringBuilder line = new StringBuilder();
        line.append(metadata.getHost()).append(";")
                .append(metadata.getProcess()).append(";")
                .append(metadata.getMetricGroup()).append(";")
                .append(metadata.getMetric()).append(";")
                .append(start).append(";")
                .append(end).append(";")
                .append("\n");

        metricsFile.write(line.toString());
        metricsFile.flush();

    }

    private static boolean deleteFile(File file, InputStream inputStream, BufferedReader reader) {
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(inputStream);

        //remove file
        return file.delete();
    }

    private static boolean isEmpty(Metadata metadata) {
        return empty(metadata.getMetric()) || empty(metadata.getMetricGroup()) || empty(metadata.getProcess()) || empty(metadata.getHost());
    }

    private static boolean empty(String metric) {
        return StringUtils.isEmpty(metric);
    }
}
