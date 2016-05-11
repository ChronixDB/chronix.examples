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

import de.qaware.chronix.importer.configuration.Configuration;
import de.qaware.chronix.importer.csv.Attributes;
import de.qaware.chronix.importer.csv.ChronixImporter;
import de.qaware.chronix.importer.csv.FileImporter;
import de.qaware.chronix.importer.csv.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class to start the csv importer
 *
 * @author f.lautenschlager
 */
public class CSVImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVImporter.class);

    /**
     * Imports the csv files from the given path that points to a directory or a file.
     * The importer expects three arguments: the path to yml config, and the path to the time series csv file(s).
     *
     * @param args contains the path to the time series csv or a directory with csv files and the date format.
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            LOGGER.info("The given arguments does not contain the required argument 'ymlConfig' 'tsPath'");
            return;
        }
        String ymlPath = args[0];
        Map<String, Object> config = Configuration.load(ymlPath);


        //First we check if the path exists
        String timeSeriesPath = args[1];
        File timeSeriesFiles = new File(timeSeriesPath);

        if (!timeSeriesFiles.exists()) {
            LOGGER.info("The file or directory {} does not exist.", timeSeriesPath);
        }

        boolean onlyMetricsFile = (boolean) config.get("onlyGenerateMetricsFile");

        //get the required values from the configuration
        String dateFormat = (String) config.get("dateFormat");
        String numberFormat = (String) config.get("numberFormat");
        String csvDelimiter = (String) config.get("csvDelimiter");
        String url = (String) config.get("chronix");
        String[] attributeFields = ((List<String>) config.get("attributeFields")).toArray(new String[0]);

        Map<Attributes, Pair<Instant, Instant>> importStatistics = new HashMap<>();
        ChronixImporter chronixImporter = new ChronixImporter(url, attributeFields);
        FileImporter importer = new FileImporter(dateFormat, numberFormat, csvDelimiter);
        Pair<Integer, Integer> result;

        LOGGER.info("Start importing files to the Chronix.");
        if (onlyMetricsFile) {
            result = importer.importPoints(importStatistics, timeSeriesFiles, chronixImporter.doNothing());
        } else {
            result = importer.importPoints(importStatistics, timeSeriesFiles, chronixImporter.importToChronix());
        }


        LOGGER.info("Import done. Imported {} time series with {} points", result.getFirst(), result.getSecond());
    }
}
