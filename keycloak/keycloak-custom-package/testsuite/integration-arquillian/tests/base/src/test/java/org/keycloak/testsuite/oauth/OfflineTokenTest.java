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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.account.AccountTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.pages.AccountApplicationsPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.TokenUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findRealmRoleByName;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsernameId;
import static org.keycloak.testsuite.util.OAuthClient.APP_ROOT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenTest extends AbstractKeycloakTest {

    private static String userId;
    private static String offlineClientAppUri;
    private static String serviceAccountUserId;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountApplicationsPage applicationsPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Before
    public void clientConfiguration() {
        userId = findUserByUsername(adminClient.realm("test"), "test-user@localhost").getId();
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        RealmBuilder realm = RealmBuilder.edit(realmRepresentation)
                .accessTokenLifespan(10)
                .ssoSessionIdleTimeout(30)
                .testEventListener();

        offlineClientAppUri = APP_ROOT + "/offline-client";

        ClientRepresentation app = ClientBuilder.create().clientId("offline-client")
                .id(KeycloakModelUtils.generateId())
                .adminUrl(offlineClientAppUri)
                .redirectUris(offlineClientAppUri)
                .directAccessGrants()
                .serviceAccountsEnabled(true)
                .secret("secret1").build();

        realm.client(app);

        serviceAccountUserId = KeycloakModelUtils.generateId();
        UserRepresentation serviceAccountUser = UserBuilder.create()
                .id(serviceAccountUserId)
                .addRoles("user", "offline_access")
                .role("test-app", "customer-user")
                .username(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + app.getClientId())
                .serviceAccountId(app.getClientId()).build();

        realm.user(serviceAccountUser);

        testRealms.add(realm.build());

    }

    @Test
    public void offlineTokenDisabledForClient() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").fullScopeAllowed(false);

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("not_allowed", tokenResponse.getError());

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .error("not_allowed")
                .clearDetails()
                .assertEvent();

        ClientManager.realm(adminClient.realm("test")).clientId("offline-client").fullScopeAllowed(true);

    }

    @Test
    public void offlineTokenUserNotAllowed() throws Exception {
        String userId = findUserByUsername(adminClient.realm("test"), "keycloak-user@localhost").getId();

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("keycloak-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .user(userId)
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");

        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals("not_allowed", tokenResponse.getError());

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .user(userId)
                .error("not_allowed")
                .clearDetails()
                .assertEvent();
    }

    @Test
    public void offlineTokenBrowserFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        oauth.redirectUri(offlineClientAppUri);
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin()
                .client("offline-client")
                .detail(Details.REDIRECT_URI, offlineClientAppUri)
                .assertEvent();

        final String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "secret1");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectCodeToToken(codeId, sessionId)
                .client("offline-client")
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();

        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        assertEquals(0, offlineToken.getExpiration());

        String newRefreshTokenString = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, sessionId, userId);

        // Change offset to very big value to ensure offline session expires
        setTimeOffset(3000000);

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(newRefreshTokenString, "secret1");
        Assert.assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(offlineToken.getId(), sessionId)
                .client("offline-client")
                .error(Errors.INVALID_TOKEN)
                .user(userId)
                .clearDetails()
                .assertEvent();


        setTimeOffset(0);
    }

    private String testRefreshWithOfflineToken(AccessToken oldToken, RefreshToken offlineToken, String offlineTokenString,
                                               final String sessionId, String userId) {
        // Change offset to big value to ensure userSession expired
        setTimeOffset(99999);
        Assert.assertFalse(oldToken.isActive());
        Assert.assertTrue(offlineToken.isActive());

        // Assert userSession expired
        testingClient.testing().removeExpired("test");
        try {
            testingClient.testing().removeUserSession("test", sessionId);
        } catch (NotFoundException nfe) {
            // Ignore
        }

        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString, "secret1");
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(200, response.getStatusCode());
        Assert.assertEquals(sessionId, refreshedToken.getSessionState());

        // Assert new refreshToken in the response
        String newRefreshToken = response.getRefreshToken();
        Assert.assertNotNull(newRefreshToken);
        Assert.assertNotEquals(oldToken.getId(), refreshedToken.getId());

        Assert.assertEquals(userId, refreshedToken.getSubject());

        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole("user"));
        Assert.assertTrue(refreshedToken.getRealmAccess().isUserInRole(Constants.OFFLINE_ACCESS_ROLE));

        Assert.assertEquals(1, refreshedToken.getResourceAccess("test-app").getRoles().size());
        Assert.assertTrue(refreshedToken.getResourceAccess("test-app").isUserInRole("customer-user"));

        EventRepresentation refreshEvent = events.expectRefresh(offlineToken.getId(), sessionId)
                .client("offline-client")
                .user(userId)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .assertEvent();
        Assert.assertNotEquals(oldToken.getId(), refreshEvent.getDetails().get(Details.TOKEN_ID));

        setTimeOffset(0);
        return newRefreshToken;
    }

    @Test
    public void offlineTokenDirectGrantFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertEquals(0, offlineToken.getExpiration());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);

        // Assert same token can be refreshed again
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);
    }

    @Test
    public void offlineTokenDirectGrantFlowWithRefreshTokensRevoked() throws Exception {
        RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(true);

        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client")
                .user(userId)
                .session(token.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertEquals(0, offlineToken.getExpiration());

        String offlineTokenString2 = testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), userId);
        RefreshToken offlineToken2 = oauth.verifyRefreshToken(offlineTokenString2);

        // Assert second refresh with same refresh token will fail
        OAuthClient.AccessTokenResponse response = oauth.doRefreshTokenRequest(offlineTokenString, "secret1");
        Assert.assertEquals(400, response.getStatusCode());
        events.expectRefresh(offlineToken.getId(), token.getSessionState())
                .client("offline-client")
                .error(Errors.INVALID_TOKEN)
                .user(userId)
                .clearDetails()
                .assertEvent();

        // Refresh with new refreshToken is successful now
        testRefreshWithOfflineToken(token, offlineToken2, offlineTokenString2, token.getSessionState(), userId);

        RealmManager.realm(adminClient.realm("test")).revokeRefreshToken(false);
    }

    @Test
    public void offlineTokenServiceAccountFlow() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token.getSessionState())
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineToken.getType());
        Assert.assertEquals(0, offlineToken.getExpiration());

        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), serviceAccountUserId);

        // Now retrieve another offline token and verify that previous offline token is still valid
        tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        AccessToken token2 = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString2 = tokenResponse.getRefreshToken();
        RefreshToken offlineToken2 = oauth.verifyRefreshToken(offlineTokenString2);

        events.expectClientLogin()
                .client("offline-client")
                .user(serviceAccountUserId)
                .session(token2.getSessionState())
                .detail(Details.TOKEN_ID, token2.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken2.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "offline-client")
                .assertEvent();

        // Refresh with both offline tokens is fine
        testRefreshWithOfflineToken(token, offlineToken, offlineTokenString, token.getSessionState(), serviceAccountUserId);
        testRefreshWithOfflineToken(token2, offlineToken2, offlineTokenString2, token2.getSessionState(), serviceAccountUserId);
    }

    @Test
    public void offlineTokenAllowedWithCompositeRole() throws Exception {
        RealmResource appRealm = adminClient.realm("test");
        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        RoleRepresentation offlineAccess = findRealmRoleByName(adminClient.realm("test"),
                Constants.OFFLINE_ACCESS_ROLE).toRepresentation();

        // Grant offline_access role indirectly through composite role
        appRealm.roles().create(RoleBuilder.create().name("composite").build());
        RoleResource roleResource = appRealm.roles().get("composite");
        roleResource.addComposites(Collections.singletonList(offlineAccess));

        testUser.roles().realmLevel().remove(Collections.singletonList(offlineAccess));
        testUser.roles().realmLevel().add(Collections.singletonList(roleResource.toRepresentation()));

        // Integration test
        offlineTokenDirectGrantFlow();

        // Revert changes
        testUser.roles().realmLevel().remove(Collections.singletonList(appRealm.roles().get("composite").toRepresentation()));
        appRealm.roles().get("composite").remove();
        testUser.roles().realmLevel().add(Collections.singletonList(offlineAccess));
        
    }

    /**
     * KEYCLOAK-4201
     *
     * @throws Exception
     */
    @Test
    public void offlineTokenAdminRESTAccess() throws Exception {
        // Grant "view-realm" role to user
        RealmResource appRealm = adminClient.realm("test");
        ClientResource realmMgmt = ApiUtil.findClientByClientId(appRealm, Constants.REALM_MANAGEMENT_CLIENT_ID);
        String realmMgmtUuid = realmMgmt.toRepresentation().getId();
        RoleRepresentation roleRep = realmMgmt.roles().get(AdminRoles.VIEW_REALM).toRepresentation();

        UserResource testUser = findUserByUsernameId(appRealm, "test-user@localhost");
        testUser.roles().clientLevel(realmMgmtUuid).add(Collections.singletonList(roleRep));

        // Login with offline token now
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");

        events.clear();

        // Set the time offset, so that "normal" userSession expires
        setTimeOffset(86400);

        // Remove expired sessions. This will remove "normal" userSession
        testingClient.testing().removeUserSessions(appRealm.toRepresentation().getId());

        // Refresh with the offline token
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken(), "secret1");

        // Use accessToken to admin REST request
        Keycloak offlineTokenAdmin = Keycloak.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth",
                AuthRealm.MASTER, Constants.ADMIN_CLI_CLIENT_ID, tokenResponse.getAccessToken());
        RealmRepresentation testRealm = offlineTokenAdmin.realm("test").toRepresentation();
        Assert.assertNotNull(testRealm);
    }


    // KEYCLOAK-4525
    @Test
    public void offlineTokenRemoveClientWithTokens() throws Exception {
        // Create new client
        RealmResource appRealm = adminClient.realm("test");

        ClientRepresentation clientRep = ClientBuilder.create().clientId("offline-client-2")
                .id(KeycloakModelUtils.generateId())
                .directAccessGrants()
                .secret("secret1").build();

        appRealm.clients().create(clientRep);

        // Direct grant login requesting offline token
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.clientId("offline-client-2");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doGrantAccessTokenRequest("secret1", "test-user@localhost", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());
        String offlineTokenString = tokenResponse.getRefreshToken();
        RefreshToken offlineToken = oauth.verifyRefreshToken(offlineTokenString);

        events.expectLogin()
                .client("offline-client-2")
                .user(userId)
                .session(token.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, token.getId())
                .detail(Details.REFRESH_TOKEN_ID, offlineToken.getId())
                .detail(Details.REFRESH_TOKEN_TYPE, TokenUtil.TOKEN_TYPE_OFFLINE)
                .detail(Details.USERNAME, "test-user@localhost")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        // Go to account mgmt applications page
        applicationsPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().client("account").detail(Details.REDIRECT_URI, AccountTest.ACCOUNT_REDIRECT + "?path=applications").assertEvent();
        Assert.assertTrue(applicationsPage.isCurrent());
        Map<String, AccountApplicationsPage.AppEntry> apps = applicationsPage.getApplications();
        Assert.assertTrue(apps.containsKey("offline-client-2"));
        Assert.assertEquals("Offline Token", apps.get("offline-client-2").getAdditionalGrants().get(0));

        // Now remove the client
        ClientResource offlineTokenClient2 = ApiUtil.findClientByClientId(appRealm, "offline-client-2" );
        offlineTokenClient2.remove();

        // Go to applications page and see offline-client not anymore
        applicationsPage.open();
        apps = applicationsPage.getApplications();
        Assert.assertFalse(apps.containsKey("offline-client-2"));

        // Login as admin and see consents of user
        UserResource user = ApiUtil.findUserByUsernameId(appRealm, "test-user@localhost");
        List<Map<String, Object>> consents = user.getConsents();
        Assert.assertTrue(consents.isEmpty());
    }
}
