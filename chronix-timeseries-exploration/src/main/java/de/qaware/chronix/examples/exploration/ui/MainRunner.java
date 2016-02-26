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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main method to run the java fx example
 *
 * @author f.lautenschlager
 */
public class MainRunner extends Application {

    //The main controller to close the solr connection
    private static MainController controller;

    //The default url
    private static String solrUrl = "http://localhost:8983/solr/chronix/";

    /**
     * Runs the example. The application checks the args of the following parameter: 'solrUrl'
     *
     * @param args the first argument is the solr url
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            solrUrl = args[0];
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader();
        Pane root = fxmlLoader.load(getClass().getResource("Main.fxml").openStream());
        controller = fxmlLoader.getController();

        //init solr
        controller.initChronix(solrUrl);

        primaryStage.setTitle("Chronix JavaFX Example");
        primaryStage.setScene(new Scene(root));

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        controller.stop();
        super.stop();
    }
}
