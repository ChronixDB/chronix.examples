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
package de.qaware.chronix.examples.exploration.ui;

import de.qaware.chronix.ChronixClient;
import de.qaware.chronix.converter.KassiopeiaSimpleConverter;
import de.qaware.chronix.examples.exploration.ui.dt.DateAxis;
import de.qaware.chronix.examples.exploration.ui.dt.ResultRow;
import de.qaware.chronix.examples.exploration.ui.log.TextAreaLogger;
import de.qaware.chronix.solr.client.ChronixSolrStorage;
import de.qaware.chronix.timeseries.MetricTimeSeries;
import de.qaware.chronix.timeseries.Point;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Main controller for our simple example ui
 *
 * @author f.lautenschlager
 */
public class MainController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

    @FXML
    private TextArea logs;

    @FXML
    private Circle connectedState;

    @FXML
    private TextArea query;

    @FXML
    private TextArea fqQuery;

    @FXML
    private LineChart<DateAxis, NumberAxis> chart;

    /**
     * Table stuff below
     */
    @FXML
    private TableView<ResultRow> resultTable;
    @FXML
    private TableColumn<ResultRow, String> timeSeries;
    @FXML
    private TableColumn<ResultRow, String> aggregationOrAnalysis;
    @FXML
    private TableColumn<ResultRow, String> arguments;
    @FXML
    private TableColumn<ResultRow, String> values;
    @FXML
    private TableColumn<ResultRow, String> order;

    private ObservableList<ResultRow> rows = FXCollections.observableArrayList();

    //Chronix stuff
    private ChronixClient<MetricTimeSeries, SolrClient, SolrQuery> chronix;
    private SolrClient solr;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        //Pipe logs to ui
        TextAreaLogger.setTextArea(logs);

        //Init table
        timeSeries.setCellValueFactory(cellData -> cellData.getValue().timeSeriesProperty());
        aggregationOrAnalysis.setCellValueFactory(cellData -> cellData.getValue().aggregationOrAnalysisProperty());
        arguments.setCellValueFactory(cellData -> cellData.getValue().argumentsProperty());
        values.setCellValueFactory(cellData -> cellData.getValue().valuesProperty());
        order.setCellValueFactory(cellData -> cellData.getValue().orderProperty());
        resultTable.setItems(rows);

        EventHandler<KeyEvent> queryExecuteHandler = event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
                queryTimeSeries();
                event.consume();
            }
        };

        //add the event handler
        query.setOnKeyPressed(queryExecuteHandler);
        fqQuery.setOnKeyPressed(queryExecuteHandler);

    }

    public void initChronix(String solrUrl) {
        //Do the worker stuff in a own thread
        Task task = new Task<Void>() {
            @Override
            public Void call() {

                LOGGER.info("Setting up Chronix with a remote Solr to URL {}", solrUrl);
                solr = new HttpSolrClient(solrUrl);

                boolean solrAvailable = solrAvailable();
                LOGGER.info("Checking connection to Solr. Result {}", solrAvailable);

                if (solrAvailable) {
                    Platform.runLater(() -> connectedState.setFill(Color.GREEN));

                } else {
                    Platform.runLater(() -> connectedState.setFill(Color.RED));
                    return null;
                }

                Function<MetricTimeSeries, String> groupBy = MainController.this::join;

                BinaryOperator<MetricTimeSeries> reduce = (timeSeries, timeSeries2) -> {
                    timeSeries.addAll(timeSeries2.getTimestampsAsArray(), timeSeries2.getValuesAsArray());
                    return timeSeries;
                };

                chronix = new ChronixClient<>(new KassiopeiaSimpleConverter(), new ChronixSolrStorage<>(200, groupBy, reduce));

                return null;
            }
        };

        new Thread(task).start();
    }

    private void queryTimeSeries() {
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String queryString = query.getText().trim();
                String fq = fqQuery.getText().trim();


                Platform.runLater(() -> {
                    chart.getData().clear();
                    rows.clear();
                    //Start the query
                    chart.setTitle("Your Query was q=" + queryString + " fq=" + fq);
                });

                SolrQuery query = new SolrQuery(queryString);
                query.addField("+data");

                boolean hasFilterQueries = !fq.isEmpty();

                if (hasFilterQueries) {
                    query.addFilterQuery(fq);
                }

                long queryStart = System.currentTimeMillis();
                List<MetricTimeSeries> result = chronix.stream(solr, query).collect(Collectors.toList());
                long queryEnd = System.currentTimeMillis();
                LOGGER.info("Query took: {} ms for {} points", (queryEnd - queryStart), size(result));
                queryStart = System.currentTimeMillis();
                result.forEach(ts -> {
                    if (hasFilterQueries) {
                        addFunctionsToTable(ts);
                    }
                    convertTsToSeries(ts);
                });
                queryEnd = System.currentTimeMillis();
                LOGGER.info("Charting took: {} ms", (queryEnd - queryStart));
                return null;
            }
        };
        new Thread(task).start();

    }

    private void addFunctionsToTable(MetricTimeSeries ts) {

        //find the aggregations / and analyses

        ts.getAttributesReference().forEach((key, value) -> {
            //we have function value
            if (key.contains("function") && !key.contains("arguments")) {
                String[] splits = key.split("_");

                //function name
                String name = splits[2];

                //Check if the function has an argument
                Object arguments = ts.attribute(splits[0] + "_" + splits[1] + "_arguments_" + splits[2]);

                ResultRow row = new ResultRow();
                row.setTimeSeries(join(ts));
                row.setAggregationOrAnalysis(name);
                if (arguments != null) {
                    row.setArguments(arguments.toString());
                }
                row.setValues(value.toString());
                row.setOrder(key);


                rows.add(row);
            }


        });

    }

    private void addPointsToSeries(XYChart.Series<DateAxis, NumberAxis> series, List<Point> points) {
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);

            series.getData().add(new XYChart.Data(Instant.ofEpochMilli(point.getTimestamp()), point.getValue()));

            //If the chart only has one point than we add the same value, but 1 week in the future
            if (i == points.size() - 1) {
                if (series.getData().size() == 1) {
                    Instant futureTimestamp = Instant.ofEpochMilli(point.getTimestamp()).plus(1, ChronoUnit.DAYS);
                    series.getData().add(new XYChart.Data(futureTimestamp, point.getValue()));
                }
            }
        }
    }

    private int size(List<MetricTimeSeries> result) {
        if (result == null) {
            return 0;
        }
        return result.stream().mapToInt(MetricTimeSeries::size).sum();
    }

    private void convertTsToSeries(MetricTimeSeries ts) {
        XYChart.Series<DateAxis, NumberAxis> series = new XYChart.Series<>();
        series.setName(join(ts));

        ts.sort();
        List<Point> points;
        if (ts.size() > 200_000) {
            points = ts.points().filter(point -> point.getIndex() % 1000 == 0).collect(Collectors.toList());
        } else {
            points = ts.points().collect(Collectors.toList());
        }
        //reduce the amount shown in the chart we add every 100ths point
        addPointsToSeries(series, points);

        Platform.runLater(() -> chart.getData().add(series));

    }

    private String join(MetricTimeSeries ts) {
        if (ts == null) {
            return "";
        }
        return ts.attribute("host") + "-" +
                ts.attribute("source") + "-" +
                ts.attribute("group") + "-" +
                ts.getMetric();
    }


    private boolean solrAvailable() {
        boolean available = false;
        try {
            available = solr.ping().getStatus() == 0;
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Could not connect to Solr storage");
        }
        return available;
    }

    /**
     * Closes the connection to solr
     */
    public void stop() {
        try {
            solr.close();
        } catch (Exception e) {
            LOGGER.info("Could not close solr connection");
        }
    }
}