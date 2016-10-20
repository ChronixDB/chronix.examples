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
 * A generic csv file importer.
 *
 * @author f.lautenschlager
 */
public class FileImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    private static final String METRICS_FILE_PATH = "metrics.csv";
    private final String dateFormat;
    private final Locale numberLocal;
    private final String csvDelimiter;

    private boolean longDate = false;
    private boolean instantDate = false;
    private boolean sdfDate = false;

    /**
     * Constructs a file importer
     *
     * @param dateFormat  the date format: long for ms since 1970, 'instant' for java 8 instant,
     *                    otherwise simple date format
     * @param numberLocal the number local, e.g. ENGLISH, GERMAN, ...
     */
    public FileImporter(String dateFormat, String numberLocal, String csvDelimiter) {
        this.dateFormat = dateFormat;

        if (dateFormat.equalsIgnoreCase("long")) {
            longDate = true;
        } else if (dateFormat.equalsIgnoreCase("instant")) {
            instantDate = true;
        } else {
            sdfDate = true;
        }

        if (numberLocal.equalsIgnoreCase("german")) {
            this.numberLocal = Locale.GERMAN;
        } else {
            this.numberLocal = Locale.ENGLISH;
        }

        this.csvDelimiter = csvDelimiter;


    }


    /**
     * Reads the given file / folder and calls the bi consumer with the extracted points
     *
     * @param points
     * @param folder
     * @param databases
     * @return
     */
    public Pair<Integer, Integer> importPoints(Map<Attributes, Pair<Instant, Instant>> points, File folder, BiConsumer<List<ImportPoint>, Attributes>... databases) {


        final AtomicInteger pointCounter = new AtomicInteger(0);
        final AtomicInteger tsCounter = new AtomicInteger(0);
        final File metricsFile = new File(METRICS_FILE_PATH);

        LOGGER.info("Writing imported metrics to {}", metricsFile);
        LOGGER.info("Import supports csv files as well as gz compressed csv files.");

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
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                NumberFormat nf = DecimalFormat.getInstance(numberLocal);

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

                    //Extract the attributes from the file name
                    //E.g. first_second_third_attribute.csv
                    String[] fileNameMetaData = file.getName().split("_");


                    String[] metrics = headerLine.split(csvDelimiter);

                    Map<Integer, Attributes> attributesPerTimeSeries = new HashMap<>(metrics.length);


                    for (int i = 1; i < metrics.length; i++) {
                        String metric = metrics[i];
                        String metricOnlyAscii = Normalizer.normalize(metric, Normalizer.Form.NFD);
                        metricOnlyAscii = metric.replaceAll("[^\\x00-\\x7F]", "");
                        Attributes attributes = new Attributes(metricOnlyAscii, fileNameMetaData);

                        //Check if meta data is completely set
                        if (isEmpty(attributes)) {
                            boolean deleted = deleteFile(file, inputStream, reader);
                            LOGGER.info("Attributes contains empty values {}. File {} deleted {}", attributes, file.getName(), deleted);
                            continue;
                        }

                        if (attributes.getMetric().equals(".*")) {
                            boolean deleted = deleteFile(file, inputStream, reader);
                            LOGGER.info("Attributes metric{}. File {} deleted {}", attributes.getMetric(), file.getName(), deleted);
                            continue;
                        }
                        attributesPerTimeSeries.put(i, attributes);
                        tsCounter.incrementAndGet();

                    }

                    Map<Integer, List<ImportPoint>> dataPoints = new HashMap<>();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] splits = line.split(csvDelimiter);
                        String date = splits[0];


                        Instant dateObject;
                        if (instantDate) {
                            dateObject = Instant.parse(date);
                        } else if (sdfDate) {
                            dateObject = sdf.parse(date).toInstant();
                        } else {
                            dateObject = Instant.ofEpochMilli(Long.valueOf(date));
                        }


                        for (int column = 1; column < splits.length; column++) {

                            String value = splits[column];
                            double numericValue = nf.parse(value).doubleValue();

                            ImportPoint point = new ImportPoint(dateObject, numericValue);


                            if (!dataPoints.containsKey(column)) {
                                dataPoints.put(column, new ArrayList<>());
                            }
                            dataPoints.get(column).add(point);
                            pointCounter.incrementAndGet();
                        }

                    }


                    dataPoints.values().forEach(Collections::sort);

                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(inputStream);

                    dataPoints.forEach((key, importPoints) -> {
                        for (BiConsumer<List<ImportPoint>, Attributes> database : databases) {
                            database.accept(importPoints, attributesPerTimeSeries.get(key));
                        }
                        points.put(attributesPerTimeSeries.get(key), Pair.of(importPoints.get(0).getDate(), importPoints.get(importPoints.size() - 1).getDate()));
                        //write the stats to the file
                        Instant start = importPoints.get(0).getDate();
                        Instant end = importPoints.get(importPoints.size() - 1).getDate();

                        try {
                            writeStatsLine(metricsFileWriter, attributesPerTimeSeries.get(key), start, end);
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

    private void writeStatsLine(FileWriter metricsFile, Attributes attributes, Instant start, Instant end) throws IOException {
        //host:process:metric-group:metric:start:end
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < attributes.size(); i++) {
            line.append(attributes.get(i)).append(csvDelimiter);
        }
        line.append(start).append(csvDelimiter)
                .append(end).append(csvDelimiter)
                .append("\n");

        metricsFile.write(line.toString());
        metricsFile.flush();

    }

    private boolean deleteFile(File file, InputStream inputStream, BufferedReader reader) {
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(inputStream);

        //remove file
        return file.delete();
    }

    private boolean isEmpty(Attributes attributes) {
        for (int i = 0; i < attributes.size(); i++) {
            if (empty(attributes.get(i))) {
                return true;
            }
        }
        return attributes.getMetric().isEmpty();
    }

    private boolean empty(String metric) {
        return StringUtils.isEmpty(metric);
    }
}
