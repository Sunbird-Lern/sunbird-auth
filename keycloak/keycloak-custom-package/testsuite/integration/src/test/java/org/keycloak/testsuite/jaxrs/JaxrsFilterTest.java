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
import org.keycloak.OAuth2Constants;
import org.keycloak.TokenIdGenerator;
import org.keycloak.adapters.CorsHeaders;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.Constants;
import org.keycloak.testsuite.OAuthClient;
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
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JaxrsFilterTest {

    private static final String JAXRS_APP_URL = Constants.SERVER_ROOT + "/jaxrs-simple/res";
    private static final String JAXRS_APP_PUSN_NOT_BEFORE_URL = Constants.SERVER_ROOT + "/jaxrs-simple/" + AdapterConstants.K_PUSH_NOT_BEFORE;

    public static final String CONFIG_FILE_INIT_PARAM = "config-file";

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            ClientModel app = appRealm.addClient("jaxrs-app");
            app.setEnabled(true);
            RoleModel role = app.addRole("jaxrs-app-user");
            UserModel user = manager.getSession().users().getUserByUsername("test-user@localhost", appRealm);
            user.grantRole(role);

            JaxrsFilterTest.appRealm = appRealm;
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
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Send GET request without token, it should fail
        Response getResp = client.target(JAXRS_APP_URL).request().get();
        Assert.assertEquals(getResp.getStatus(), 401);
        getResp.close();

        // Send POST request without token, it should fail
        Response postResp = client.target(JAXRS_APP_URL).request().post(Entity.form(new Form()));
        Assert.assertEquals(postResp.getStatus(), 401);
        postResp.close();

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

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

    @Test
    public void testRelativeUriAndPublicKey() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String,String> initParams = new TreeMap<String,String>();
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak-relative.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Send GET request without token, it should fail
        Response getResp = client.target(JAXRS_APP_URL).request().get();
        Assert.assertEquals(getResp.getStatus(), 401);
        getResp.close();

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

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
    }

    @Test
    public void testSslRequired() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String, String> initParams = new TreeMap<String, String>();
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak-ssl.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

        // Fail due to non-https
        Response getResp = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get();
        Assert.assertEquals(getResp.getStatus(), 403);
        getResp.close();
    }

    @Test
    public void testResourceRoleMappings() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String, String> initParams = new TreeMap<String, String>();
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak-resource-mappings.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

        // Send GET request with token and assert it's passing
        JaxrsTestResource.SimpleRepresentation getRep = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(JaxrsTestResource.SimpleRepresentation.class);
        Assert.assertEquals("get", getRep.getMethod());

        // principal is username
        Assert.assertEquals("test-user@localhost", getRep.getPrincipal());

        // User is in jaxrs-app-user role thanks to use-resource-role-mappings
        Assert.assertFalse(getRep.getHasUserRole());
        Assert.assertFalse(getRep.getHasAdminRole());
        Assert.assertTrue(getRep.getHasJaxrsAppRole());
    }

    @Test
    public void testCors() {
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String,String> initParams = new TreeMap<String,String>();
                initParams.put(CONFIG_FILE_INIT_PARAM, "classpath:jaxrs-test/jaxrs-keycloak.json");
                keycloakRule.deployJaxrsApplication("JaxrsSimpleApp", "/jaxrs-simple", JaxrsTestApplication.class, initParams);
            }

        });

        // Send OPTIONS request
        Response optionsResp = client.target(JAXRS_APP_URL).request()
                .header(CorsHeaders.ORIGIN, "http://localhost:8081")
                .options();
        Assert.assertEquals("true", optionsResp.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assert.assertEquals("http://localhost:8081", optionsResp.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        optionsResp.close();

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

        // Send GET request with token but bad origin
        Response badOriginResp = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(CorsHeaders.ORIGIN, "http://evil.org")
                .get();
        Assert.assertEquals(403, badOriginResp.getStatus());
        badOriginResp.close();

        // Send GET request with token and good origin
        Response goodResp = client.target(JAXRS_APP_URL).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(CorsHeaders.ORIGIN, "http://localhost:8081")
                .get();
        Assert.assertEquals(200, goodResp.getStatus());
        Assert.assertEquals("true", optionsResp.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        Assert.assertEquals("http://localhost:8081", optionsResp.getHeaderString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        JaxrsTestResource.SimpleRepresentation getRep = goodResp.readEntity(JaxrsTestResource.SimpleRepresentation.class);
        Assert.assertEquals("get", getRep.getMethod());
        goodResp.close();
    }

    // @Test
    public void testCxfExample() {
        //String uri = "http://localhost:9000/customerservice/customers/123";
        String uri = "http://localhost:8080/jax_rs_basic_servlet/services/service1/customerservice/customers/123";
        Response resp = client.target(uri).request()
                .get();
        Assert.assertEquals(resp.getStatus(), 401);
        resp.close();

        // Retrieve token
        OAuthClient.AccessTokenResponse accessTokenResp = retrieveAccessToken();
        String authHeader = "Bearer " + accessTokenResp.getAccessToken();

        String resp2 = client.target(uri).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .get(String.class);
        System.out.println(resp2);
    }


    private OAuthClient.AccessTokenResponse retrieveAccessToken() {
        OAuthClient oauth = new OAuthClient(driver);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(200, response.getStatusCode());
        return response;
    }

}
