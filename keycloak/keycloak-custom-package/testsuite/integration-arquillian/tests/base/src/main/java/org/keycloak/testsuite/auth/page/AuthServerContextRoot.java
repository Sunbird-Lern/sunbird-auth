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

package org.keycloak.testsuite.auth.page;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.URL;

/**
 * Context root of the tested Keycloak server.
 * 
 * URL: http://localhost:${auth.server.http.port}
 * 
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AuthServerContext
    private URL authServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return authServerContextRoot;
    }

}
