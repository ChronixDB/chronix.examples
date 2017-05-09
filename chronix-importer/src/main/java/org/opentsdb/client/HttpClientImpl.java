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
package org.opentsdb.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.opentsdb.client.builder.MetricBuilder;
import org.opentsdb.client.response.ErrorDetail;
import org.opentsdb.client.response.Response;
import org.opentsdb.client.response.SimpleHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HTTP implementation of a client.
 */
public class HttpClientImpl implements HttpClient {

    private static Logger logger = LoggerFactory.getLogger(HttpClientImpl.class);

    private String serviceUrl;

    private Gson mapper;

    PoolingHttpClient httpClient = new PoolingHttpClient();
    private boolean commit;

    public HttpClientImpl(String serviceUrl) {
        this.serviceUrl = serviceUrl;

        GsonBuilder builder = new GsonBuilder();
        mapper = builder.create();
    }

    @Override
    public Response pushMetrics(MetricBuilder builder) throws IOException {
        return pushMetrics(builder, ExpectResponse.STATUS_CODE);

    }

    @Override
    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    @Override
    public Response pushMetrics(MetricBuilder builder,
                                ExpectResponse expectResponse) throws IOException {
        checkNotNull(builder);

        SimpleHttpResponse response = httpClient
                .doPost(buildUrl(serviceUrl, POST_API, expectResponse),
                        builder.build());

        return getResponse(response);
    }

    private String buildUrl(String serviceUrl, String postApiEndPoint,
                            ExpectResponse expectResponse) {
        String url = serviceUrl + postApiEndPoint;

        switch (expectResponse) {
            case SUMMARY:
                url += "?summary";
                break;
            case DETAIL:
                url += "?details";
                break;
            default:
                break;
        }

        url += "?commit=" + commit;


        return url;
    }

    private Response getResponse(SimpleHttpResponse httpResponse) {
        Response response = new Response(httpResponse.getStatusCode());
        String content = httpResponse.getContent();
        if (StringUtils.isNotEmpty(content)) {
            if (response.isSuccess()) {
                ErrorDetail errorDetail = mapper.fromJson(content,
                        ErrorDetail.class);
                response.setErrorDetail(errorDetail);
            } else {
                logger.error("request failed!" + httpResponse);
            }
        }
        return response;
    }
}