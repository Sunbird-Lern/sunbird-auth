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

package org.keycloak.testsuite.admin.authentication;

import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.AuthenticatorConfigInfoRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.testsuite.Assert;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class ProvidersTest extends AbstractAuthenticationTest {

    @Test
    public void testFormProviders() {
        List<Map<String, Object>> result = authMgmtResource.getFormProviders();

        Assert.assertNotNull("null result", result);
        Assert.assertEquals("size", 1, result.size());
        Map<String, Object> item = result.get(0);

        Assert.assertEquals("id", "registration-page-form", item.get("id"));
        Assert.assertEquals("displayName", "Registration Page", item.get("displayName"));
        Assert.assertEquals("description", "This is the controller for the registration page", item.get("description"));
    }

    @Test
    public void testFormActionProviders() {
        List<Map<String, Object>> result = authMgmtResource.getFormActionProviders();

        List<Map<String, Object>> expected = new LinkedList<>();
        addProviderInfo(expected, "registration-profile-action", "Profile Validation",
                "Validates email, first name, and last name attributes and stores them in user data.");
        addProviderInfo(expected, "registration-recaptcha-action", "Recaptcha",
                "Adds Google Recaptcha button.  Recaptchas verify that the entity that is registering is a human.  " +
                        "This can only be used on the internet and must be configured after you add it.");
        addProviderInfo(expected, "registration-password-action", "Password Validation",
                "Validates that password matches password confirmation field.  It also will store password in user's credential store.");
        addProviderInfo(expected, "registration-user-creation", "Registration User Creation",
                "This action must always be first! Validates the username of the user in validation phase.  " +
                        "In success phase, this will create the user in the database.");

        compareProviders(expected, result);
    }

    @Test
    public void testClientAuthenticatorProviders() {
        List<Map<String, Object>> result = authMgmtResource.getClientAuthenticatorProviders();

        List<Map<String, Object>> expected = new LinkedList<>();
        addProviderInfo(expected, "client-jwt", "Signed Jwt",
                "Validates client based on signed JWT issued by client and signed with the Client private key");
        addProviderInfo(expected, "client-secret", "Client Id and Secret", "Validates client based on 'client_id' and " +
                "'client_secret' sent either in request parameters or in 'Authorization: Basic' header");
        addProviderInfo(expected, "testsuite-client-passthrough", "Testsuite Dummy Client Validation", "Testsuite dummy authenticator, " +
                "which automatically authenticates hardcoded client (like 'test-app' )");

        compareProviders(expected, result);
    }

    @Test
    public void testPerClientConfigDescriptions() {
        Map<String, List<ConfigPropertyRepresentation>> configs = authMgmtResource.getPerClientConfigDescription();
        Assert.assertTrue(configs.containsKey("client-jwt"));
        Assert.assertTrue(configs.containsKey("client-secret"));
        Assert.assertTrue(configs.containsKey("testsuite-client-passthrough"));
        Assert.assertTrue(configs.get("client-jwt").isEmpty());
        Assert.assertTrue(configs.get("client-secret").isEmpty());
        List<ConfigPropertyRepresentation> cfg = configs.get("testsuite-client-passthrough");
        Assert.assertProviderConfigProperty(cfg.get(0), "passthroughauth.foo", "Foo Property", null,
                "Foo Property of this authenticator, which does nothing", "String");
        Assert.assertProviderConfigProperty(cfg.get(1), "passthroughauth.bar", "Bar Property", null,
                "Bar Property of this authenticator, which does nothing", "boolean");
    }

    @Test
    public void testAuthenticatorConfigDescription() {
        // Try some not-existent provider
        try {
            authMgmtResource.getAuthenticatorConfigDescription("not-existent");
            Assert.fail("Don't expected to find provider 'not-existent'");
        } catch (NotFoundException nfe) {
            // Expected
        }

        AuthenticatorConfigInfoRepresentation infoRep = authMgmtResource.getAuthenticatorConfigDescription(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        Assert.assertEquals("Create User If Unique", infoRep.getName());
        Assert.assertEquals(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, infoRep.getProviderId());
        Assert.assertEquals("Detect if there is existing Keycloak account with same email like identity provider. If no, create new user", infoRep.getHelpText());
        Assert.assertEquals(1, infoRep.getProperties().size());
        Assert.assertProviderConfigProperty(infoRep.getProperties().get(0), "require.password.update.after.registration", "Require Password Update After Registration",
                null, "If this option is true and new user is successfully imported from Identity Provider to Keycloak (there is no duplicated email or username detected in Keycloak DB), then this user is required to update his password",
                "boolean");
    }


    @Test
    public void testInitialAuthenticationProviders() {

        List<Map<String, Object>> providers = authMgmtResource.getAuthenticatorProviders();
        providers = sortProviders(providers);

        compareProviders(expectedAuthProviders(), providers);
    }

    private List<Map<String, Object>> expectedAuthProviders() {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        addProviderInfo(result, "auth-conditional-otp-form", "Conditional OTP Form",
                "Validates a OTP on a separate OTP form. Only shown if required based on the configured conditions.");
        addProviderInfo(result, "auth-cookie", "Cookie", "Validates the SSO cookie set by the auth server.");
        addProviderInfo(result, "auth-otp-form", "OTP Form", "Validates a OTP on a separate OTP form.");
        if (Profile.isFeatureEnabled(Profile.Feature.SCRIPTS)) {
            addProviderInfo(result, "auth-script-based", "Script", "Script based authentication. Allows to define custom authentication logic via JavaScript.");
        }
        addProviderInfo(result, "auth-spnego", "Kerberos", "Initiates the SPNEGO protocol.  Most often used with Kerberos.");
        addProviderInfo(result, "auth-username-password-form", "Username Password Form",
                "Validates a username and password from login form.");
        addProviderInfo(result, "auth-x509-client-username-form", "X509/Validate Username Form",
                "Validates username and password from X509 client certificate received as a part of mutual SSL handshake.");
        addProviderInfo(result, "direct-grant-auth-x509-username", "X509/Validate Username",
                "Validates username and password from X509 client certificate received as a part of mutual SSL handshake.");
        addProviderInfo(result, "direct-grant-validate-otp", "OTP", "Validates the one time password supplied as a 'totp' form parameter in direct grant request");
        addProviderInfo(result, "direct-grant-validate-password", "Password",
                "Validates the password supplied as a 'password' form parameter in direct grant request");
        addProviderInfo(result, "direct-grant-validate-username", "Username Validation",
                "Validates the username supplied as a 'username' form parameter in direct grant request");
        addProviderInfo(result, "docker-http-basic-authenticator", "Docker Authenticator", "Uses HTTP Basic authentication to validate docker users, returning a docker error token on auth failure");
        addProviderInfo(result, "expected-param-authenticator", "TEST: Expected Parameter",
                "You will be approved if you send query string parameter 'foo' with expected value.");
        addProviderInfo(result, "http-basic-authenticator", "HTTP Basic Authentication", "Validates username and password from Authorization HTTP header");
        addProviderInfo(result, "identity-provider-redirector", "Identity Provider Redirector", "Redirects to default Identity Provider or Identity Provider specified with kc_idp_hint query parameter");
        addProviderInfo(result, "idp-confirm-link", "Confirm link existing account", "Show the form where user confirms if he wants " +
                "to link identity provider with existing account or rather edit user profile data retrieved from identity provider to avoid conflict");
        addProviderInfo(result, "idp-create-user-if-unique", "Create User If Unique", "Detect if there is existing Keycloak account " +
                "with same email like identity provider. If no, create new user");
        addProviderInfo(result, "idp-email-verification", "Verify existing account by Email", "Email verification of existing Keycloak " +
                "user, that wants to link his user account with identity provider");
        addProviderInfo(result, "idp-review-profile", "Review Profile",
                "User reviews and updates profile data retrieved from Identity Provider in the displayed form");
        addProviderInfo(result, "idp-username-password-form", "Username Password Form for identity provider reauthentication",
                "Validates a password from login form. Username is already known from identity provider authentication");
        addProviderInfo(result, "push-button-authenticator", "TEST: Button Login",
                "Just press the button to login.");
        addProviderInfo(result, "reset-credential-email", "Send Reset Email", "Send email to user and wait for response.");
        addProviderInfo(result, "reset-credentials-choose-user", "Choose User", "Choose a user to reset credentials for");
        addProviderInfo(result, "reset-otp", "Reset OTP", "Sets the Configure OTP required action if execution is REQUIRED.  " +
                "Will also set it if execution is OPTIONAL and the OTP is currently configured for it.");
        addProviderInfo(result, "reset-password", "Reset Password", "Sets the Update Password required action if execution is REQUIRED.  " +
                "Will also set it if execution is OPTIONAL and the password is currently configured for it.");
        addProviderInfo(result, "testsuite-dummy-click-through", "Testsuite Dummy Click Thru",
                "Testsuite Dummy authenticator.  User needs to click through the page to continue.");
        addProviderInfo(result, "testsuite-dummy-passthrough", "Testsuite Dummy Pass Thru",
                "Testsuite Dummy authenticator.  Just passes through and is hardcoded to a specific user");
        addProviderInfo(result, "testsuite-dummy-registration", "Testsuite Dummy Pass Thru",
                "Testsuite Dummy authenticator.  Just passes through and is hardcoded to a specific user");

        return result;
    }

    private List<Map<String, Object>> sortProviders(List<Map<String, Object>> providers) {
        ArrayList<Map<String, Object>> sorted = new ArrayList<>(providers);
        Collections.sort(sorted, new ProviderComparator());
        return sorted;
    }

    private void compareProviders(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {
        Assert.assertEquals("Providers count", expected.size(), actual.size());
        // compare ignoring list and map impl types
        Assert.assertEquals(normalizeResults(expected), normalizeResults(actual));
    }

    private List<Map<String, Object>> normalizeResults(List<Map<String, Object>> list) {
        ArrayList<Map<String, Object>> result = new ArrayList();
        for (Map<String, Object> item: list) {
            result.add(new HashMap(item));
        }
        return result;
    }

    private void addProviderInfo(List<Map<String, Object>> list, String id, String displayName, String description) {
        HashMap<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("displayName", displayName);
        item.put("description", description);
        list.add(item);
    }

    private static class ProviderComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return String.valueOf(o1.get("id")).compareTo(String.valueOf(o2.get("id")));
        }
    }
}
