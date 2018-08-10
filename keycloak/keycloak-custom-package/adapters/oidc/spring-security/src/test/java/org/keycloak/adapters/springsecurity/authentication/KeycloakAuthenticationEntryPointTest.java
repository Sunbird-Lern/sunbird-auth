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

package org.keycloak.adapters.springsecurity.authentication;

import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

/**
 * Keycloak authentication entry point tests.
 */
public class KeycloakAuthenticationEntryPointTest {

    private KeycloakAuthenticationEntryPoint authenticationEntryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    @Mock
    private ApplicationContext applicationContext;
   
    @Mock
    private AdapterDeploymentContext adapterDeploymentContext;
    
    @Mock
    private KeycloakDeployment keycloakDeployment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        authenticationEntryPoint = new KeycloakAuthenticationEntryPoint(adapterDeploymentContext);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(applicationContext.getBean(eq(AdapterDeploymentContext.class))).thenReturn(adapterDeploymentContext);
        when(adapterDeploymentContext.resolveDeployment(any(HttpFacade.class))).thenReturn(keycloakDeployment);
        when(keycloakDeployment.isBearerOnly()).thenReturn(Boolean.FALSE);
    }

    @Test
    public void testCommenceWithRedirect() throws Exception {
        configureBrowserRequest();
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI, response.getHeader("Location"));
    }

    @Test
    public void testCommenceWithRedirectNotRootContext() throws Exception {
        configureBrowserRequest();
        String contextPath = "/foo";
        request.setContextPath(contextPath);
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(contextPath + KeycloakAuthenticationEntryPoint.DEFAULT_LOGIN_URI, response.getHeader("Location"));
    }

    @Test
    public void testCommenceWithUnauthorizedWithAccept() throws Exception {
        request.addHeader(HttpHeaders.ACCEPT, "application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertNotNull(response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    public void testSetLoginUri() throws Exception {
        configureBrowserRequest();
        final String logoutUri = "/foo";
        authenticationEntryPoint.setLoginUri(logoutUri);
        authenticationEntryPoint.commence(request, response, null);
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals(logoutUri, response.getHeader("Location"));
    }

    private void configureBrowserRequest() {
        request.addHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    }
}
