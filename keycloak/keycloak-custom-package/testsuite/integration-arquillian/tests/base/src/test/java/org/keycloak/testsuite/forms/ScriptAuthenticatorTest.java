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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ExecutionBuilder;
import org.keycloak.testsuite.util.FlowBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * Tests for {@link org.keycloak.authentication.authenticators.browser.ScriptBasedAuthenticator}
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class ScriptAuthenticatorTest extends AbstractFlowTest {

    @Page
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private AuthenticationFlowRepresentation flow;

    public static final String EXECUTION_ID = "scriptAuth";

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumePreview();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        UserRepresentation failUser = UserBuilder.create()
                .id("fail")
                .username("fail")
                .email("fail@test.com")
                .enabled(true)
                .password("password")
                .build();

        UserRepresentation okayUser = UserBuilder.create()
                .id("user")
                .username("user")
                .email("user@test.com")
                .enabled(true)
                .password("password")
                .build();

        RealmBuilder.edit(testRealm)
                .user(failUser)
                .user(okayUser);
    }

    @Before
    public void configureFlows() throws Exception {
        if (testContext.isInitialized()) {
            return;
        }

        String scriptFlow = "scriptBrowser";

        AuthenticationFlowRepresentation scriptBrowserFlow = FlowBuilder.create()
                .alias(scriptFlow)
                .description("dummy pass through registration")
                .providerId("basic-flow")
                .topLevel(true)
                .builtIn(false)
                .build();

        Response createFlowResponse = testRealm().flows().createFlow(scriptBrowserFlow);
        Assert.assertEquals(201, createFlowResponse.getStatus());

        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(scriptFlow);
        realm.setDirectGrantFlow(scriptFlow);
        testRealm().update(realm);

        this.flow = findFlowByAlias(scriptFlow);

        AuthenticationExecutionRepresentation usernamePasswordFormExecution = ExecutionBuilder.create()
                .id("username password form")
                .parentFlow(this.flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator(UsernamePasswordFormFactory.PROVIDER_ID)
                .build();

        AuthenticationExecutionRepresentation authScriptExecution = ExecutionBuilder.create()
                .id(EXECUTION_ID)
                .parentFlow(this.flow.getId())
                .requirement(AuthenticationExecutionModel.Requirement.REQUIRED.name())
                .authenticator(ScriptBasedAuthenticatorFactory.PROVIDER_ID)
                .build();

        Response addExecutionResponse = testRealm().flows().addExecution(usernamePasswordFormExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();

        addExecutionResponse = testRealm().flows().addExecution(authScriptExecution);
        Assert.assertEquals(201, addExecutionResponse.getStatus());
        addExecutionResponse.close();

        testContext.setInitialized(true);
    }

    /**
     * KEYCLOAK-3491
     */
    @Test
    public void loginShouldWorkWithScriptAuthenticator() {
        addConfigFromFile("/scripts/authenticator-example.js");

        loginPage.open();

        loginPage.login("user", "password");

        events.expectLogin().user("user").detail(Details.USERNAME, "user").assertEvent();
    }

    /**
     * KEYCLOAK-3491
     */
    @Test
    public void loginShouldFailWithScriptAuthenticator() {
        addConfigFromFile("/scripts/authenticator-example.js");

        loginPage.open();

        loginPage.login("fail", "password");

        events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.USER_NOT_FOUND).assertEvent();
    }

    /**
     * KEYCLOAK-4505
     */
    @Test
    public void scriptWithClientSession()  {
        addConfigFromFile("/scripts/client-session-test.js", ImmutableMap.of(
                "realm", "test",
                "clientId", "test-app",
                "authMethod", "openid-connect"));

        loginPage.open();

        loginPage.login("user", "password");

        events.expectLogin().user("user").detail(Details.USERNAME, "user").assertEvent();
    }

    private void addConfigFromFile(String filename) {
        addConfigFromFile(filename, null);
    }

    private void addConfigFromFile(String filename, Map<String, String> parameters) {

        String alias = filename.substring(filename.lastIndexOf("/") + 1);
        String script = loadFile(filename, parameters);

        Response newExecutionConfigResponse = testRealm().flows().
                newExecutionConfig(EXECUTION_ID, createScriptAuthConfig(EXECUTION_ID, alias, script, "script based authenticator"));
        newExecutionConfigResponse.close();

        Assert.assertEquals(201, newExecutionConfigResponse.getStatus());
    }

    private String loadFile(String filename, Map<String, String> parameters) {
        String script = null;
        try {
            script = IOUtils.toString(getClass().getResourceAsStream(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (parameters != null) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                script = script.replaceAll("\\$\\{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return script;
    }

    private AuthenticatorConfigRepresentation createScriptAuthConfig(String alias, String scriptName, String script, String scriptDescription) {

        AuthenticatorConfigRepresentation configRep = new AuthenticatorConfigRepresentation();
        configRep.setAlias(alias);
        configRep.getConfig().put("scriptCode", script);
        configRep.getConfig().put("scriptName", scriptName);
        configRep.getConfig().put("scriptDescription", scriptDescription);

        return configRep;
    }
}
