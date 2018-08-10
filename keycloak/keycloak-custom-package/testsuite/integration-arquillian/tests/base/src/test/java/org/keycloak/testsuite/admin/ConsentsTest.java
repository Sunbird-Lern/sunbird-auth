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

package org.keycloak.testsuite.admin;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.ConsentPage;
import org.keycloak.testsuite.pages.LoginPage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ConsentsTest extends AbstractKeycloakTest {

    final static String REALM_PROV_NAME = "provider";
    final static String REALM_CONS_NAME = "consumer";

    final static String IDP_OIDC_ALIAS = "kc-oidc-idp";
    final static String IDP_OIDC_PROVIDER_ID = "keycloak-oidc";

    final static String CLIENT_ID = "brokerapp";
    final static String CLIENT_SECRET = "secret";

    final static String USER_LOGIN = "testuser";
    final static String USER_EMAIL = "user@localhost.com";
    final static String USER_PASSWORD = "password";
    final static String USER_FIRSTNAME = "User";
    final static String USER_LASTNAME = "Tester";

    protected RealmRepresentation createProviderRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_PROV_NAME);
        realm.setEnabled(true);

        return realm;
    }

    protected RealmRepresentation createConsumerRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM_CONS_NAME);
        realm.setEnabled(true);

        return realm;
    }

    protected List<ClientRepresentation> createProviderClients() {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(CLIENT_ID);
        client.setName(CLIENT_ID);
        client.setSecret(CLIENT_SECRET);
        client.setEnabled(true);
        client.setConsentRequired(true);

        client.setRedirectUris(Collections.singletonList(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint/*"));

        client.setAdminUrl(getAuthRoot() +
                "/auth/realms/" + REALM_CONS_NAME + "/broker/" + IDP_OIDC_ALIAS + "/endpoint");

        return Collections.singletonList(client);
    }

    protected IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);

        Map<String, String> config = idp.getConfig();

        config.put("clientId", CLIENT_ID);
        config.put("clientSecret", CLIENT_SECRET);
        config.put("prompt", "login");
        config.put("authorizationUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
        config.put("tokenUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
        config.put("logoutUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
        config.put("userInfoUrl", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
        config.put("defaultScope", "email profile");
        config.put("backchannelSupported", "true");

        return idp;
    }

    protected String getUserLogin() {
        return USER_LOGIN;
    }

    protected String getUserPassword() {
        return USER_PASSWORD;
    }

    protected String getUserEmail() {
        return USER_EMAIL;
    }

    protected String getUserFirstName() {
        return USER_FIRSTNAME;
    }

    protected String getUserLastName() {
        return USER_LASTNAME;
    }
    protected String providerRealmName() {
        return REALM_PROV_NAME;
    }

    protected String consumerRealmName() {
        return REALM_CONS_NAME;
    }

    protected String getIDPAlias() {
        return IDP_OIDC_ALIAS;
    }


    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected ConsentPage consentPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = createProviderRealm();
        RealmRepresentation consumerRealm = createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(getUserLogin());
        user.setEmail(getUserEmail());
        user.setFirstName(getUserFirstName());
        user.setLastName(getUserLastName());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + consumerRealmName());

        RealmResource realm = adminClient.realm(consumerRealmName());
        realm.identityProviders().create(setUpIdentityProvider());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + providerRealmName());

                providerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    protected IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setDisplayName(providerId);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    private void waitForPage(String title) {
        long startAt = System.currentTimeMillis();

        while (!driver.getTitle().toLowerCase().contains(title)
                && System.currentTimeMillis() - startAt < 200) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignore) {}
        }
    }

    @Test
    public void testConsents() {
        driver.navigate().to(getAccountUrl(consumerRealmName()));

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        if (!driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/")) {
            log.debug("Not on provider realm page, url: " + driver.getCurrentUrl());
        }

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(getUserLogin(), getUserPassword());

        waitForPage("grant access");

        Assert.assertTrue(consentPage.isCurrent());
        consentPage.confirm();

        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"));

        UsersResource consumerUsers = adminClient.realm(consumerRealmName()).users();
        Assert.assertTrue("There must be at least one user", consumerUsers.count() > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, 5);

        UserRepresentation foundUser = null;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail())) {
                foundUser = user;
                break;
            }
        }

        Assert.assertNotNull("There must be user " + getUserLogin() + " in realm " + consumerRealmName(),
                foundUser);

        // get user with the same username from provider realm
        RealmResource providerRealm = adminClient.realm(providerRealmName());
        users = providerRealm.users().search(null, foundUser.getFirstName(), foundUser.getLastName(), null, 0, 1);
        Assert.assertEquals("Same user should be in provider realm", 1, users.size());

        String userId = users.get(0).getId();
        UserResource userResource = providerRealm.users().get(userId);

        // list consents
        List<Map<String, Object>> consents = userResource.getConsents();
        Assert.assertEquals("There should be one consent", 1, consents.size());

        Map<String, Object> consent = consents.get(0);
        Assert.assertEquals("Consent should be given to " + CLIENT_ID, CLIENT_ID, consent.get("clientId"));

        // list sessions
        List<UserSessionRepresentation> sessions = userResource.getUserSessions();
        Assert.assertEquals("There should be one active session", 1, sessions.size());

        // revoke consent
        userResource.revokeConsent(CLIENT_ID);

        // list consents
        consents = userResource.getConsents();
        Assert.assertEquals("There should be no consents", 0, consents.size());

        // list sessions
        sessions = userResource.getUserSessions();
        Assert.assertEquals("There should be no active session", 0, sessions.size());
    }

    private String getAccountUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account";
    }
}
