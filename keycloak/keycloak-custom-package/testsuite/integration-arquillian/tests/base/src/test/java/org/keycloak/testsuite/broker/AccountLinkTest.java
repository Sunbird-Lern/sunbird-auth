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
package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.federation.PassThroughFederatedUserStorageProvider;
import org.keycloak.testsuite.federation.PassThroughFederatedUserStorageProviderFactory;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccountLinkTest extends AbstractKeycloakTest {
    public static final String CHILD_IDP = "child";
    public static final String PARENT_IDP = "parent-idp";
    public static final String PARENT_USERNAME = "parent";

    @Page
    protected AccountFederatedIdentityPage accountFederatedIdentityPage;

    @Page
    protected UpdateAccountInformationPage profilePage;

    @Page
    protected LoginPage loginPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(CHILD_IDP);
        realm.setEnabled(true);
        testRealms.add(realm);

        realm = new RealmRepresentation();
        realm.setRealm(PARENT_IDP);
        realm.setEnabled(true);

        testRealms.add(realm);

    }

    @Before
    public void beforeBrokerTest() {
        if (testContext.isInitialized()) {
            return;
        }

        // addIdpUser
        RealmResource realmParent = adminClient.realms().realm(PARENT_IDP);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(PARENT_USERNAME);
        user.setEnabled(true);
        String userId = createUserAndResetPasswordWithAdminClient(realmParent, user, "password");

        // addChildUser
        RealmResource realmChild = adminClient.realms().realm(CHILD_IDP);
        user = new UserRepresentation();
        user.setUsername("child");
        user.setEnabled(true);
        userId = createUserAndResetPasswordWithAdminClient(realmChild, user, "password");

        // setupUserStorageProvider
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("passthrough");
        provider.setProviderId(PassThroughFederatedUserStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", Integer.toString(1));
        realmChild.components().add(provider);

        // createBroker
        createParentChild();

        testContext.setInitialized(true);
    }


    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP, suiteContext);
    }

    @Test
    public void testAccountLink() {
        String childUsername = "child";
        String childPassword = "password";
        String childIdp = CHILD_IDP;

        testAccountLink(childUsername, childPassword, childIdp);

    }

    @Test
    public void testAccountLinkWithUserStorageProvider() {
        String childUsername = PassThroughFederatedUserStorageProvider.PASSTHROUGH_USERNAME;
        String childPassword = PassThroughFederatedUserStorageProvider.INITIAL_PASSWORD;
        String childIdp = CHILD_IDP;

        testAccountLink(childUsername, childPassword, childIdp);

    }

    protected void testAccountLink(String childUsername, String childPassword, String childIdp) {
        accountFederatedIdentityPage.realm(childIdp);
        accountFederatedIdentityPage.open();
        loginPage.isCurrent();
        loginPage.login(childUsername, childPassword);
        assertTrue(accountFederatedIdentityPage.isCurrent());

        accountFederatedIdentityPage.clickAddProvider(PARENT_IDP);

        this.loginPage.isCurrent();
        loginPage.login(PARENT_USERNAME, "password");

        // Assert identity linked in account management
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertTrue(driver.getPageSource().contains("id=\"remove-" + PARENT_IDP + "\""));

        // Logout from account management
        accountFederatedIdentityPage.logout();

        // Assert I am logged immediately to account management due to previously linked "test-user" identity
        loginPage.isCurrent();
        loginPage.clickSocial(PARENT_IDP);
        loginPage.login(PARENT_USERNAME, "password");
        System.out.println(driver.getCurrentUrl());
        System.out.println("--------------------------------");
        System.out.println(driver.getPageSource());
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertTrue(driver.getPageSource().contains("id=\"remove-" + PARENT_IDP + "\""));

        // Unlink my "test-user"
        accountFederatedIdentityPage.clickRemoveProvider(PARENT_IDP);
        assertTrue(driver.getPageSource().contains("id=\"add-" + PARENT_IDP + "\""));


        // Logout from account management
        accountFederatedIdentityPage.logout();

        this.loginPage.clickSocial(PARENT_IDP);
        this.loginPage.login(PARENT_USERNAME, "password");
        this.profilePage.assertCurrent();
    }


}
