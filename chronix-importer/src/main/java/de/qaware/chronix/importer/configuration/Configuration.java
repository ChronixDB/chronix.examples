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
package de.qaware.chronix.importer.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * The configuration class that is filled from a given yaml file
 *
 * @author f.lautenschlager
 */
public final class Configuration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static Map<String, Object> config = null;

    private Configuration(String ymlPath) {
        Yaml configuration = new Yaml();
        try {
            config = (Map<String, Object>) configuration.load(new FileInputStream(ymlPath));
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not load yaml configuration from path {}", ymlPath, e);
            return;
        }
    }

    /**
     *
     * @param path the path to the configuration (yml file)
     * @return the loaded configuration as map
     */
    public static synchronized Map<String, Object> load(String path) {
        if (config == null) {
            new Configuration(path);
        }
        return config;
    }

}
