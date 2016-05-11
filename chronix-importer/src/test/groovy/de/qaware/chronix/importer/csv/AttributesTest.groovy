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
package de.qaware.chronix.importer.csv

import spock.lang.Specification

/**
 * Simple unit test for the attributes class
 * @author f.lautenschlager
 */
class AttributesTest extends Specification {
    def "test get"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def field1 = attribute.get(0)
        def field2 = attribute.get(1)
        then:
        field1 == "one"
        field2 == "two"
    }

    def "test getMetric"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def metric = attribute.getMetric()
        then:
        metric == "metric"
    }

    def "test size"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def size = attribute.size()
        then:
        size == 2
    }

    def "test equals"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def equals = attribute.equals(attribute)
        then:
        equals == true
    }

    def "test hashCode"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def hash = attribute.hashCode()
        then:
        hash == attribute.hashCode()
    }

    def "test toString"() {
        given:
        def attribute = new Attributes("metric", "one", "two")

        when:
        def string = attribute.toString()
        then:
        string.contains("metric")
        string.contains("one")
        string.contains("two")
    }
}
