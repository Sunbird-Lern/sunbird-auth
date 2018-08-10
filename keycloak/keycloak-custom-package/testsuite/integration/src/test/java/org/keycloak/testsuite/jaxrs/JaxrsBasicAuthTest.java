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

package org.keycloak.testsuite.jaxrs;

import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.common.util.Base64;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Test for basic authentication.
 */
public class JaxrsBasicAuthTest {

    private static final String JAXRS_APP_URL = Constants.SERVER_ROOT + "/jaxrs-simple/res";

    public static final String CONFIG_FILE_INIT_PARAM = "config-file";

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel app = KeycloakModelUtils.createClient(appRealm, "jaxrs-app");
            app.setEnabled(true);
            app.setSecret("password");
            app.setFullScopeAllowed(true);
            app.setDirectAccessGrantsEnabled(true);

            JaxrsBasicAuthTest.appRealm = appRealm;
        }
    });

    @ClassRule
    public static ExternalResource clientRule = new ExternalResource() {

        @Override
        protected void before() throws Throwable {
            DefaultHttpClient httpClient = (DefaultHttpClient) new HttpClientBuilder().build();
            ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
            client = new ResteasyClientBuilder().httpEngine(engine).build();
        }

        @Override
        protected void after() {
            client.close();
        }
    };

    private static ResteasyClient client;

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    // Used for signing admin action
    protected static RealmModel appRealm;


    @Test
    public void testBasic() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String,String> initParams = new TreeMap<String,String>();
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak-basicauth.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Send GET request without credentials, it should fail
        Response getResp = client.target(JAXRS_APP_URL).request().get();
        Assert.assertEquals(getResp.getStatus(), 401);
        getResp.close();

        // Send POST request without credentials, it should fail
        Response postResp = client.target(JAXRS_APP_URL).request().post(Entity.form(new Form()));
        Assert.assertEquals(postResp.getStatus(), 401);
        postResp.close();

        // Retrieve token
        String incorrectAuthHeader = "Basic "+encodeCredentials("invalid-user", "password");

        // Send GET request with incorrect credentials, it shojuld fail
        getResp = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, incorrectAuthHeader)
                .get();
        Assert.assertEquals(getResp.getStatus(), 401);
        getResp.close();
        
        // Retrieve token
        String authHeader = "Basic "+encodeCredentials("test-user@localhost", "password");

        // Send GET request with token and assert it's passing
        JaxrsTestResource.SimpleRepresentation getRep = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(JaxrsTestResource.SimpleRepresentation.class);
        Assert.assertEquals("get", getRep.getMethod());
        
        Assert.assertTrue(getRep.getHasUserRole());
        Assert.assertFalse(getRep.getHasAdminRole());
        Assert.assertFalse(getRep.getHasJaxrsAppRole());
        // Assert that principal is ID of user (should be in UUID format)
        UUID.fromString(getRep.getPrincipal());

        // Send POST request with token and assert it's passing
        JaxrsTestResource.SimpleRepresentation postRep = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(Entity.form(new Form()), JaxrsTestResource.SimpleRepresentation.class);
        Assert.assertEquals("post", postRep.getMethod());
        Assert.assertEquals(getRep.getPrincipal(), postRep.getPrincipal());
    }

    private String encodeCredentials(String username, String password) {
        String text=username+":"+password;
        return (Base64.encodeBytes(text.getBytes()));
    }
}
