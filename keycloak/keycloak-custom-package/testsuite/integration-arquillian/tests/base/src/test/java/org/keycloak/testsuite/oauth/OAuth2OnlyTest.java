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

package org.keycloak.testsuite.oauth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;

import static org.junit.Assert.assertEquals;

/**
 * Test for scenarios when 'scope=openid' is missing. Which means we have pure OAuth2 request (not OpenID Connect)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuth2OnlyTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId("more-uris-client");
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("http://localhost:8180/auth/realms/master/app/auth", "http://localhost:8180/foo"));
        client.setBaseUrl("http://localhost:8180/auth/realms/master/app/auth");

        testRealm.getClients().add(client);

        ClientRepresentation testApp = testRealm.getClients().stream()
                .filter(cl -> cl.getClientId().equals("test-app"))
                .findFirst().get();
        testApp.setImplicitFlowEnabled(true);
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.init(adminClient, driver);
    }


    // If scope=openid is missing, IDToken won't be present
    @Test
    public void testMissingIDToken() {
        String loginFormUrl = oauth.getLoginFormUrl();
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.SCOPE);

        driver.navigate().to(loginFormUrl);
        oauth.fillLoginForm("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String code = new OAuthClient.AuthorizationEndpointResponse(oauth).getCode();
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        // IDToken is not there
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNull(response.getIdToken());
        Assert.assertNotNull(response.getRefreshToken());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(token.getSubject(), loginEvent.getUserId());

        // Refresh and assert idToken still not present
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "password");
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertNull(response.getIdToken());

        token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(token.getSubject(), loginEvent.getUserId());
    }


    // If scope=openid is missing, IDToken won't be present
    @Test
    public void testMissingScopeOpenidInResourceOwnerPasswordCredentialRequest() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());

        // idToken not present
        Assert.assertNull(response.getIdToken());

        Assert.assertNotNull(response.getRefreshToken());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(accessToken.getPreferredUsername(), "test-user@localhost");

    }


    // In OAuth2, it is allowed that redirect_uri is not mandatory as long as client has just 1 redirect_uri configured without wildcard
    @Test
    public void testMissingRedirectUri() throws Exception {
        // OAuth2 login without redirect_uri. It will be allowed.
        String loginFormUrl = oauth.getLoginFormUrl();
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.SCOPE);
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.REDIRECT_URI);

        driver.navigate().to(loginFormUrl);
        loginPage.assertCurrent();
        oauth.fillLoginForm("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        // Client 'more-uris-client' has 2 redirect uris. OAuth2 login without redirect_uri won't be allowed
        oauth.clientId("more-uris-client");
        loginFormUrl = oauth.getLoginFormUrl();
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.SCOPE);
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.REDIRECT_URI);

        driver.navigate().to(loginFormUrl);
        errorPage.assertCurrent();
        Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
        events.expectLogin()
                .error(Errors.INVALID_REDIRECT_URI)
                .client("more-uris-client")
                .user(Matchers.nullValue(String.class))
                .session(Matchers.nullValue(String.class))
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }


    // In OAuth2 (when response_type=token and no scope=openid) we don't treat nonce parameter mandatory
    @Test
    public void testMissingNonceInOAuth2ImplicitFlow() throws Exception {
        oauth.responseType("token");
        oauth.nonce(null);
        String loginFormUrl = oauth.getLoginFormUrl();
        loginFormUrl = ActionURIUtils.removeQueryParamFromURI(loginFormUrl, OAuth2Constants.SCOPE);

        driver.navigate().to(loginFormUrl);
        loginPage.assertCurrent();
        oauth.fillLoginForm("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        OAuthClient.AuthorizationEndpointResponse response = new OAuthClient.AuthorizationEndpointResponse(oauth);
        Assert.assertNull(response.getError());
        Assert.assertNull(response.getCode());
        Assert.assertNull(response.getIdToken());
        Assert.assertNotNull(response.getAccessToken());
    }

}
