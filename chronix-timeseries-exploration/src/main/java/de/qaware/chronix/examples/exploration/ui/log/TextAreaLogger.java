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
package de.qaware.chronix.examples.exploration.ui.log;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Instant;

/**
 * An appender to log messages to the given text area
 *
 * @author f.lautenschlager
 */
@Plugin(name = "TextAreaAppender", category = "Core", elementType = "appender", printObject = true)
public class TextAreaLogger extends AbstractAppender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextAreaLogger.class);

    private static volatile TextArea textArea = null;

    private static final String NEW_LINE = System.getProperty("line.separator");

    protected TextAreaLogger(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }


    /**
     * Set the target TextArea for the logging information to appear.
     *
     * @param textArea
     */
    public static void setTextArea(final TextArea textArea) {
        TextAreaLogger.textArea = textArea;
    }

    @Override
    public void append(LogEvent event) {
        // Append formatted message to text area using the Thread.
        try {
            Platform.runLater(() -> {
                try {
                    if (textArea != null) {
                        if (textArea.getText().length() == 0) {
                            textArea.setText(format(event));
                        } else {
                            textArea.selectEnd();
                            textArea.insertText(textArea.getText().length(), format(event));
                        }
                    }
                } catch (final Throwable t) {
                    System.out.println("Unable to append log to text area: "
                            + t.getMessage());
                }
            });
        } catch (final IllegalStateException e) {
            // ignore case when the platform hasn't yet been initialized
        }
    }

    private String format(LogEvent event) {
        Instant logTS = Instant.ofEpochMilli(event.getTimeMillis());
        if (event.getMessage().getThrowable() != null) {
            return logTS + " " + event.getLevel() + " " + event.getMessage().getFormattedMessage() + event.getMessage().getThrowable().toString() + NEW_LINE;

        } else {
            return logTS + " " + event.getLevel() + " " + event.getMessage().getFormattedMessage() + NEW_LINE;
        }
    }

    /**
     * A custom appender needs to declare a factory method annotated with `@PluginFactory`.
     * Log4j will parse the configuration and call this factory method to construct an appender instance with
     * the configured attributes.
     *
     * @param name           - the logger name
     * @param layout         - the layout
     * @param filter         - the filter
     * @param otherAttribute - other attributes
     * @return a text area logger
     */
    @PluginFactory
    public static TextAreaLogger createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TextAreaLogger(name, filter, layout, true);
    }
}
