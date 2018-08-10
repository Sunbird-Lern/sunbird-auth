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

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.List;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author Sebastian Rose, AOE on 02.06.15.
 */
public class OAuthDanceClientSessionExtensionTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Test
    public void doOauthDanceWithClientSessionStateAndHost() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        String clientSessionState = "1234";
        String clientSessionHost = "test-client-host";

        OAuthClient.AccessTokenResponse tokenResponse = oauth.clientSessionState(clientSessionState)
                .clientSessionHost(clientSessionHost)
                .doAccessTokenRequest(code, "password");

        String refreshTokenString = tokenResponse.getRefreshToken();

        EventRepresentation tokenEvent = events.expectCodeToToken(codeId, sessionId)
                .detail(Details.CLIENT_SESSION_STATE, clientSessionState)
                .detail(Details.CLIENT_SESSION_HOST, clientSessionHost)
                .assertEvent();


        String updatedClientSessionState = "5678";

        oauth.clientSessionState(updatedClientSessionState)
                .clientSessionHost(clientSessionHost)
                .doRefreshTokenRequest(refreshTokenString, "password");

        events.expectRefresh(tokenEvent.getDetails().get(Details.REFRESH_TOKEN_ID), sessionId)
                .detail(Details.CLIENT_SESSION_STATE, updatedClientSessionState)
                .detail(Details.CLIENT_SESSION_HOST, clientSessionHost)
                .assertEvent();

    }

}
