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

package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class BasicAuthExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "basic-auth-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("basicauth");
        return fixedUrl != null ? fixedUrl : url;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .userInfo("{user}:{password}")
                .path("service/echo")
                .queryParam("value", "{value}");
    }

    public BasicAuthExample setTemplateValues(String user, String password, String value) {
        setUriParameter("user", user);
        setUriParameter("password", password);
        setUriParameter("value", value);
        return this;
    }

}
