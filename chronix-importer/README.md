# Chronix CSV Importer
The Chronix CSV importer is a small util to import csv files into Chronix.
It imports all csv files within a directory or even single csv files.
Furthermore it writs a file (***metrics.csv***) that includes all time series attributes, the start and end, and the metric name.

## Format of the time series
The importer supports time series of pairs of timestamp and numeric value.

| timestamp | value |
|:---------:|:-----:|
|    4711   |   11  |
|    4811   |   12  |
|    ...    |  ...  |

The timestamps are encoded as long (ms since 1970). The values are doubles.
The time series could have arbitrary describing attributes.
Each attribute is a key-value-pair.

| key    |  value  |
|:------:|:-------:|
| host   | github  |
|process |  java   | 
|   ...  |   ...   |

But a time series need at least three fields:

- metric (String)
- start (long)
- end (long)

## Format of the csv files
The following sections describe the file name and the internal structure of the csv files.

### File Name Format and Attributes
We first want to take a look on how the attributes are represented.
The attributes are part of the file name and are separated by ***\_***.
The schema looks like:

 - attribute_attribute_attribute_..._attribute.csv.(gz)

For example:

 - github_jmx_java_2016.csv.gz
 
This will represent a time series with four attributes: github, jmx, java, and 2016.
The field names are defined in the ***config.yml*** in the section ***attributeFields***

```
attributeFields:
 - host
 - group
 - source
```

***Important***: The order matters. This means that the first defined field is mapped to the first attribute, and so on.
Furthermore one have to ensure that the field names also exist in the Solr-Schema (***schema.xml***).
Otherwise this an unknown field error is thrown.

### CSV Structure
The csv structure is quite simple.
The first column in the first row is called ***DATE***.
In the further columns of the first row are the metric names of the time series that all have the same attributes, except the metric name.
Thus starting at the second row the first column contains always the timestamp, the following columns the matching numeric values.

| DATE | ts-metric-1 |ts-metric-2 |ts-metric-... |ts-metric-n |
|:---------:|:-----:|:---------:|:---------:|:---------:|
|    4711   |   11  |     21    |     31    |     n1    |
|    4811   |   12  |     22    |     32    |     n2    |
|    ...    |  ...  |     ...    |     ...    |    ...    |

The format of the timestamp could be:

 - a long (ms since 1970): E.g. *dateFormat: LONG*
 - the instant representation: E.g. *dateFormat: INSTANT*
 - any format that could be expressed with the simple date format: E.g. *dateFormat: dd.MM.yyyy HH:mm:ss.SSS*
 
One can pass that value to the importer by setting them in the configuration file (***config.yml***).
The columns could be delimited by ***,*** or ***;***. Again that's also a configuration parameter.

## Configuration
The imported is configured using the ***config.yml*** file. 
The file is self-descriptive:

```
#The connection to chronix
chronix: http://localhost:8983/solr/chronix

#valid values: LONG (ms since 1970), INSTANT (default java 8 instant), 'SDF-FORMAT' e.g dd.MM.yyyy HH:mm:ss.SSS
dateFormat: dd.MM.yyyy HH:mm:ss.SSS

#valid values: ENGLISH, GERMAN
numberFormat: ENGLISH

# delimiter for csv files.
#valid values: , or ;
csvDelimiter: ;

#the name of the attribute fields.
#the order matters.
#the attributes are extracted from the file name
#jenkins_global_unix-global_qaware-jenkins
#host_group_process
attributeFields:
 - host
 - group
 - process


#Will parse the csv files without importing them.
#Only generates the metrics.csv file
onlyGenerateMetricsFile: true
```

## Execution
The folder release contains very simple bash script to execute the importer.
```
# !/bin/bash
java -Dlog4j.configurationFile=log4j2.xml -jar lib/chronix-importer-0.2-beta-1.jar config.yml data/
```
