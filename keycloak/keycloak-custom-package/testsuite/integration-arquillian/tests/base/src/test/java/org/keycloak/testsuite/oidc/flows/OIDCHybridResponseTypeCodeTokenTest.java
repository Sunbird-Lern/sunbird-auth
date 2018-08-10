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

package org.keycloak.testsuite.oidc.flows;

import org.junit.Before;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Collections;
import java.util.List;

/**
 * Tests with response_type=code token
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCHybridResponseTypeCodeTokenTest extends AbstractOIDCResponseTypeTest  {

    @Before
    public void clientConfiguration() {
        clientManagerBuilder().standardFlow(true).implicitFlow(true);

        oauth.clientId("test-app");
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN);
    }


    protected List<IDToken> retrieveIDTokens(EventRepresentation loginEvent) {
        Assert.assertEquals(OIDCResponseType.CODE + " " + OIDCResponseType.TOKEN, loginEvent.getDetails().get(Details.RESPONSE_TYPE));

        OAuthClient.AuthorizationEndpointResponse authzResponse = new OAuthClient.AuthorizationEndpointResponse(oauth, true);
        Assert.assertNotNull(authzResponse.getAccessToken());
        Assert.assertNull(authzResponse.getIdToken());

        // IDToken exchanged for the code
        IDToken idToken2 = sendTokenRequestAndGetIDToken(loginEvent);

        return Collections.singletonList(idToken2);
    }


    @Test
    public void nonceNotUsedErrorExpected() {
        super.validateNonceNotUsedErrorExpected();
    }

    @Test
    public void errorStandardFlowNotAllowed() throws Exception {
        super.validateErrorStandardFlowNotAllowed();
    }

    @Test
    public void errorImplicitFlowNotAllowed() throws Exception {
        super.validateErrorImplicitFlowNotAllowed();
    }

}
