/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.client.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.authorization.client.ClientAuthenticator;
import org.keycloak.authorization.client.Configuration;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpMethod<R> {

    private final HttpClient httpClient;
    private final ClientAuthenticator authenticator;
    private final RequestBuilder builder;
    protected final Configuration configuration;
    protected final HashMap<String, String> headers;
    protected final HashMap<String, String> params;
    private HttpMethodResponse<R> response;

    public HttpMethod(Configuration configuration, ClientAuthenticator authenticator, RequestBuilder builder) {
        this(configuration, authenticator, builder, new HashMap<String, String>(), new HashMap<String, String>());
    }

    public HttpMethod(Configuration configuration, ClientAuthenticator authenticator, RequestBuilder builder, HashMap<String, String> params, HashMap<String, String> headers) {
        this.configuration = configuration;
        this.httpClient = configuration.getHttpClient();
        this.authenticator = authenticator;
        this.builder = builder;
        this.params = params;
        this.headers = headers;
    }

    public void execute() {
        execute(new HttpResponseProcessor<R>() {
            @Override
            public R process(byte[] entity) {
                return null;
            }
        });
    }

    public R execute(HttpResponseProcessor<R> responseProcessor) {
        byte[] bytes = null;

        try {
            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                this.builder.setHeader(header.getKey(), header.getValue());
            }

            preExecute(this.builder);

            HttpResponse response = this.httpClient.execute(this.builder.build());
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                bytes = EntityUtils.toByteArray(entity);
            }

            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpResponseException("Unexpected response from server: " + statusCode + " / " + statusLine.getReasonPhrase(), statusCode, statusLine.getReasonPhrase(), bytes);
            }

            if (bytes == null) {
                return null;
            }

            return responseProcessor.process(bytes);
        } catch (HttpResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error executing http method [" + builder + "]. Response : " + String.valueOf(bytes), e);
        }
    }

    protected void preExecute(RequestBuilder builder) {
        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.addParameter(param.getKey(), param.getValue());
        }
    }

    public HttpMethod<R> authorizationBearer(String bearer) {
        this.builder.addHeader("Authorization", "Bearer " + bearer);
        return this;
    }

    public HttpMethodResponse<R> response() {
        this.response = new HttpMethodResponse(this);
        return this.response;
    }

    public HttpMethodAuthenticator<R> authentication() {
        return new HttpMethodAuthenticator<R>(this, authenticator);
    }

    public HttpMethod<R> param(String name, String value) {
        this.params.put(name, value);
        return this;
    }

    public HttpMethod<R> json(byte[] entity) {
        this.builder.addHeader("Content-Type", "application/json");
        this.builder.setEntity(new ByteArrayEntity(entity));
        return this;
    }

    public HttpMethod<R> form() {
        return new HttpMethod<R>(this.configuration, authenticator, this.builder, this.params, this.headers) {
            @Override
            protected void preExecute(RequestBuilder builder) {
                if (params != null) {
                    List<NameValuePair> formparams = new ArrayList<>();

                    for (Map.Entry<String, String> param : params.entrySet()) {
                        formparams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
                    }

                    try {
                        builder.setEntity(new UrlEncodedFormEntity(formparams, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Error creating form parameters");
                    }
                }
            }
        };
    }
}
