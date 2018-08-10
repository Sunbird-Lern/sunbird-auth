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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.NoSuchElementException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                                             .id("login-test")
                                             .username("login-test")
                                             .email("login@test.com")
                                             .enabled(true)
                                             .password("password")
                                             .build();
        userId = user.getId();

        UserRepresentation user2 = UserBuilder.create()
                                              .id("login-test2")
                                              .username("login-test2")
                                              .email("login2@test.com")
                                              .enabled(true)
                                              .password("password")
                                              .build();
        user2Id = user2.getId();

        RealmBuilder.edit(testRealm)
                    .user(user)
                    .user(user2);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    private static String userId;

    private static String user2Id;

    @Test
    public void testBrowserSecurityHeaders() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(oauth.getLoginFormUrl()).request().get();
        Assert.assertEquals(200, response.getStatus());
        for (Map.Entry<String, String> entry : BrowserSecurityHeaders.defaultHeaders.entrySet()) {
            String headerName = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            String headerValue = response.getHeaderString(headerName);
            Assert.assertNotNull(headerValue);
            Assert.assertEquals(headerValue, entry.getValue());
        }
        response.close();
    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test2", "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("login-test2", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(user2Id).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test2")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("login-test");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    private void setUserEnabled(String userName, boolean enabled) {
        UserRepresentation rep = adminClient.realm("test").users().get(userName).toRepresentation();
        rep.setEnabled(enabled);
        adminClient.realm("test").users().get(userName).update(rep);
    }

    @Test
    public void loginInvalidPasswordDisabledUser() {
        setUserEnabled("login-test", false);

        try {
            loginPage.open();
            loginPage.login("login-test", "invalid");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Invalid username or password.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled("login-test", true);
        }
    }

    @Test
    public void loginDisabledUser() {
        setUserEnabled("login-test", false);

        try {
            loginPage.open();
            loginPage.login("login-test", "password");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Account is disabled, contact admin.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled("login-test", true);
        }
    }

    @Test
    public void loginInvalidUsername() {
        loginPage.open();
        loginPage.login("invalid", "password");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("invalid", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginMissingUsername() {
        loginPage.open();
        loginPage.missingUsername();

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    // KEYCLOAK-2557
    public void loginUserWithEmailAsUsername() {
        loginPage.open();
        loginPage.login("login@test.com", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login@test.com").assertEvent();
    }

    @Test
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        loginPage.open();
        loginPage.login(" login-test \t ", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailWhitespaceSuccess() {
        loginPage.open();
        loginPage.login("    login@test.com    ", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).assertEvent();
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setPasswordPolicy(policy);
        adminClient.realm("test").update(realmRep);
    }

    @Test
    public void loginWithForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to more than one day to force password update
            // elapsedTime > timeToExpire
            setTimeOffset(86405);

            loginPage.open();

            loginPage.login("login-test", "password");

            updatePasswordPage.assertCurrent();

            updatePasswordPage.changePassword("updatedPassword", "updatedPassword");

            setTimeOffset(0);

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").assertEvent();

            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            assertEquals("bad expectation, on page: " + currentUrl, RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        } finally {
            setPasswordPolicy(null);
            UserResource userRsc = adminClient.realm("test").users().get("login-test");
            ApiUtil.resetUserPassword(userRsc, "password", false);
        }
    }

    @Test
    public void loginWithoutForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to less than one day to avoid forced password update
            // elapsedTime < timeToExpire
            setTimeOffset(86205);

            loginPage.open();

            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

            setTimeOffset(0);

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginNoTimeoutWithLongWait() {
        loginPage.open();

        setTimeOffset(1700);

        loginPage.login("login-test", "password");

        setTimeOffset(0);

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }



    @Test
    public void loginLoginHint() {
        String loginFormUrl = oauth.getLoginFormUrl() + "&login_hint=login-test";
        driver.navigate().to(loginFormUrl);

        Assert.assertEquals("login-test", loginPage.getUsername());
        loginPage.login("password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailSuccess() {
        loginPage.open();
        loginPage.login("login@test.com", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).assertEvent();
    }

    private void setRememberMe(boolean enabled) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        rep.setRememberMe(enabled);
        adminClient.realm("test").update(rep);
    }

    @Test
    public void loginWithRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    //KEYCLOAK-2741
    @Test
    public void loginAgainWithoutRememberMe() {
        setRememberMe(true);

        try {
            //login with remember me
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            //login without remember me
            loginPage.setRememberMe(false);
            loginPage.login("login-test", "password");
            
            // Expire session
            loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .assertEvent();
            sessionId = loginEvent.getSessionId();
            testingClient.testing().removeUserSession("test", sessionId);
            
            // Assert rememberMe not checked nor username/email prefilled
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("login-test", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }
    
    @Test
    // KEYCLOAK-3181
    public void loginWithEmailUserAndRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login@test.com")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            
            Assert.assertEquals("login@test.com", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }


    // Login timeout scenarios

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCode() {
        loginPage.open();
        setTimeOffset(5000);
        // No explicitly call "removeExpired". Hence authSession will still exists, but will be expired
        //testingClient.testing().removeExpired("test");

        loginPage.login("login@test.com", "password");
        loginPage.assertCurrent();

        Assert.assertEquals("You took too long to login. Login process starting from beginning.", loginPage.getError());
        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .assertEvent();
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCodeWithExplicitRemoveExpired() {
        loginPage.open();
        setTimeOffset(5000);
        // Explicitly call "removeExpired". Hence authSession won't exist, but will be restarted from the KC_RESTART
        testingClient.testing().removeExpired("test");

        loginPage.login("login@test.com", "password");

        //loginPage.assertCurrent();
        loginPage.assertCurrent();

        Assert.assertEquals("You took too long to login. Login process starting from beginning.", loginPage.getError());
        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .client((String) null)
                .assertEvent();
    }


    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        loginPage.open();

        driver.manage().deleteAllCookies();

        // Cookies are expired including KC_RESTART. No way to continue login. Error page must be shown with the "back to application" link
        loginPage.login("login@test.com", "password");
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm("test"), "third-party").toRepresentation();
        Assert.assertNotNull(link, thirdParty.getBaseUrl());
    }



    @Test
    public void openLoginFormWithDifferentApplication() throws Exception {
        // Login form shown after redirect from admin console
        oauth.clientId(Constants.ADMIN_CONSOLE_CLIENT_ID);
        oauth.redirectUri(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/admin/test/console");
        oauth.openLoginForm();

        // Login form shown after redirect from app
        oauth.clientId("test-app");
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLoginForm();

        assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void openLoginFormAfterExpiredCode() throws Exception {
        oauth.openLoginForm();

        setTimeOffset(5000);

        oauth.openLoginForm();

        loginPage.assertCurrent();
        try {
            String loginError = loginPage.getError();
            Assert.fail("Not expected to have error on loginForm. Error is: " + loginError);
        } catch (NoSuchElementException nsee) {
            // Expected
        }

        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

}
