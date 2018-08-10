/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.page;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractPageWithInjectedUrl extends AbstractPage {

    public abstract URL getInjectedUrl();

    //EAP6 URL fix
    protected URL createInjectedURL(String url) {
        if (!System.getProperty("app.server","").startsWith("eap6")) {
            return null;
        }
        try {
            if(Boolean.parseBoolean(System.getProperty("app.server.ssl.required"))) {
                return new URL("https://localhost:" + System.getProperty("app.server.https.port", "8543") + "/" + url);
            };
            return new URL("http://localhost:" + System.getProperty("app.server.http.port", "8180") + "/" + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public UriBuilder createUriBuilder() {
        try {
            return UriBuilder.fromUri(getInjectedUrl().toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
