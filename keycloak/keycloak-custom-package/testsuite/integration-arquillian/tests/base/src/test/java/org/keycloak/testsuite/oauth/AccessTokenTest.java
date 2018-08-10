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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientTemplateResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.util.BasicAuthHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createRoleNameMapper;

import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccessTokenTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
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
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        UserBuilder user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("no-permissions")
                .addRoles("user")
                .password("password");
        realm.getUsers().add(user.build());

        testRealms.add(realm);

    }
    
    @Test
    public void loginFormUsernameOrEmailLabel() throws Exception {
        oauth.openLoginForm();
        
        assertEquals("Username or email", driver.findElement(By.xpath("//label[@for='username']")).getText());
    }

    @Test
    public void accessTokenRequest() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        Assert.assertThat(response.getExpiresIn(), allOf(greaterThanOrEqualTo(250), lessThanOrEqualTo(300)));
        Assert.assertThat(response.getRefreshExpiresIn(), allOf(greaterThanOrEqualTo(1750), lessThanOrEqualTo(1800)));

        assertEquals("bearer", response.getTokenType());

        String expectedKid = oauth.doCertsRequest("test").getKeys()[0].getKeyId();

        JWSHeader header = new JWSInput(response.getAccessToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getIdToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        header = new JWSInput(response.getRefreshToken()).getHeader();
        assertEquals("RS256", header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertEquals(expectedKid, header.getKeyId());
        assertNull(header.getContentType());

        AccessToken token = oauth.verifyToken(response.getAccessToken());

        assertEquals(findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId(), token.getSubject());
        Assert.assertNotEquals("test-user@localhost", token.getSubject());

        assertEquals(sessionId, token.getSessionState());

        assertEquals(1, token.getRealmAccess().getRoles().size());
        assertTrue(token.getRealmAccess().isUserInRole("user"));

        assertEquals(1, token.getResourceAccess(oauth.getClientId()).getRoles().size());
        assertTrue(token.getResourceAccess(oauth.getClientId()).isUserInRole("customer-user"));

        EventRepresentation event = events.expectCodeToToken(codeId, sessionId).assertEvent();
        assertEquals(token.getId(), event.getDetails().get(Details.TOKEN_ID));
        assertEquals(oauth.verifyRefreshToken(response.getRefreshToken()).getId(), event.getDetails().get(Details.REFRESH_TOKEN_ID));
        assertEquals(sessionId, token.getSessionState());

    }

    // KEYCLOAK-3692
    @Test
    public void accessTokenWrongCode() throws Exception {
        oauth.clientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        oauth.redirectUri(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/admin/test/console/nosuch.html");
        oauth.openLoginForm();

        String actionURI = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        String loginPageCode = ActionURIUtils.parseQueryParamsFromActionURI(actionURI).get("code");

        oauth.fillLoginForm("test-user@localhost", "password");

        events.expectLogin().client(Constants.ADMIN_CONSOLE_CLIENT_ID).detail(Details.REDIRECT_URI, AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/admin/test/console/nosuch.html").assertEvent();

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(loginPageCode, null);

        assertEquals(400, response.getStatusCode());
        assertNull(response.getRefreshToken());
    }

    @Test
    public void accessTokenInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "invalid");
        assertEquals(400, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_client_credentials").clearDetails().user((String) null).session((String) null);
        expectedEvent.assertEvent();
    }

    @Test
    public void accessTokenMissingClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);
        assertEquals(400, response.getStatusCode());

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_client_credentials").clearDetails().user((String) null).session((String) null);
        expectedEvent.assertEvent();
    }

    @Test
    public void accessTokenInvalidRedirectUri() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        //@TODO This new and was necesssary to not mess up with other tests cases
        String redirectUri = oauth.getRedirectUri();

        oauth.redirectUri("http://invalid");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Incorrect redirect_uri", response.getErrorDescription());

        events.expectCodeToToken(codeId, loginEvent.getSessionId()).error("invalid_code")
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .assertEvent();

        //@TODO Reset back to the original URI. Maybe we should have something to reset to the original state at OAuthClient
        oauth.redirectUri(redirectUri);

    }

    @Test
    public void accessTokenUserSessionExpired() {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String sessionId = loginEvent.getSessionId();


        testingClient.testing().removeUserSession("test", sessionId);
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        assertEquals(400, tokenResponse.getStatusCode());
        assertNull(tokenResponse.getAccessToken());
        assertNull(tokenResponse.getRefreshToken());

        events.expectCodeToToken(codeId, sessionId)
                .removeDetail(Details.TOKEN_ID)
                .user((String) null)
                .session((String) null)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .error(Errors.INVALID_CODE).assertEvent();

        events.clear();
    }

    @Test
    public void accessTokenCodeExpired() {
        RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(1);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        setTimeOffset(2);

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        setTimeOffset(0);

        AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, null);
        expectedEvent.error("invalid_code")
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_ID)
                .removeDetail(Details.REFRESH_TOKEN_TYPE)
                .user((String) null);
        expectedEvent.assertEvent();

        events.clear();

        RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(60);
    }

    @Test
    public void accessTokenCodeUsed() throws IOException {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        loginEvent.getSessionId();

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(200, response.getStatusCode());
        String accessToken = response.getAccessToken();

        Client jaxrsClient = javax.ws.rs.client.ClientBuilder.newClient();
        try {
            // Check that userInfo can be invoked
            Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(jaxrsClient, accessToken);
            UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user@localhost", "test-user@localhost");

            // Check that tokenIntrospection can be invoked
            String introspectionResponse = oauth.introspectAccessTokenWithClientCredential("test-app", "password", accessToken);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(introspectionResponse);
            Assert.assertEquals(true, jsonNode.get("active").asBoolean());
            Assert.assertEquals("test-user@localhost", jsonNode.get("email").asText());

            events.clear();

            // Repeating attempt to exchange code should be refused and invalidate previous clientSession
            response = oauth.doAccessTokenRequest(code, "password");
            Assert.assertEquals(400, response.getStatusCode());

            AssertEvents.ExpectedEvent expectedEvent = events.expectCodeToToken(codeId, null);
            expectedEvent.error("invalid_code")
                    .removeDetail(Details.TOKEN_ID)
                    .removeDetail(Details.REFRESH_TOKEN_ID)
                    .removeDetail(Details.REFRESH_TOKEN_TYPE)
                    .user((String) null);
            expectedEvent.assertEvent();

            // Check that userInfo can't be invoked with invalidated accessToken
            userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(jaxrsClient, accessToken);
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), userInfoResponse.getStatus());
            userInfoResponse.close();

            // Check that tokenIntrospection can't be invoked with invalidated accessToken
            introspectionResponse = oauth.introspectAccessTokenWithClientCredential("test-app", "password", accessToken);
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(introspectionResponse);
            Assert.assertEquals(false, jsonNode.get("active").asBoolean());
            Assert.assertNull(jsonNode.get("email"));

            events.clear();

            RealmManager.realm(adminClient.realm("test")).accessCodeLifeSpan(60);
        } finally {
            jaxrsClient.close();
        }
    }

    @Test
    public void accessTokenCodeRoleMissing() {
        RealmResource realmResource = adminClient.realm("test");
        RoleRepresentation role = RoleBuilder.create().name("tmp-role").build();
        realmResource.roles().create(role);
        UserResource user = findUserByUsernameId(realmResource, "test-user@localhost");
        UserManager.realm(realmResource).user(user).assignRoles(role.getName());

        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        realmResource.roles().deleteRole("tmp-role");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        Assert.assertEquals(200, response.getStatusCode());

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(1, token.getRealmAccess().getRoles().size());
        assertTrue(token.getRealmAccess().isUserInRole("user"));

        events.clear();
    }

    @Test
    public void accessTokenCodeHasRequiredAction() {

        UserResource user = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
        UserManager.realm(adminClient.realm("test")).user(user).addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString());

        oauth.doLogin("test-user@localhost", "password");

        String actionURI = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        String code = ActionURIUtils.parseQueryParamsFromActionURI(actionURI).get("code");

        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");
        Assert.assertEquals(400, response.getStatusCode());

        EventRepresentation event = events.poll();
        assertNull(event.getDetails().get(Details.CODE_ID));

        UserManager.realm(adminClient.realm("test")).user(user).removeRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString());
    }

    @Test
    public void testGrantAccessToken() throws Exception {
        Client client = javax.ws.rs.client.ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        {   // test checkSsl
            {
                RealmResource realmsResource = adminClient.realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setSslRequired(SslRequired.ALL.toString());
                realmsResource.update(realmRepresentation);
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(403, response.getStatus());
            response.close();

            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setSslRequired(SslRequired.EXTERNAL.toString());
                realmsResource.update(realmRepresentation);
            }

        }

        {   // test null username
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("password", "password");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test no password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   // test invalid password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            form.param("password", "invalid");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }
        {   // test no password
            String header = BasicAuthHelper.createHeader("test-app", "password");
            Form form = new Form();
            form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
            form.param("username", "test-user@localhost");
            Response response = grantTarget.request()
                    .header(HttpHeaders.AUTHORIZATION, header)
                    .post(Entity.form(form));
            assertEquals(401, response.getStatus());
            response.close();
        }

        {   //test bearer-only

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setBearerOnly(true);
                clientResource.update(clientRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setBearerOnly(false);
                clientResource.update(clientRepresentation);
            }

        }

        {   // test realm disabled
            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setEnabled(false);
                realmsResource.update(realmRepresentation);
            }

            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(403, response.getStatus());
            response.close();

            {
                RealmResource realmsResource = realmsResouce().realm("test");
                RealmRepresentation realmRepresentation = realmsResource.toRepresentation();
                realmRepresentation.setEnabled(true);
                realmsResource.update(realmRepresentation);
            }

        }

        {   // test application disabled

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setEnabled(false);
                clientResource.update(clientRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                ClientResource clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
                ClientRepresentation clientRepresentation = clientResource.toRepresentation();
                clientRepresentation.setEnabled(true);
                clientResource.update(clientRepresentation);

            }

        }

        {   // test user action required

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
                userResource.update(userRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.getRequiredActions().remove(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
                userResource.update(userRepresentation);
            }

        }

        {   // test user disabled
            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(false);
                userResource.update(userRepresentation);
            }


            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(400, response.getStatus());
            response.close();

            {
                UserResource userResource = findUserByUsernameId(adminClient.realm("test"), "test-user@localhost");
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setEnabled(true);
                userResource.update(userRepresentation);
            }

        }

        {
            Response response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            response.close();
        }

        client.close();
        events.clear();
    }

    @Test
    public void testKeycloak2221() throws Exception {
        Client client = javax.ws.rs.client.ClientBuilder.newClient();
        UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
        URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
        WebTarget grantTarget = client.target(grantUri);

        ClientResource clientResource;

        {

            clientResource = findClientByClientId(adminClient.realm("test"), "test-app");
            clientResource.getProtocolMappers().createMapper(createRoleNameMapper("rename-role", "user", "realm-user"));
            clientResource.getProtocolMappers().createMapper(createRoleNameMapper("rename-role2", "admin", "the-admin"));

        }

        {
            Response response = executeGrantRequest(grantTarget, "no-permissions", "password");
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            AccessToken accessToken = getAccessToken(tokenResponse);
            assertEquals(accessToken.getRealmAccess().getRoles().size(), 1);
            assertTrue(accessToken.getRealmAccess().getRoles().contains("realm-user"));

            response.close();
        }

        // undo mappers
        {
            ClientResource app = findClientByClientId(adminClient.realm("test"), "test-app");
            ClientRepresentation clientRepresentation = app.toRepresentation();
            for (ProtocolMapperRepresentation protocolRep : clientRepresentation.getProtocolMappers()) {
                if (protocolRep.getName().startsWith("rename-role")) {
                    clientResource.getProtocolMappers().delete(protocolRep.getId());
                }
            }
        }

        events.clear();

    }

    @Test
    public void testClientTemplate() throws Exception {
        RealmResource realm = adminClient.realm("test");
        RoleRepresentation realmRole = new RoleRepresentation();
        realmRole.setName("realm-test-role");
        realm.roles().create(realmRole);
        realmRole = realm.roles().get("realm-test-role").toRepresentation();
        RoleRepresentation realmRole2 = new RoleRepresentation();
        realmRole2.setName("realm-test-role2");
        realm.roles().create(realmRole2);
        realmRole2 = realm.roles().get("realm-test-role2").toRepresentation();


        List<UserRepresentation> users = realm.users().search("test-user@localhost", -1, -1);
        assertEquals(1, users.size());
        UserRepresentation user = users.get(0);

        List<RoleRepresentation> addRoles = new LinkedList<>();
        addRoles.add(realmRole);
        addRoles.add(realmRole2);
        realm.users().get(user.getId()).roles().realmLevel().add(addRoles);

        ClientTemplateRepresentation rep = new ClientTemplateRepresentation();
        rep.setName("template");
        rep.setProtocol("oidc");
        Response response = realm.clientTemplates().create(rep);
        assertEquals(201, response.getStatus());
        URI templateUri = response.getLocation();
        response.close();
        ClientTemplateResource templateResource = adminClient.proxy(ClientTemplateResource.class, templateUri);
        ProtocolMapperModel hard = HardcodedClaim.create("hard", "hard", "coded", "String", false, null, true, true);
        ProtocolMapperRepresentation mapper = ModelToRepresentation.toRepresentation(hard);
        response = templateResource.getProtocolMappers().createMapper(mapper);
        assertEquals(201, response.getStatus());
        response.close();
        List<ClientRepresentation> clients = realm.clients().findAll();
        ClientRepresentation clientRep = null;
        for (ClientRepresentation c : clients) {
            if (c.getClientId().equals("test-app")) {
                clientRep = c;
                break;
            }

        }
        clientRep.setClientTemplate("template");
        clientRep.setFullScopeAllowed(false);
        clientRep.setUseTemplateMappers(true);
        clientRep.setUseTemplateScope(true);
        clientRep.setUseTemplateConfig(true);
        realm.clients().get(clientRep.getId()).update(clientRep);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            IDToken idToken = getIdToken(tokenResponse);
            assertEquals("coded", idToken.getOtherClaims().get("hard"));

            AccessToken accessToken = getAccessToken(tokenResponse);
            assertEquals("coded", accessToken.getOtherClaims().get("hard"));

            // check zero scope for template
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test that scope is added
        List<RoleRepresentation> addRole1 = new LinkedList<>();
        addRole1.add(realmRole);
        templateResource.getScopeMappings().realmLevel().add(addRole1);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            AccessToken accessToken = getAccessToken(tokenResponse);
            // check zero scope for template
            assertNotNull(accessToken.getRealmAccess());
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test combined scopes
        List<RoleRepresentation> addRole2 = new LinkedList<>();
        addRole2.add(realmRole2);
        realm.clients().get(clientRep.getId()).getScopeMappings().realmLevel().add(addRole2);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);

            // check zero scope for template
            assertNotNull(accessToken.getRealmAccess());
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // remove scopes and retest
        templateResource.getScopeMappings().realmLevel().remove(addRole1);
        realm.clients().get(clientRep.getId()).getScopeMappings().realmLevel().remove(addRole2);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test full scope on template
        rep.setFullScopeAllowed(true);
        templateResource.update(rep);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);

            // check zero scope for template
            assertNotNull(accessToken.getRealmAccess());
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            assertTrue(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // test don't use template scope
        clientRep.setUseTemplateScope(false);
        realm.clients().get(clientRep.getId()).update(clientRep);

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);

            AccessToken accessToken = getAccessToken(tokenResponse);
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole.getName()));
            Assert.assertFalse(accessToken.getRealmAccess().getRoles().contains(realmRole2.getName()));


            response.close();
            client.close();
        }

        // undo mappers
        clientRep.setClientTemplate(ClientTemplateRepresentation.NONE);
        clientRep.setFullScopeAllowed(true);
        realm.clients().get(clientRep.getId()).update(clientRep);
        realm.users().get(user.getId()).roles().realmLevel().remove(addRoles);
        realm.roles().get(realmRole.getName()).remove();
        realm.roles().get(realmRole2.getName()).remove();
        templateResource.remove();

        {
            Client client = javax.ws.rs.client.ClientBuilder.newClient();
            UriBuilder builder = UriBuilder.fromUri(AUTH_SERVER_ROOT);
            URI grantUri = OIDCLoginProtocolService.tokenUrl(builder).build("test");
            WebTarget grantTarget = client.target(grantUri);

            response = executeGrantAccessTokenRequest(grantTarget);
            assertEquals(200, response.getStatus());
            org.keycloak.representations.AccessTokenResponse tokenResponse = response.readEntity(org.keycloak.representations.AccessTokenResponse.class);
            IDToken idToken = getIdToken(tokenResponse);
            assertNull(idToken.getOtherClaims().get("hard"));

            AccessToken accessToken = getAccessToken(tokenResponse);
            assertNull(accessToken.getOtherClaims().get("hard"));


            response.close();
            client.close();
        }
        events.clear();

    }

    // KEYCLOAK-1595 Assert that public client is able to retrieve token even if header "Authorization: Negotiate something" was used (parameter client_id has preference in this case)
    @Test
    public void testAuthorizationNegotiateHeaderIgnored() throws Exception {

        adminClient.realm("test").clients().create(ClientBuilder.create()
                .clientId("sample-public-client")
                .authenticatorType("client-secret")
                .redirectUris("http://localhost:8180/auth/realms/master/app/*")
                .publicClient()
                .build());

        oauth.clientId("sample-public-client");
        oauth.doLogin("test-user@localhost", "password");
        EventRepresentation loginEvent = events.expectLogin().client("sample-public-client").assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(oauth.getAccessTokenUrl());

            List<NameValuePair> parameters = new LinkedList<NameValuePair>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            post.setHeader("Authorization", "Negotiate something-which-will-be-ignored");

            UrlEncodedFormEntity formEntity = null;
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);

            OAuthClient.AccessTokenResponse response = new OAuthClient.AccessTokenResponse(client.execute(post));
            Assert.assertEquals(200, response.getStatusCode());
            AccessToken token = oauth.verifyToken(response.getAccessToken());
            events.expectCodeToToken(codeId, sessionId).client("sample-public-client").assertEvent();
        } finally {
            oauth.closeClient(client);
        }
    }

    private IDToken getIdToken(org.keycloak.representations.AccessTokenResponse tokenResponse) throws JWSInputException {
        JWSInput input = new JWSInput(tokenResponse.getIdToken());
        return input.readJsonContent(IDToken.class);
    }

    private AccessToken getAccessToken(org.keycloak.representations.AccessTokenResponse tokenResponse) throws JWSInputException {
        JWSInput input = new JWSInput(tokenResponse.getToken());
        return input.readJsonContent(AccessToken.class);
    }

    protected Response executeGrantAccessTokenRequest(WebTarget grantTarget) {
        String username = "test-user@localhost";
        String password = "password";
        return executeGrantRequest(grantTarget, username, password);
    }

    protected Response executeGrantRequest(WebTarget grantTarget, String username, String password) {
        String header = BasicAuthHelper.createHeader("test-app", "password");
        Form form = new Form();
        form.param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .param("username", username)
                .param("password", password)
                .param(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID);
        return grantTarget.request()
                .header(HttpHeaders.AUTHORIZATION, header)
                .post(Entity.form(form));
    }
}
